package com.doordash.bundler.service

import com.doordash.bundler.dto.BundleRequestDTO
import com.doordash.bundler.dto.MenuItemDTO
import com.doordash.bundler.model.BundleRequest
import com.doordash.bundler.model.ItemUnit
import com.doordash.bundler.solver.GreedySolver
import com.doordash.bundler.solver.OrToolsMilpSolver
import com.doordash.bundler.solver.Solution
import com.doordash.bundler.solver.SolverConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Smart Menu Bundling Service
 * 
 * This service helps restaurants create perfect meal combinations for groups.
 * It tries two different approaches and recommends the best one for your needs.
 * 
 * Example Scenario:
 * ```
 * Input:
 * {
 *   "people": 20,
 *   "budgetCents": 2000,  // $20 per person
 *   "requiredByDiet": {
 *     "VEGAN": 5,
 *     "VEGETARIAN": 7,
 *     "MEAT": 8
 *   },
 *   "menu": [
 *     {
 *       "id": "bowl1",
 *       "name": "Buddha Bowl",
 *       "price": 1200,    // $12.00
 *       "serves": 1,
 *       "diet": "VEGAN",
 *       "popular": true
 *     },
 *     // ... more items ...
 *   ]
 * }
 * ```
 * 
 * How It Works:
 * 
 * 1. Pre-processing
 *    - Splits large items into individual servings
 *    - Example: Large pizza (serves 4) → 4 individual slices
 *    - Filters out items with no stock
 * 
 * 2. Try Advanced Optimizer (MILP)
 *    - Uses complex math to find perfect combinations
 *    - Considers all possible combinations at once
 *    - Usually finds the best possible solutions
 *    - Takes a bit longer (0.2-2 seconds typically)
 * 
 * 3. Try Quick Optimizer (Greedy)
 *    - Uses practical rules to find good combinations
 *    - Makes decisions one item at a time
 *    - Very fast (0.01-0.05 seconds typically)
 *    - Solutions are good but maybe not perfect
 * 
 * 4. Smart Recommendation
 *    The service suggests which solutions to use based on:
 * 
 *    a) Quality Difference:
 *       - Small (<15 points): Use quick solutions
 *       - Large (>15 points): Use advanced solutions
 *       - Example: If advanced=85, quick=72 → Use advanced
 * 
 *    b) Time Trade-off:
 *       - Fast (<10x): Always use better solutions
 *       - Slow (>10x): Consider both for different uses
 *       - Example: If advanced takes 1s, quick takes 0.05s
 *         → 20x slower, might want both options
 * 
 *    c) Success Rate:
 *       - If only one approach works, use it
 *       - If both work, compare quality and time
 *       - If neither works, suggest relaxing constraints
 * 
 * Example Output:
 * ```
 * {
 *   "milpSolutions": [
 *     {
 *       "items": [...],
 *       "metrics": {
 *         "optimalityScore": 85,
 *         "totalCost": 35000,    // $350.00
 *         "popularItemsPercent": 75.5,
 *         "kitchenLoadPercent": 60.0
 *       }
 *     }
 *   ],
 *   "greedySolutions": [
 *     {
 *       "items": [...],
 *       "metrics": {
 *         "optimalityScore": 72,
 *         "totalCost": 36000,    // $360.00
 *         "popularItemsPercent": 70.0,
 *         "kitchenLoadPercent": 65.0
 *       }
 *     }
 *   ],
 *   "recommendedApproach": "MILP",
 *   "recommendation": "Use the advanced solutions - they're 13 points 
 *                     better and only took 5x longer to find."
 * }
 * ```
 */
