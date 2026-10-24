package app.kwela.domain

data class TaskRunSnapshot(
    val id: String,
    val status: TaskStatus,
    val completesAt: Long
)

enum class TaskStatus {
    QUEUED,
    RUNNING,
    WAITING_FOR_USER,
    COMPLETED,
    CANCELLED,
    FAILED
}

sealed interface RecoveryAction {
    data class ResumeAfter(val delayMillis: Long) : RecoveryAction
    data object CompleteNow : RecoveryAction
    data object NoOp : RecoveryAction
}

object TaskRecoveryPlanner {
    fun plan(task: TaskRunSnapshot, now: Long): RecoveryAction = when {
        task.status != TaskStatus.RUNNING -> RecoveryAction.NoOp
        task.completesAt <= now -> RecoveryAction.CompleteNow
        else -> RecoveryAction.ResumeAfter(task.completesAt - now)
    }
}
