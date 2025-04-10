package com.qut.webservices.igalogicservices.controller

import com.qut.webservices.igalogicservices.dataaccess.HashDataAccess
import com.qut.webservices.igalogicservices.dataaccess.LogDataAccess
import com.qut.webservices.igalogicservices.models.ExecuteRulesRequest
import com.qut.webservices.igalogicservices.models.ExecuteRulesResponse
import com.qut.webservices.igalogicservices.models.PositionChangeImpactsRequest
import com.qut.webservices.igalogicservices.models.PositionChangeImpactsResponse
import com.qut.webservices.igalogicservices.models.ResolveDataRequirementsRequest
import com.qut.webservices.igalogicservices.models.StatusUpdates
import com.qut.webservices.igalogicservices.rules.core.StateManager
import com.qut.webservices.igalogicservices.rules.identity.ChangeRouter
import com.qut.webservices.igalogicservices.rules.identity.RulesExecutor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class LogicServicesController @Autowired constructor(
    private val changeRouter: ChangeRouter,
    val rulesExecutor: RulesExecutor,
    val logDataAccess: LogDataAccess,
    val hashDataAccess: HashDataAccess,
    val stateManager: StateManager
) {
    private val logger: Logger = LoggerFactory.getLogger(LogicServicesController::class.java)

    @PostMapping("/resolveDataRequirements")
    fun resolveDataRequirements(@RequestBody resolveDataRequirementsRequest: ResolveDataRequirementsRequest): Any {
        logger.debug(
            "resolveDataRequirements called with correlationId: {}, fullRefresh: {}, identity: {}",
            resolveDataRequirementsRequest.correlationId,
            resolveDataRequirementsRequest.fullRefresh,
            resolveDataRequirementsRequest.identity
        )
        return changeRouter.resolveDataRequirements(resolveDataRequirementsRequest)
    }

    @PostMapping("/executeRules")
    fun executeRules(@RequestBody executeRulesRequest: ExecuteRulesRequest): ExecuteRulesResponse {
        logger.debug(
            "executeRules called with correlationId: {}, executeRulesRequest: {}",
            executeRulesRequest.correlationId,
            executeRulesRequest
        )
        return rulesExecutor.executeRules(executeRulesRequest)
    }

    @GetMapping("/log")
    fun queryLog(
        @RequestParam(required = false) igaUserId: String? = null,
        @RequestParam(required = false) correlationId: String? = null,
        @RequestParam(required = false) fromDate: LocalDate? = null,
        @RequestParam(required = false) toDate: LocalDate? = null,
    ): Any {
        logger.debug(
            "queryLog called with igaUserId={}, correlationId={}, fromDate={}, toDate={}",
            igaUserId,
            correlationId,
            fromDate,
            toDate
        )
        return logDataAccess.getLogMessages(igaUserId, correlationId, fromDate, toDate)
    }

    @DeleteMapping("/hash/{igaUserId}")
    fun deleteHashes(@PathVariable igaUserId: String): Any {
        logger.debug("deleteHashes called with $igaUserId")
        return hashDataAccess.clearHashes(igaUserId)
    }

    @PutMapping("/igaState/{igaUserId}")
//    Boomi can't do Patch
    @PatchMapping("/igaState/{igaUserId}")
    fun patchState(@PathVariable igaUserId: String, @RequestBody stateUpdates: StatusUpdates) {
        logger.debug("patchState called with $igaUserId")
        for (update in stateUpdates.stateUpdates) {
            stateManager.updateStateObjectStatus(igaUserId, update.hash, update.update)
        }
    }

    @PostMapping("/position-change-impacts")
    fun calculatePositionChangeImpact(@RequestBody request: PositionChangeImpactsRequest): ResponseEntity<PositionChangeImpactsResponse> {
        logger.debug(
            "/position-change-impacts called with request: {}",
            request
        )
        val response = PositionChangeImpactsResponse()
        return ResponseEntity.ok(response)
    }
}
