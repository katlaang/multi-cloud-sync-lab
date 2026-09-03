package com.cyc26.sync.service

sealed interface ApplyResult {
    data object Applied : ApplyResult
    data object Duplicate : ApplyResult
    data object Stale : ApplyResult
    data object Queued : ApplyResult

    data class Conflict(
        val winner: String,
        val existingDescription: String,
        val incomingDescription: String
    ) : ApplyResult
}
