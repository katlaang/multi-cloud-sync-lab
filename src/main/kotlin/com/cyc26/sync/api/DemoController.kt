package com.cyc26.sync.api

import com.cyc26.sync.model.CloudProvider
import com.cyc26.sync.model.DemoState
import com.cyc26.sync.service.SyncOrchestrator
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class DemoController(
    private val orchestrator: SyncOrchestrator
) {

    data class ManualUpdateRequest(
        val source: CloudProvider,
        val status: String,
        val gate: String
    )

    @GetMapping("/state")
    fun state(): DemoState = orchestrator.state()

    @PostMapping("/reset")
    fun reset(): DemoState = orchestrator.reset()

    @PostMapping("/scenario/normal")
    fun normal(): DemoState = orchestrator.normalSync()

    @PostMapping("/scenario/conflict")
    fun conflict(): DemoState = orchestrator.createConflict()

    @PostMapping("/scenario/outage-update")
    fun outageUpdate(): DemoState =
        orchestrator.publishUpdate(
            source = CloudProvider.AWS,
            status = "GATE_CHANGED",
            gate = "C22"
        )

    @PostMapping("/clouds/{provider}/offline")
    fun offline(@PathVariable provider: CloudProvider): DemoState =
        orchestrator.setOffline(provider)

    @PostMapping("/clouds/{provider}/online")
    fun online(@PathVariable provider: CloudProvider): DemoState =
        orchestrator.restore(provider)

    @PostMapping("/update")
    fun update(@RequestBody request: ManualUpdateRequest): DemoState =
        orchestrator.publishUpdate(
            source = request.source,
            status = request.status,
            gate = request.gate
        )

    @PostMapping("/duplicate")
    fun duplicate(): DemoState = orchestrator.resendDuplicate()

    @ExceptionHandler(IllegalStateException::class, IllegalArgumentException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun badRequest(ex: RuntimeException): Map<String, String> =
        mapOf("error" to (ex.message ?: "Invalid request"))
}
