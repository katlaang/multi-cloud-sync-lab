package com.cyc26.sync.service

import com.cyc26.sync.model.*
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

@Service
class SyncOrchestrator(
    private val cloudStore: CloudStore
) {

    private val audit = CopyOnWriteArrayList<AuditEntry>()

    @Volatile
    private var lastPublishedEvent: SyncEvent? = null

    @PostConstruct
    fun initialize() {
        reset()
    }

    @Synchronized
    fun reset(): DemoState {
        val seed = FlightRecord(
            entityId = ENTITY_ID,
            flightNumber = FLIGHT_NUMBER,
            status = "ON_TIME",
            gate = "C20",
            version = 1,
            lastUpdated = Instant.now(),
            source = CloudProvider.AWS
        )

        cloudStore.reset(seed)
        audit.clear()
        audit += AuditEntry(
            type = "RESET",
            message = "All simulated cloud stores seeded with $FLIGHT_NUMBER v1 ON_TIME at gate C20."
        )
        lastPublishedEvent = null
        return state()
    }

    @Synchronized
    fun normalSync(): DemoState {
        publishUpdate(
            source = CloudProvider.AWS,
            status = "BOARDING",
            gate = "C21"
        )
        return state()
    }

    @Synchronized
    fun createConflict(): DemoState {
        val baseTime = Instant.now()

        val aws = buildNextEvent(
            source = CloudProvider.AWS,
            status = "BOARDING",
            gate = "C22",
            timestamp = baseTime
        )

        val azure = buildNextEvent(
            source = CloudProvider.AZURE,
            status = "DELAYED",
            gate = "C24",
            timestamp = baseTime.plusMillis(25)
        )

        cloudStore.localWrite(aws)
        cloudStore.localWrite(azure)

        audit += AuditEntry(
            type = "SOURCE_WRITE",
            provider = CloudProvider.AWS,
            message = "AWS independently wrote ${aws.entityId} v${aws.version}: BOARDING at C22."
        )
        audit += AuditEntry(
            type = "SOURCE_WRITE",
            provider = CloudProvider.AZURE,
            message = "Azure independently wrote ${azure.entityId} v${azure.version}: DELAYED at C24."
        )

        broadcast(aws)
        broadcast(azure)

        lastPublishedEvent = azure
        return state()
    }

    @Synchronized
    fun publishUpdate(
        source: CloudProvider,
        status: String,
        gate: String
    ): DemoState {
        val event = buildNextEvent(source, status, gate, Instant.now())

        cloudStore.localWrite(event)
        audit += AuditEntry(
            type = "SOURCE_WRITE",
            provider = source,
            message = "${source.name} wrote ${event.entityId} v${event.version}: $status at $gate."
        )

        broadcast(event)
        lastPublishedEvent = event
        return state()
    }

    @Synchronized
    fun setOffline(provider: CloudProvider): DemoState {
        cloudStore.setOnline(provider, false)
        audit += AuditEntry(
            type = "OFFLINE",
            provider = provider,
            message = "$provider is now offline. New synchronization events will queue for this target."
        )
        return state()
    }

    @Synchronized
    fun restore(provider: CloudProvider): DemoState {
        cloudStore.setOnline(provider, true)

        val pending = cloudStore.drainPending(provider)
        audit += AuditEntry(
            type = "RECOVERY",
            provider = provider,
            message = "$provider restored. Replaying ${pending.size} queued event(s)."
        )

        pending.forEach { event ->
            val result = cloudStore.receive(provider, event)
            auditResult(provider, event, result, replay = true)
        }

        return state()
    }

    @Synchronized
    fun resendDuplicate(): DemoState {
        val event = lastPublishedEvent
            ?: throw IllegalStateException("No event has been published yet.")

        audit += AuditEntry(
            type = "REDELIVERY",
            provider = event.source,
            message = "Re-sending event ${event.eventId} to simulate at-least-once delivery."
        )
        broadcast(event)
        return state()
    }

    fun state(): DemoState =
        DemoState(
            clouds = cloudStore.allStates(),
            audit = audit.takeLast(60)
        )

    private fun buildNextEvent(
        source: CloudProvider,
        status: String,
        gate: String,
        timestamp: Instant
    ): SyncEvent {
        val current = cloudStore.state(source).current
            ?: error("No current record for $source.")

        return SyncEvent(
            eventId = UUID.randomUUID(),
            entityId = ENTITY_ID,
            version = current.version + 1,
            source = source,
            timestamp = timestamp,
            operation = Operation.UPDATE,
            payload = FlightPayload(
                flightNumber = FLIGHT_NUMBER,
                status = status,
                gate = gate
            )
        )
    }

    private fun broadcast(event: SyncEvent) {
        CloudProvider.entries.forEach { provider ->
            val result = cloudStore.receive(provider, event)
            auditResult(provider, event, result)
        }
    }

    private fun auditResult(
        provider: CloudProvider,
        event: SyncEvent,
        result: ApplyResult,
        replay: Boolean = false
    ) {
        val prefix = if (replay) "REPLAY" else when (result) {
            ApplyResult.Applied -> "APPLIED"
            ApplyResult.Duplicate -> "DUPLICATE_SKIPPED"
            ApplyResult.Stale -> "STALE_SKIPPED"
            ApplyResult.Queued -> "QUEUED"
            is ApplyResult.Conflict -> "CONFLICT"
        }

        val message = when (result) {
            ApplyResult.Applied ->
                "$provider applied ${event.entityId} v${event.version} from ${event.source}."

            ApplyResult.Duplicate ->
                "$provider ignored already-processed or equivalent event ${event.eventId}."

            ApplyResult.Stale ->
                "$provider ignored stale v${event.version} from ${event.source}."

            ApplyResult.Queued ->
                "$provider is offline. Queued v${event.version} from ${event.source}. Pending=${cloudStore.state(provider).pendingEvents}."

            is ApplyResult.Conflict ->
                "$provider detected same-version conflict. Winner=${result.winner}. Existing=[${result.existingDescription}] Incoming=[${result.incomingDescription}]."
        }

        audit += AuditEntry(
            type = prefix,
            provider = provider,
            message = message
        )
    }

    companion object {
        private const val ENTITY_ID = "FLIGHT-CYC2026"
        private const val FLIGHT_NUMBER = "CYC2026"
    }
}
