package app.kwela.data

import android.content.Context
import android.net.Uri
import app.kwela.data.local.ChatMessageEntity
import app.kwela.data.local.SavedDocumentEntity
import app.kwela.data.remote.ApiResult
import app.kwela.data.remote.KwelaApi
import app.kwela.domain.CareerReplyParser
import app.kwela.domain.ChatRequestBuilder
import app.kwela.domain.DocumentUpdates
import app.kwela.domain.FileEntry
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

/** What came of asking the writer for a reply. */
class ReplyOutcome(
    /** A message safe to show the person, or null when the reply arrived. */
    val error: String? = null,
    /** True when the server no longer accepts this sign-in. */
    val sessionExpired: Boolean = false,
    /** The text of the reply, for a notification. Null when there was none. */
    val replyText: String? = null
)

/** Sends the conversation to the writer and saves what comes back. Runs from the chat screen's background job so it survives the person leaving the app. */
class ReplyEngine(private val context: Context, private val repository: KwelaRepository, private val api: KwelaApi) {
    suspend fun run(): ReplyOutcome {
        val outcome = produce()
        // A job that was cancelled (the person sent something newer, or signed out) throws before this point and leaves the state alone.
        repository.setReplyError(outcome.error)
        if (outcome.sessionExpired) repository.setSignInNotice("Please sign in again.")
        return outcome
    }

    private suspend fun produce(): ReplyOutcome = coroutineScope {
        val lastUser = repository.messages.first().lastOrNull { !LocalMessages.isLocal(it.id) && it.role == "USER" }
        val slow = lastUser != null && ReplyPacing.isSlow(lastUser.content, lastUser.attachmentUri != null)
        val ownBubbles = java.util.Collections.synchronizedList(mutableListOf<String>())
        var succeeded = false
        var stillOnIt: Job? = null
        try {
            if (slow) ownBubbles += repository.sayLocally(SLOW_ACK)
            stillOnIt = if (slow) launch { delay(STILL_ON_IT_AFTER); ownBubbles += repository.sayLocally(STILL_ON_IT) } else null
            val outcome = try {
                fetchReply()
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

    private suspend fun fetchReply(): ReplyOutcome {
        val reader = AttachmentReader(context)
        // The app's own bubbles ("Here's your CV") are never sent, so they must not use up the window or hide the real newest message.
        val recent = repository.messages.first().filter { !LocalMessages.isLocal(it.id) }.takeLast(20)
        val contextJson = repository.conversationContext.first()
        val newestUserId = recent.lastOrNull { it.role == "USER" }?.id
        val contextCursor = contextJson?.let { raw ->
            runCatching {
                (Json.parseToJsonElement(raw) as? JsonObject)?.get("throughMessageId")?.jsonPrimitive?.content
            }.getOrNull()
        }
        val cursorIndex = contextCursor?.let { id -> recent.indexOfFirst { it.id == id } } ?: -1
        val compactFrom = if (cursorIndex < 0) 0 else cursorIndex + 1
        val compactUntil = (recent.size - 12).coerceAtLeast(0)
        val compactingFileIds = if (compactUntil - compactFrom >= 6) {
            recent.subList(compactFrom, compactUntil).map { it.id }.toSet()
        } else emptySet()
        val history = withContext(Dispatchers.IO) {
            recent.map { message ->
                // Re-send older attachments only when their turns are about to be compacted.
                val uri = message.attachmentUri.takeIf { message.id == newestUserId || message.id in compactingFileIds }
                val file = uri?.let { reader.read(message.id, Uri.parse(it)) }
                if (uri != null && file == null && message.id == newestUserId) return@withContext null
                OutgoingMessage(message.id, if (message.role == "USER") OutgoingRole.USER else OutgoingRole.ASSISTANT, message.content, listOfNotNull(file), message.applicationJson)
            }
        } ?: return ReplyOutcome(error = "Kwela couldn’t read your attached file. Attach it again.")
        val token = repository.sessionToken() ?: return expired()
        val result = api.chat(token, ChatRequestBuilder.build(
            history,
            repository.candidateProfile.first(),
            conversationContextJson = contextJson
        ))
        if (result is ApiResult.Failure) return if (result.status == 401) expired() else ReplyOutcome(error = result.message)
        val reply = CareerReplyParser.parse((result as ApiResult.Success).body) ?: return ReplyOutcome(error = "Kwela sent a reply the app couldn’t read. Please try again.")
        reply.conversationContextJson?.let { context -> repository.saveConversationContext(context) }

        val now = System.currentTimeMillis()
        // A revision of a job already written for updates those files, so there is only ever one CV, one letter and one job breakdown per job.
        val saved = repository.documents.first()
        val plan = DocumentUpdates.plan(
            saved.map { FileEntry(it.id, it.title, it.groupId, it.groupTitle, it.createdAt, it.kind) }, reply.groupTitle.orEmpty(), reply.documents.map { it.kind }
        )
        val groupId = plan.groupId ?: UUID.randomUUID().toString()
        reply.documents.forEachIndexed { index, document ->
            val id = plan.replace[document.kind] ?: UUID.randomUUID().toString()
            repository.saveDocument(
                SavedDocumentEntity(id, document.title, document.body, now + index, document.kind.ifEmpty { null }, document.sourceJson, groupId, reply.groupTitle)
            )
        }
        repository.saveMessage(
            ChatMessageEntity(
                UUID.randomUUID().toString(), "ASSISTANT", reply.message, now + reply.documents.size,
                applicationJson = reply.applicationJson, documentGroupId = if (reply.documents.isEmpty()) null else groupId
            )
        )
        val recentAssistantTexts = repository.messages.first().takeLast(8).filter { it.role == "ASSISTANT" }.map { it.content }
        val alreadyAsked = repository.answeredReviewLines()
        val question = reply.review.firstOrNull { it.line !in alreadyAsked }?.takeIf { ReviewPrompts.shouldAsk(recentAssistantTexts) }
        if (question != null) {
            repository.saveMessage(ChatMessageEntity(UUID.randomUUID().toString(), "ASSISTANT", ReviewPrompts.bubble(question), now + reply.documents.size + 1))
            repository.savePendingReview(Json.encodeToString(question))
        }
        reply.candidateProfileJson?.let { profile ->
            repository.saveCandidateProfile(profile)
            api.saveProfile(token, profile)
        }
        return ReplyOutcome(replyText = listOfNotNull(reply.message, question?.let { ReviewPrompts.bubble(it) }).joinToString("\n\n"))
    }

    private suspend fun expired(): ReplyOutcome {
        repository.signOut()
        return ReplyOutcome(sessionExpired = true)
    }

    private companion object {
        const val SLOW_ACK = "Got it, thank you. Give me about a minute to put this together."
        const val STILL_ON_IT = "Still on it, nearly there."
        const val STILL_ON_IT_AFTER = 90_000L
    }
}
