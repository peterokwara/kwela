package app.kwela.domain

enum class ReviewDecision { APPROVE, DENY }

object ApprovalPlanner {
    fun afterRun(status: TaskStatus): TaskStatus =
        if (status == TaskStatus.RUNNING) TaskStatus.WAITING_FOR_USER else status

    fun resolve(status: TaskStatus, decision: ReviewDecision): TaskStatus = when {
        status != TaskStatus.WAITING_FOR_USER -> status
        decision == ReviewDecision.APPROVE -> TaskStatus.COMPLETED
        else -> TaskStatus.CANCELLED
    }
}
