package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ApprovalPlannerTest {
    @Test
    fun aFinishedRunWaitsForTheUsersReview() {
        assertEquals(TaskStatus.WAITING_FOR_USER, ApprovalPlanner.afterRun(TaskStatus.RUNNING))
    }

    @Test
    fun approvingAWaitingTaskCompletesIt() {
        assertEquals(TaskStatus.COMPLETED, ApprovalPlanner.resolve(TaskStatus.WAITING_FOR_USER, ReviewDecision.APPROVE))
    }

    @Test
    fun denyingAWaitingTaskCancelsIt() {
        assertEquals(TaskStatus.CANCELLED, ApprovalPlanner.resolve(TaskStatus.WAITING_FOR_USER, ReviewDecision.DENY))
    }

    @Test
    fun aTaskThatIsNotWaitingIsLeftAlone() {
        assertEquals(TaskStatus.COMPLETED, ApprovalPlanner.resolve(TaskStatus.COMPLETED, ReviewDecision.DENY))
        assertEquals(TaskStatus.CANCELLED, ApprovalPlanner.resolve(TaskStatus.CANCELLED, ReviewDecision.APPROVE))
        assertEquals(TaskStatus.RUNNING, ApprovalPlanner.resolve(TaskStatus.RUNNING, ReviewDecision.APPROVE))
    }
}
