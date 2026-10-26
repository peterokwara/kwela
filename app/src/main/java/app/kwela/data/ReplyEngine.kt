package app.kwela.data

import android.content.Context
import android.net.Uri
import app.kwela.data.local.ChatMessageEntity
import app.kwela.data.local.SavedDocumentEntity
import app.kwela.data.remote.ApiResult
import app.kwela.data.remote.KwelaApi
import app.kwela.domain.CareerReplyParser
import app.kwela.domain.ChatRequestBuilder
import app.kwela.domain.ConversationContextPolicy
import app.kwela.domain.DocumentUpdates
import app.kwela.domain.FileEntry
import app.kwela.domain.HonestyLine
import app.kwela.domain.LocalMessages
import app.kwela.domain.OutgoingMessage
import app.kwela.domain.OutgoingRole
import app.kwela.domain.ReplyPacing
import app.kwela.domain.ReviewPrompts
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.booleanOrNull

/** What came of asking the writer for a reply. */
class ReplyOutcome(
    /** A message safe to show the person, or null when the reply arrived. */
    val error: String? = null,
    /** True when the server no longer accepts this sign-in. */
    val sessionExpired: Boolean = false,
    /** The text of the reply, for a notification. Null when there was none. */
    val replyText: String? = null,
    /** True while the backend-owned run is still working. */
    val pending: Boolean = false
)

/** Sends the conversation to the writer and saves what comes back. Runs from the chat screen's background job so it survives the person leaving the app. */
class ReplyEngine(private val context: Context, private val repository: KwelaRepository, private val api: KwelaApi) {
    suspend fun run(targetMessageId: String, retryKey: String? = null): ReplyOutcome {
        val outcome = produce(targetMessageId, retryKey)
        // A message the person deleted while this ran must not leave a "Not sent" mark behind.
        val stillThere = repository.messages.first().any { it.id == targetMessageId }
        if (outcome.error != null) {
            if (stillThere) repository.markReplyFailed(targetMessageId, outcome.error)
        } else if (!outcome.pending && !outcome.sessionExpired) {
            repository.clearReplyFailed(targetMessageId)
        }
        if (outcome.sessionExpired) repository.setSignInNotice("Please sign in again.")
        return outcome
    }

    private suspend fun produce(targetMessageId: String, retryKey: String?): ReplyOutcome = coroutineScope {
        val lastUser = repository.messages.first().firstOrNull { it.id == targetMessageId && it.role == "USER" }
        val slow = lastUser != null && ReplyPacing.isSlow(lastUser.content, lastUser.attachmentUri != null)
        val ownBubbles = java.util.Collections.synchronizedList(mutableListOf<String>())
        var succeeded = false
        var stillOnIt: Job? = null
        try {
            if (slow) ownBubbles += repository.sayLocally(SLOW_ACK, id = localBubbleId(targetMessageId, "ack"), createdAt = lastUser!!.createdAt + 1)
            stillOnIt = if (slow) launch { delay(STILL_ON_IT_AFTER); ownBubbles += repository.sayLocally(STILL_ON_IT, id = localBubbleId(targetMessageId, "working"), createdAt = lastUser!!.createdAt + 2) } else null
            val outcome = try {
                fetchReply(targetMessageId, retryKey)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                ReplyOutcome(error = "Something went wrong. Please try again.")
            }
            succeeded = outcome.error == null && !outcome.sessionExpired
            outcome
        } finally {
            // Whatever happened, make sure the app's own "Got it" bubbles don't linger when there is no reply to follow them.
            withContext(NonCancellable) {
                stillOnIt?.cancelAndJoin()
                if (!succeeded) ownBubbles.toList().forEach { repository.deleteMessage(it) }
            }
        }
    }

