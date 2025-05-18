package com.doordash.bundler.controller

import com.doordash.bundler.dto.BundleRequestDTO
import com.doordash.bundler.dto.BundleResponse
import com.doordash.bundler.dto.BundleSolution
import com.doordash.bundler.dto.PlanLine
import com.doordash.bundler.dto.SolutionMetrics
import com.doordash.bundler.exception.BundleException
import com.doordash.bundler.service.BundleComparison
import com.doordash.bundler.service.BundleService
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1")
class BundleController(private val service: BundleService) {

    @PostMapping("/bundles")
    fun getBundles(@RequestBody req: BundleRequestDTO): ResponseEntity<BundleResponse> =
        runBlocking {
            try {
                val comparison = service.build(req)
                ResponseEntity.ok(comparison.toBundleResponse())
            } catch (e: BundleException) {
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
                    BundleResponse(
                        solutions = emptyList(),
                        solverType = req.solver,
                        findingTimeMs = 0,
                        message = "Could not find valid solutions: ${e.message}"
                    )
                )
            }
        }

    private fun BundleComparison.toBundleResponse(): BundleResponse {
        fun List<com.doordash.bundler.solver.Solution>.toBundleSolutions(): List<BundleSolution> =
            map { solution ->
                BundleSolution(
                    items = solution.items.entries.sortedBy { it.key.name }.map { (item, qty) ->
                        PlanLine(item.id, item.name, qty, item.priceCents)
                    },
                    metrics = SolutionMetrics(
                        optimalityScore = solution.optimalityScore,
                        totalCost = solution.totalCost,
                        averageCostPerPerson = solution.averageCostPerPerson,
                        popularItemsPercent = solution.popularItemsPercent,
                        kitchenLoadPercent = solution.kitchenLoadPercent,
                        findingTimeMs = solution.findingTimeMs
                    )
                )
            }

        return BundleResponse(
            solutions = solutions.toBundleSolutions(),
            solverType = solverType,
            findingTimeMs = findingTimeMs,
            message = "Found ${solutions.size} solutions using ${solverType.name} solver"
        )
    }
} 