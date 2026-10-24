package app.kwela.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskRecoveryPlannerTest {
    @Test
    fun resumesAnUnfinishedMockTaskForItsRemainingTime() {
        val task = TaskRunSnapshot(
            id = "task-1",
            status = TaskStatus.RUNNING,
            completesAt = 20_000L
        )

        assertEquals(RecoveryAction.ResumeAfter(7_500L), TaskRecoveryPlanner.plan(task, 12_500L))
    }

    @Test
    fun completesAnInterruptedTaskWhoseMockDeadlinePassed() {
        val task = TaskRunSnapshot(
            id = "task-1",
            status = TaskStatus.RUNNING,
            completesAt = 20_000L
        )

        assertEquals(RecoveryAction.CompleteNow, TaskRecoveryPlanner.plan(task, 20_000L))
        assertEquals(RecoveryAction.CompleteNow, TaskRecoveryPlanner.plan(task, 22_000L))
    }

    @Test
    fun leavesWaitingOrCancelledTasksAlone() {
        assertEquals(
            RecoveryAction.NoOp,
            TaskRecoveryPlanner.plan(TaskRunSnapshot("task-2", TaskStatus.WAITING_FOR_USER, 20_000L), 12_500L)
        )
        assertEquals(
            RecoveryAction.NoOp,
            TaskRecoveryPlanner.plan(TaskRunSnapshot("task-3", TaskStatus.CANCELLED, 20_000L), 12_500L)
        )
    }
}