    private suspend fun fetchReply(targetMessageId: String, retryKey: String?): ReplyOutcome {
        val reader = AttachmentReader(context)
        // Each queued run sees only the history through its own message, so later messages cannot jump the queue.
        val allMessages = repository.messages.first().filter { !LocalMessages.isLocal(it.id) }
        val targetIndex = allMessages.indexOfFirst { it.id == targetMessageId && it.role == "USER" }
        if (targetIndex < 0) return ReplyOutcome(error = "Kwela couldn’t find that message. Please send it again.")
        val token = repository.sessionToken() ?: return expired()
        val found = api.chatRun(token, targetMessageId)
        val started = if (found is ApiResult.Success) {
            val status = runCatching { (Json.parseToJsonElement(found.body) as? JsonObject)?.get("status")?.jsonPrimitive?.contentOrNull }.getOrNull()
            if (status == "FAILED" && retryKey != null) api.startChatRun(token, targetMessageId, "{}", retryKey) else found
        } else if (found is ApiResult.Failure && found.status == 404) {
            val savedProfileJson = repository.candidateProfile.first()
            val scoped = allMessages.take(targetIndex + 1)
            var contextJson = repository.conversationContext.first()
            val contextCursor = contextJson?.let { raw -> runCatching {
                (Json.parseToJsonElement(raw) as? JsonObject)?.get("throughMessageId")?.jsonPrimitive?.content
            }.getOrNull() }
            if (contextCursor != null && scoped.none { it.id == contextCursor }) contextJson = null
            val start = ConversationContextPolicy.windowStart(scoped.map { it.id }, if (contextJson != null) contextCursor else null)
            val recent = scoped.drop(start)
            if (recent.size + (if (savedProfileJson != null) 1 else 0) > ConversationContextPolicy.MAX_MESSAGES) {
                return ReplyOutcome(error = "Kwela couldn’t safely condense this conversation. Please try again later.")
            }
            val compactUntil = ConversationContextPolicy.compactionEnd(recent.size)
            val compactingFileIds = if (compactUntil >= ConversationContextPolicy.MIN_MESSAGES_TO_COMPACT) recent.take(compactUntil).map { it.id }.toSet() else emptySet()
            val history = withContext(Dispatchers.IO) {
                recent.map { message ->
                    val uri = message.attachmentUri.takeIf { message.id == targetMessageId || message.id in compactingFileIds }
                    val file = uri?.let { reader.read(message.id, Uri.parse(it)) }
                    if (uri != null && file == null && message.id == targetMessageId) return@withContext null
                    OutgoingMessage(message.id, if (message.role == "USER") OutgoingRole.USER else OutgoingRole.ASSISTANT, message.content, listOfNotNull(file), message.applicationJson)
                }
            } ?: return ReplyOutcome(error = "Kwela couldn’t read your attached file. Attach it again.")
            api.startChatRun(token, targetMessageId, ChatRequestBuilder.build(history, savedProfileJson, conversationContextJson = contextJson), retryKey)
        } else found
        if (started is ApiResult.Failure) {
            if (started.status == 401) return expired()
            return if (started.status == null || started.status >= 500) ReplyOutcome(pending = true) else ReplyOutcome(error = started.message)
        }
        val startJson = runCatching { Json.parseToJsonElement((started as ApiResult.Success).body) as? JsonObject }.getOrNull()
            ?: return ReplyOutcome(error = "Kwela sent a task update the app couldn’t read. Please try again.")
        val runId = startJson["runId"]?.jsonPrimitive?.contentOrNull
            ?: return ReplyOutcome(error = "Kwela sent a task update the app couldn’t read. Please try again.")
        val deadline = System.currentTimeMillis() + MAX_POLL_DURATION_MS
        var replyJson: kotlinx.serialization.json.JsonElement? = null
        var acknowledged = false
        while (System.currentTimeMillis() < deadline) {
            val statusResult = api.chatRun(token, runId)
            if (statusResult is ApiResult.Failure) {
                if (statusResult.status == 401) return expired()
                if (statusResult.status != null && statusResult.status < 500) return ReplyOutcome(error = statusResult.message)
                delay(POLL_INTERVAL_MS)
                continue
            }
            val statusBody = runCatching { Json.parseToJsonElement((statusResult as ApiResult.Success).body) as? JsonObject }.getOrNull()
                ?: return ReplyOutcome(error = "Kwela sent a task update the app couldn’t read. Please try again.")
            when (statusBody["status"]?.jsonPrimitive?.contentOrNull) {
                "COMPLETE" -> {
                    replyJson = statusBody["reply"]
                    acknowledged = statusBody["acknowledged"]?.jsonPrimitive?.booleanOrNull == true
                    break
                }
                "FAILED" -> return ReplyOutcome(error = statusBody["error"]?.jsonPrimitive?.contentOrNull ?: "Kwela couldn’t finish that one. Tap Try again.")
                else -> delay(POLL_INTERVAL_MS)
            }
        }
        if (replyJson == null) return when {
            System.currentTimeMillis() >= deadline -> ReplyOutcome(pending = true)
            acknowledged -> ReplyOutcome()
            else -> ReplyOutcome(error = "Kwela couldn’t recover this reply. Please send your message again.")
        }
        val reply = CareerReplyParser.parse(replyJson.toString()) ?: return ReplyOutcome(error = "Kwela sent a reply the app couldn’t read. Please try again.")
        reply.conversationContextJson?.let { context -> repository.saveConversationContext(context) }

        val now = System.currentTimeMillis()
        val targetMessageAt = allMessages[targetIndex].createdAt
        val replyAt = targetMessageAt + 3
        // A revision of a job already written for updates those files, so there is only ever one CV, one letter and one job breakdown per job.
        val saved = repository.documents.first()
        val plan = DocumentUpdates.plan(
            saved.map { FileEntry(it.id, it.title, it.groupId, it.groupTitle, it.createdAt, it.kind) }, reply.groupTitle.orEmpty(), reply.documents.map { it.kind }
        )
        val groupId = plan.groupId ?: UUID.nameUUIDFromBytes("group-$targetMessageId".toByteArray()).toString()
        reply.documents.forEachIndexed { index, document ->
            val id = plan.replace[document.kind] ?: UUID.nameUUIDFromBytes("document-$targetMessageId-$index-${document.kind}".toByteArray()).toString()
            repository.saveDocument(
                SavedDocumentEntity(id, document.title, document.body, now + index, document.kind.ifEmpty { null }, document.sourceJson, groupId, reply.groupTitle)
            )
        }
        // Only the first handover says she is a digital helper. A sincere "are you AI?" has no documents, so it is never trimmed.
        val earlierAssistantTexts = repository.messages.first().filter { it.role == "ASSISTANT" && it.id != "reply-$targetMessageId" }.map { it.content }
        val message = if (reply.documents.isNotEmpty() && HonestyLine.alreadySaid(earlierAssistantTexts)) HonestyLine.strip(reply.message).ifBlank { reply.message } else reply.message
        repository.saveMessage(
            ChatMessageEntity(
                "reply-$targetMessageId", "ASSISTANT", message, replyAt,
                applicationJson = reply.applicationJson, documentGroupId = if (reply.documents.isEmpty()) null else groupId
            )
        )
        val recentAssistantTexts = repository.messages.first().takeLast(8).filter { it.role == "ASSISTANT" }.map { it.content }
        val alreadyAsked = repository.answeredReviewLines()
        val question = reply.review.firstOrNull { it.line !in alreadyAsked }?.takeIf { ReviewPrompts.shouldAsk(recentAssistantTexts) }
        if (question != null) {
            repository.saveMessage(ChatMessageEntity("review-$targetMessageId", "ASSISTANT", ReviewPrompts.bubble(question), replyAt + 1))
            repository.savePendingReview(Json.encodeToString(question))
            // Ask about a line once. Someone who ignores the question should not be asked the same thing again after every reply.
            repository.addAnsweredReviewLine(question.line)
        }
        reply.candidateProfileJson?.let { profile ->
            repository.saveCandidateProfile(profile)
            api.saveProfile(token, profile)
        }
        api.acknowledgeChatRun(token, runId)
        return ReplyOutcome(replyText = listOfNotNull(message, question?.let { ReviewPrompts.bubble(it) }).joinToString("\n\n"))
    }

    private suspend fun expired(): ReplyOutcome {
        repository.signOut()
        return ReplyOutcome(sessionExpired = true)
    }

    private fun localBubbleId(targetMessageId: String, kind: String): String = "${LocalMessages.PREFIX}${targetMessageId}-$kind"

    private companion object {
        const val SLOW_ACK = "Got it, thank you. Give me about a minute to put this together."
        const val STILL_ON_IT = "Still on it, nearly there."
        const val STILL_ON_IT_AFTER = 90_000L
        const val POLL_INTERVAL_MS = 4_000L
        const val MAX_POLL_DURATION_MS = 7 * 60_000L
    }
}
