package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MessageDeliveryTest {
    private fun of(id: String = "m1", failed: Set<String> = emptySet(), pending: Map<String, Boolean> = emptyMap(), slow: Boolean = false) =
        MessageDelivery.of(id, failedIds = failed, pendingWaiting = pending, slow = slow)

    @Test
    fun aMessageNobodyIsWorkingOnIsJustSent() {
        assertEquals(Delivery.SENT, of())
    }

    @Test
    fun aFailedMessageSaysItWasNotSent() {
        assertEquals(Delivery.FAILED, of(failed = setOf("m1")))
        assertEquals(Delivery.SENT, of(failed = setOf("m2")))
    }

    @Test
    fun severalMessagesCanFailAtOnceAndEachSaysSo() {
        val failed = setOf("m1", "m2")
        assertEquals(Delivery.FAILED, of(id = "m1", failed = failed))
        assertEquals(Delivery.FAILED, of(id = "m2", failed = failed))
        assertEquals(Delivery.SENT, of(id = "m3", failed = failed))
    }

    @Test
    fun aQuickMessageBeingAnsweredShowsNothingBecauseTheTypingDotsAlreadyTellThePerson() {
        assertEquals(Delivery.SENT, of(pending = mapOf("m1" to false), slow = false))
    }

    @Test
    fun aSlowMessageBeingWorkedOnSaysSo() {
        assertEquals(Delivery.WORKING, of(pending = mapOf("m1" to false), slow = true))
    }

    @Test
    fun aMessageWaitingForTheConnectionShowsItEvenWhenQuick() {
        assertEquals(Delivery.WAITING, of(pending = mapOf("m1" to true), slow = false))
    }

    @Test
    fun resendingClearsTheFailedStateEvenIfTheOldMarkIsStillStored() {
        assertEquals(Delivery.WORKING, of(failed = setOf("m1"), pending = mapOf("m1" to false), slow = true))
    }

    @Test
    fun theLineUnderTheMessageMatchesGoogleMessagesWording() {
        assertEquals("Not sent.", MessageDelivery.line(Delivery.FAILED))
        assertEquals("Waiting to connect.", MessageDelivery.line(Delivery.WAITING))
        assertEquals("Working on it.", MessageDelivery.line(Delivery.WORKING))
        assertNull(MessageDelivery.line(Delivery.SENT))
    }

    @Test
    fun onlyAMessageWithSomethingToReportOffersOptions() {
        assertEquals(false, MessageDelivery.hasOptions(Delivery.SENT))
        assertEquals(true, MessageDelivery.hasOptions(Delivery.FAILED))
        assertEquals(true, MessageDelivery.hasOptions(Delivery.WAITING))
        assertEquals(true, MessageDelivery.hasOptions(Delivery.WORKING))
    }

    @Test
    fun workTaggedWithAMessageIdGivesBackThatId() {
        assertEquals("m1", MessageDelivery.messageIdFromTags(setOf("slow", "msg:m1")))
        assertNull(MessageDelivery.messageIdFromTags(setOf("slow")))
        assertEquals("msg:m1", MessageDelivery.tag("m1"))
    }
}
