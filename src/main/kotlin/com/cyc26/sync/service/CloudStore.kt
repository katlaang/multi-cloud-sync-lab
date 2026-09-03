package com.cyc26.sync.service

import com.cyc26.sync.model.CloudProvider
import com.cyc26.sync.model.CloudState
import com.cyc26.sync.model.FlightRecord
import com.cyc26.sync.model.SyncEvent
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Component
class CloudStore {

    private data class Node(
        var online: Boolean = true,
        var current: FlightRecord? = null,
        val pending: MutableList<SyncEvent> = mutableListOf(),
        val processedEventIds: MutableSet<UUID> = ConcurrentHashMap.newKeySet()
    )

    private val nodes = CloudProvider.entries.associateWith { Node() }.toMutableMap()

    @Synchronized
    fun reset(seed: FlightRecord) {
        nodes.forEach { (_, node) ->
            node.online = true
            node.current = seed
            node.pending.clear()
            node.processedEventIds.clear()
        }
    }

    @Synchronized
    fun state(provider: CloudProvider): CloudState {
        val node = nodes.getValue(provider)
        return CloudState(
            provider = provider,
            online = node.online,
            current = node.current,
            pendingEvents = node.pending.size
        )
    }

    @Synchronized
    fun allStates(): List<CloudState> =
        CloudProvider.entries.map(::state)

    @Synchronized
    fun setOnline(provider: CloudProvider, online: Boolean) {
        nodes.getValue(provider).online = online
    }

    @Synchronized
    fun isOnline(provider: CloudProvider): Boolean =
        nodes.getValue(provider).online

    @Synchronized
    fun localWrite(event: SyncEvent): FlightRecord {
        val node = nodes.getValue(event.source)
        require(node.online) { "${event.source} is offline" }

        val record = event.toRecord()
        node.current = record
        node.processedEventIds += event.eventId
        return record
    }

    @Synchronized
    fun receive(provider: CloudProvider, event: SyncEvent): ApplyResult {
        val node = nodes.getValue(provider)

        if (!node.online) {
            node.pending += event
            return ApplyResult.Queued
        }

        if (node.processedEventIds.contains(event.eventId)) {
            return ApplyResult.Duplicate
        }

        val existing = node.current

        if (existing == null || event.version > existing.version) {
            node.current = event.toRecord()
            node.processedEventIds += event.eventId
            return ApplyResult.Applied
        }

        if (event.version < existing.version) {
            node.processedEventIds += event.eventId
            return ApplyResult.Stale
        }

        val incomingSameValue =
            existing.status == event.payload.status &&
            existing.gate == event.payload.gate &&
            existing.flightNumber == event.payload.flightNumber

        if (incomingSameValue) {
            node.processedEventIds += event.eventId
            return ApplyResult.Duplicate
        }

        val incomingWins = when {
            event.timestamp.isAfter(existing.lastUpdated) -> true
            event.timestamp.isBefore(existing.lastUpdated) -> false
            else -> priority(event.source) > priority(existing.source)
        }

        val result = if (incomingWins) {
            node.current = event.toRecord()
            ApplyResult.Conflict(
                winner = event.source.name,
                existingDescription = existing.describe(),
                incomingDescription = event.describe()
            )
        } else {
            ApplyResult.Conflict(
                winner = existing.source.name,
                existingDescription = existing.describe(),
                incomingDescription = event.describe()
            )
        }

        node.processedEventIds += event.eventId
        return result
    }

    @Synchronized
    fun drainPending(provider: CloudProvider): List<SyncEvent> {
        val node = nodes.getValue(provider)
        val copy = node.pending.toList()
        node.pending.clear()
        return copy
    }

    private fun priority(provider: CloudProvider): Int =
        when (provider) {
            CloudProvider.AWS -> 1
            CloudProvider.AZURE -> 2
            CloudProvider.GCP -> 3
        }

    private fun SyncEvent.toRecord() =
        FlightRecord(
            entityId = entityId,
            flightNumber = payload.flightNumber,
            status = payload.status,
            gate = payload.gate,
            version = version,
            lastUpdated = timestamp,
            source = source
        )

    private fun FlightRecord.describe() =
        "${source.name} v$version status=$status gate=$gate @ $lastUpdated"

    private fun SyncEvent.describe() =
        "${source.name} v$version status=${payload.status} gate=${payload.gate} @ $timestamp"
}