@Service
class BundleService(
    private val solverConfig: SolverConfig = SolverConfig()
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Creates optimal bundles using both approaches and recommends the best one.
     * 
     * Decision Process:
     * 1. Try both optimizers
     * 2. Compare solution quality
     * 3. Compare solution timing
     * 4. Make smart recommendation
     * 
     * Example Recommendations:
     * - "Use advanced solutions (15 points better, 5x slower)"
     * - "Use quick solutions (similar quality, 20x faster)"
     * - "Consider both (15 points better but 20x slower)"
     * - "Relax constraints (no valid solutions found)"
     *
     * @param dto The bundle request with menu items and requirements
     * @return Comparison of solutions with smart recommendation
     * @throws IllegalArgumentException if the menu is empty
     */
    fun build(dto: BundleRequestDTO): BundleComparison {
        val units = dto.menu.flatMap { it.toUnits() }.filter { it.availableQty > 0 }
        require(units.isNotEmpty()) { "menu empty" }

        val req = BundleRequest(
            people = dto.people,
            maxPricePerPersonCents = dto.maxPricePerPersonCents,
            requiredByDiet = dto.requiredByDiet,
            topN = dto.topN,
        )

        // Try both approaches
        val milpStartTime = System.currentTimeMillis()
        val milpSolver = OrToolsMilpSolver(units, req, dto.kitchenCap, solverConfig)
        val milpSolutions = milpSolver.solve(req.topN)
        val milpTime = System.currentTimeMillis() - milpStartTime

        val greedyStartTime = System.currentTimeMillis()
        val greedySolver = GreedySolver(units, req, dto.kitchenCap)
        val greedySolutions = greedySolver.solve(req.topN)
        val greedyTime = System.currentTimeMillis() - greedyStartTime

        // Analyze results and make a recommendation
        val recommendation = when {
            milpSolutions.isEmpty() && greedySolutions.isEmpty() -> 
                BundleComparison(
                    milpSolutions = emptyList(),
                    greedySolutions = emptyList(),
                    recommendedApproach = "NONE",
                    recommendation = "No valid solutions found with either approach. " +
                        "Consider relaxing constraints (budget, dietary requirements, etc.)"
                )

            milpSolutions.isEmpty() -> 
                BundleComparison(
                    milpSolutions = emptyList(),
                    greedySolutions = greedySolutions,
                    recommendedApproach = "GREEDY",
                    recommendation = "Only the quick approach found solutions. " +
                        "The advanced optimizer couldn't find valid combinations - " +
                        "your constraints might be too strict."
                )

            greedySolutions.isEmpty() ->
                BundleComparison(
                    milpSolutions = milpSolutions,
                    greedySolutions = emptyList(),
                    recommendedApproach = "MILP",
                    recommendation = "Only the advanced optimizer found solutions. " +
                        "This suggests your requirements need careful optimization."
                )

            else -> {
                // Compare solution quality and timing
                val bestMilp = milpSolutions.maxOf { it.optimalityScore }
                val bestGreedy = greedySolutions.maxOf { it.optimalityScore }
                val scoreDiff = bestMilp - bestGreedy
                val timeRatio = milpTime.toDouble() / greedyTime

                when {
                    scoreDiff > 15 && timeRatio < 10 ->
                        BundleComparison(
                            milpSolutions = milpSolutions,
                            greedySolutions = greedySolutions,
                            recommendedApproach = "MILP",
                            recommendation = "Use the advanced optimizer solutions. " +
                                "They're significantly better (${scoreDiff} points) " +
                                "and only took ${String.format("%.1f", timeRatio)}x longer."
                        )

                    scoreDiff > 15 ->
                        BundleComparison(
                            milpSolutions = milpSolutions,
                            greedySolutions = greedySolutions,
                            recommendedApproach = "BOTH",
                            recommendation = "Consider both options. Advanced solutions are better " +
                                "but took ${String.format("%.1f", timeRatio)}x longer. " +
                                "Use quick solutions for testing, advanced for final orders."
                        )

                    else ->
                        BundleComparison(
                            milpSolutions = milpSolutions,
                            greedySolutions = greedySolutions,
                            recommendedApproach = "GREEDY",
                            recommendation = "Use the quick solutions. " +
                                "They're nearly as good (only ${scoreDiff} points lower) " +
                                "but ${String.format("%.1f", timeRatio)}x faster!"
                        )
                }
            }
        }

        if (solverConfig.enableDetailedLogging) {
            logger.info("Solution comparison:")
            logger.info("MILP: ${milpSolutions.size} solutions in ${milpTime}ms")
            logger.info("Greedy: ${greedySolutions.size} solutions in ${greedyTime}ms")
            logger.info("Recommendation: ${recommendation.recommendedApproach}")
            logger.info("Reason: ${recommendation.recommendation}")
        }

        return recommendation
    }
}

/**
 * Converts a menu item into individual servings.
 * 
 * Example:
 * ```
 * Input: Large Pizza
 * - Serves: 4
 * - Price: $20.00
 * - Stock: 10 pizzas
 * - Rating: 90% (900 upvotes, 100 downvotes)
 * 
 * Output: 4 Pizza Slices
 * - Each serves: 1
 * - Each price: $5.00 (20/4)
 * - Each stock: 40 slices (10*4)
 * - Each rating: 90% (same rating applies to each slice)
 * ```
 */
private fun MenuItemDTO.toUnits(): List<ItemUnit> {
    if (serves <= 0 || availableServings == 0) return emptyList()
    val perSliceStock = availableServings / serves
    if (perSliceStock == 0) return emptyList()
    val unitPrice = (priceCents + serves - 1) / serves

    // Calculate rating percentage
    val itemRating = rating?.let {
        val voteCount = it.upvoteCount + it.downvoteCount
        when (voteCount == 0L) {
            true -> 0.0F
            false -> it.upvoteCount.toFloat() / voteCount
        }
    } ?: 0.0F

    return List(serves) { idx ->
        ItemUnit(
            id = "$id#${idx + 1}",
            name = if (serves == 1) name else "$name (slice ${idx + 1}/$serves)",
            priceCents = unitPrice,
            diet = diet,
            availableQty = perSliceStock,
            prepLoad = prepLoadPerServing,
            upvoteCount = rating?.upvoteCount ?: 0,
            downvoteCount = rating?.downvoteCount ?: 0,
            reviewCount = rating?.reviewCount ?: 0,
            ratingPercentage = itemRating
        )
    }
}

/**
 * Holds solutions from both approaches and recommendation.
 * 
 * Example:
 * ```
 * {
 *   milpSolutions: [
 *     Solution(score=85, cost=$350),
 *     Solution(score=82, cost=$355)
 *   ],
 *   greedySolutions: [
 *     Solution(score=72, cost=$360)
 *   ],
 *   recommendedApproach: "MILP",
 *   recommendation: "Use advanced - 13 points better"
 * }
 * ```
 */
data class BundleComparison(
    val milpSolutions: List<Solution>,
    val greedySolutions: List<Solution>,
    val recommendedApproach: String,
    val recommendation: String
) 