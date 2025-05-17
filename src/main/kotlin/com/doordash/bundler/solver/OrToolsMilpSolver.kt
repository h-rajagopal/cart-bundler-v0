package com.doordash.bundler.solver

import com.doordash.bundler.model.BundleRequest
import com.doordash.bundler.model.ItemUnit
import com.google.ortools.sat.CpModel
import com.google.ortools.sat.CpSolver
import com.google.ortools.sat.CpSolverStatus
import com.google.ortools.sat.LinearExpr
import com.google.ortools.sat.IntVar
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * The Smart Menu Bundle Creator! 🍽️
 * 
 * Business View:
 * A smart catering manager that helps you plan group orders by:
 * - Finding the perfect mix of dishes
 * - Ensuring everyone gets enough food
 * - Staying within budget
 * - Managing kitchen workload
 * - Including popular items when possible
 * 
 * MILP (Mixed Integer Linear Programming) View:
 * A mathematical optimization model that:
 * - Uses integer variables for item quantities
 * - Enforces constraints as linear inequalities
 * - Minimizes a cost-based objective function
 * - Finds globally optimal solutions
 * 
 * Example Business Scenario:
 * Planning food for 50 people with:
 * - Mixed dietary requirements
 * - $15 per person budget
 * - Need good variety of dishes
 * 
 * Example MILP Formulation:
 * Variables:
 * - x[i] = number of units of item i to order (integer)
 * - y[i] = whether item i is selected (binary)
 * 
 * Constraints:
 * 1. Total Servings: ∑x[i] ≥ people
 * 2. Dietary: ∑x[i] where diet[i]=d ≥ required[d]
 * 3. Budget: ∑(price[i] * x[i]) ≤ budget
 * 4. Kitchen: ∑(load[i] * x[i]) ≤ capacity
 * 5. Distribution: |x[i] - x[j]| ≤ range * people
 * 
 * Objective:
 * Minimize: ∑(price[i] * x[i] - bonus[i] * y[i])
 * 
 * Features:
 * Business View:
 * - Fair Distribution: No single dish dominates
 * - Balanced Variety: Good mix of different items
 * - Smart Selection: Considers ratings and popularity
 * - Multiple Options: Several good combinations
 * 
 * MILP View:
 * - Binary Selection: y[i] ∈ {0,1}
 * - Quantity Bounds: 0 ≤ x[i] ≤ stock[i]
 * - Linking Constraints: x[i] > 0 ⟺ y[i] = 1
 * - Diversity: ∑y[i] ≥ min_items
 * 
 * How to Use:
 * Business View:
 * 1. Provide:
 *    - Number of people
 *    - Dietary needs
 *    - Budget
 *    - Desired options
 * 2. Receive:
 *    - Multiple menu combinations
 *    - Cost breakdowns
 *    - Portion distributions
 *    - Quality metrics
 * 
 * MILP View:
 * 1. Input:
 *    - Demand vector (people per diet)
 *    - Cost vector (price per item)
 *    - Capacity constraints
 *    - Binary indicators
 * 2. Output:
 *    - Optimal integer solution
 *    - Multiple feasible solutions
 *    - Solution metrics
 */
class OrToolsMilpSolver(
    private val items: List<ItemUnit>,
    private val req: BundleRequest,
    private val kitchenCap: Int,
    private val config: SolverConfig = SolverConfig(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        // Keep portions fair - no single item should dominate
        private const val MAX_SINGLE_ITEM_PERCENTAGE = 0.5  // Max 50% of total order for small orders
        private const val MIN_ITEM_PERCENTAGE = 0.1   // At least 10% if selected for small orders
        private const val MIN_DIFFERENT_ITEMS = 2      // Use at least 2 different items
        private const val TARGET_DISTRIBUTION_RANGE = 0.3  // Keep portion sizes within 30% of each other for small orders
    }

    /**
     * Finds the best possible combinations of menu items.
     * 
     * @param k How many different combinations to find
     * @return List of solutions, sorted from best to worst
     */
    fun solve(k: Int): List<Solution> {
        val foundSolutions = mutableListOf<Solution>()
        
        if (config.enableDetailedLogging) {
            logger.info("Looking for $k different menu combinations")
            logger.info("Solutions must be at least ${config.minSolutionDiversityPercent}% different")
            logger.info("Available menu items: ${items.size}")
            logger.info("Need to feed: ${req.people} people")
            logger.info("Dietary requirements: ${req.requiredByDiet}")
            logger.info("Budget per person: ${req.maxPricePerPersonCents} cents")
            logger.info("Kitchen capacity: $kitchenCap units")
        }

        // Try to find k different solutions
        for (iteration in 0 until k) {
            val solution = findSolution(foundSolutions)
            
            if (solution != null) {
                foundSolutions.add(solution)
                if (config.enableDetailedLogging) {
                    logger.info("Found combination ${iteration + 1}:")
                    logger.info("  Quality score: ${solution.optimalityScore}/100")
                    logger.info("  Total cost: ${solution.totalCost} cents")
                    logger.info("  Popular items: ${solution.popularItemsPercent}%")
                    logger.info("  Kitchen workload: ${solution.kitchenLoadPercent}%")
                }
            } else {
                if (config.enableDetailedLogging) {
                    logger.info("No more valid combinations found after $iteration attempts")
                }
                break
            }
        }

        return foundSolutions.sorted()
    }

    private fun findSolution(previousSolutions: List<Solution>): Solution? {
        // Create a new MILP model
        val model = CpModel()
        
        // Calculate size-dependent constraints
        val minItems = min(MIN_DIFFERENT_ITEMS, req.people)
        val maxItemPercentage = if (req.people <= 5) MAX_SINGLE_ITEM_PERCENTAGE else 0.25
        val minItemPercentage = if (req.people <= 5) MIN_ITEM_PERCENTAGE else 0.05
        val distributionRange = if (req.people <= 5) TARGET_DISTRIBUTION_RANGE else 0.15
        
        // DECISION VARIABLES
        // Primary Variables: x[i] = number of units to order for item i
        // - Domain: 0 to availableQty (integer)
        // - Business: How many of each menu item to include
        val itemVars = items.mapIndexed { idx, it -> 
            model.newIntVar(0, it.availableQty.toLong(), "x$idx")
        }

        // AUXILIARY VARIABLE
        // Total servings across all items: ∑x[i]
        // Used in multiple constraints for normalization
        val totalServings = LinearExpr.sum(itemVars.toTypedArray())

        // CONSTRAINT 1: Total Servings Requirement
        // Mathematical: ∑x[i] ≥ people
        // Business: Must have enough food for everyone
        model.addGreaterOrEqual(totalServings, req.people.toLong())

        // CONSTRAINT 2: Dietary Requirements
        // Mathematical: For each diet d: ∑x[i] where diet[i]=d ≥ required[d]
        // Business: Must meet each dietary need exactly
        req.requiredByDiet.forEach { (diet, need) ->
            val dietVars = items.withIndex()
                .filter { it.value.diet == diet }
                .map { itemVars[it.index] }
                .toTypedArray()
            if (dietVars.isNotEmpty()) {
                model.addGreaterOrEqual(LinearExpr.sum(dietVars), need.toLong())
            }
        }

        // CONSTRAINT 3: Budget Limit
        // Mathematical: ∑(price[i] * x[i]) ≤ budget_per_person * people
        // Business: Total cost must not exceed budget
        val priceCoeffs = items.map { it.priceCents.toLong() }.toLongArray()
        model.addLessOrEqual(
            LinearExpr.weightedSum(itemVars.toTypedArray(), priceCoeffs),
            req.maxPricePerPersonCents.toLong() * req.people
        )

        // CONSTRAINT 4: Kitchen Capacity
        // Mathematical: ∑(load[i] * x[i]) ≤ kitchen_capacity
        // Business: Don't overwhelm the kitchen
        val loadCoeffs = items.map { it.prepLoad.toLong() }.toLongArray()
        model.addLessOrEqual(
            LinearExpr.weightedSum(itemVars.toTypedArray(), loadCoeffs),
            kitchenCap.toLong()
        )

        // CONSTRAINT 5: Fair Distribution
        // Ensures portions are evenly distributed across selected items
        for (i in itemVars.indices) {
            for (j in (i + 1)..itemVars.lastIndex) {
                // BINARY VARIABLES
                // y[i] = whether item i is selected (0 or 1)
                val isFirstSelected = model.newBoolVar("is_selected_$i")
                val isSecondSelected = model.newBoolVar("is_selected_$j")

                // LINKING CONSTRAINTS
                // Mathematical: x[i] > 0 ⟺ y[i] = 1
                // Business: Track which items are actually used
                model.addGreaterThan(itemVars[i], 0).onlyEnforceIf(isFirstSelected)
                model.addEquality(itemVars[i], 0).onlyEnforceIf(isFirstSelected.not())
                model.addGreaterThan(itemVars[j], 0).onlyEnforceIf(isSecondSelected)
                model.addEquality(itemVars[j], 0).onlyEnforceIf(isSecondSelected.not())

                // AUXILIARY VARIABLE
                // z[i,j] = whether both items i and j are selected
                val bothSelected = model.newBoolVar("both_selected_${i}_$j")
                model.addBoolAnd(arrayOf(isFirstSelected, isSecondSelected)).onlyEnforceIf(bothSelected)
                model.addBoolOr(arrayOf(isFirstSelected.not(), isSecondSelected.not())).onlyEnforceIf(bothSelected.not())

                // DISTRIBUTION CONSTRAINT
                // Mathematical: |x[i] - x[j]| ≤ range * people when both selected
                // Business: Selected items should have similar quantities
                val targetRange = (req.people * distributionRange * 0.8).toLong()
                model.addLessOrEqual(
                    LinearExpr.weightedSum(arrayOf(itemVars[i], itemVars[j]), longArrayOf(1, -1)),
                    targetRange
                ).onlyEnforceIf(bothSelected)
                model.addLessOrEqual(
                    LinearExpr.weightedSum(arrayOf(itemVars[i], itemVars[j]), longArrayOf(-1, 1)),
                    targetRange
                ).onlyEnforceIf(bothSelected)
            }
        }

        // CONSTRAINT 6: Item Selection and Portion Sizes
        // Track selected items and enforce minimum/maximum portions
        val selectedItems = itemVars.mapIndexed { idx, itemVar ->
            model.newBoolVar("selected_$idx").also { isSelected ->
                // LINKING CONSTRAINTS
                model.addGreaterThan(itemVar, 0).onlyEnforceIf(isSelected)
                model.addEquality(itemVar, 0).onlyEnforceIf(isSelected.not())

                // MINIMUM PORTION CONSTRAINT
                // Mathematical: x[i] ≥ MIN_PERCENTAGE * total when selected
                // Business: Each selected item must be meaningful portion
                model.addGreaterOrEqual(
                    LinearExpr.term(itemVar, 100),
                    LinearExpr.term(totalServings, (minItemPercentage * 100).toLong())
                ).onlyEnforceIf(isSelected)

                // MAXIMUM PORTION CONSTRAINT
                // Mathematical: x[i] ≤ MAX_PERCENTAGE * total
                // Business: No single item should dominate
                model.addLessOrEqual(
                    LinearExpr.term(itemVar, 100),
                    LinearExpr.term(totalServings, (maxItemPercentage * 100).toLong())
                )
            }
        }

        // CONSTRAINT 7: Minimum Item Diversity
        // Mathematical: ∑y[i] ≥ MIN_DIFFERENT_ITEMS
        // Business: Must use at least 4 different items
        model.addGreaterOrEqual(
            LinearExpr.sum(selectedItems.toTypedArray()),
            minItems.toLong()
        )

        // CONSTRAINT 8: Solution Diversity
        // Ensures new solutions are sufficiently different from previous ones
        if (previousSolutions.isNotEmpty()) {
            for (prevSolution in previousSolutions) {
                val prevTotal = prevSolution.items.values.sum()
                
                // BINARY VARIABLES
                // d[i] = whether item i has different quantity (0 or 1)
                val diffVars = items.mapIndexed { idx, item ->
                    val prevQty = prevSolution.items[item] ?: 0
                    val diffVar = model.newBoolVar("diff_$idx")
                    
                    model.addDifferent(itemVars[idx], prevQty.toLong()).onlyEnforceIf(diffVar)
                    model.addEquality(itemVars[idx], prevQty.toLong()).onlyEnforceIf(diffVar.not())
                    
                    diffVar
                }

                // DIVERSITY CONSTRAINT
                // Mathematical: ∑d[i] ≥ minDifference
                // Business: At least 30% of items must be different
                val minDifference = max((prevTotal * config.minSolutionDiversityPercent / 100.0).roundToInt(), 1)
                model.addGreaterOrEqual(
                    LinearExpr.sum(diffVars.toTypedArray()),
                    minDifference.toLong()
                )
            }
        }

        // OBJECTIVE FUNCTION
        // Minimize: ∑(price[i] * BIG * x[i] - bonus[i] * y[i])
        // Business: Find cheapest combination while preferring:
        // - Popular items (+1 bonus)
        // - Highly rated items (+2 bonus)
        // - Good rated items (+1 bonus)
        // - Different items (+1 bonus)
        val BIG = 1_000L
        val objCoeffs = items.mapIndexed { idx, item ->
            val popularityBonus = if (item.isPopular()) 1 else 0
            val ratingBonus = when {
                item.isHighlyRated() -> 2  // Extra bonus for highly rated items
                item.hasGoodRating() -> 1  // Some bonus for good ratings
                else -> 0
            }
            val diversityBonus = 1  // Small bonus for using different items
            item.priceCents * BIG - (popularityBonus + ratingBonus + diversityBonus)
        }.map { it.toLong() }.toLongArray()
        
        model.minimize(LinearExpr.weightedSum(itemVars.toTypedArray(), objCoeffs))

        // SOLVE THE MODEL
        val solver = CpSolver()
        solver.parameters.maxTimeInSeconds = config.maxTimePerSolutionMs.toDouble() / 1000.0

        val startTime = System.currentTimeMillis()
        
        // BUILD AND RETURN SOLUTION
        return when (solver.solve(model)) {
            CpSolverStatus.OPTIMAL, CpSolverStatus.FEASIBLE -> {
                // Extract solution variables
                val itemQuantities = buildMap<ItemUnit, Int> {
                    itemVars.forEachIndexed { idx, itemVar -> 
                        val quantity = solver.value(itemVar).toInt()
                        if (quantity > 0) {
                            put(items[idx], quantity)
                        }
                    }
                }
                
                // Calculate solution metrics
                val totalCost = itemQuantities.entries.sumOf { it.key.priceCents * it.value }
                val totalLoad = itemQuantities.entries.sumOf { it.key.prepLoad * it.value }
                val popularItems = itemQuantities.entries.count { it.key.isPopular() }
                val totalItems = itemQuantities.values.sum()
                
                Solution(
                    items = itemQuantities,
                    totalCost = totalCost,
                    averageCostPerPerson = totalCost / req.people,
                    popularItemsPercent = (popularItems * 100.0) / totalItems,
                    kitchenLoadPercent = (totalLoad * 100.0) / kitchenCap,
                    optimalityScore = calculateOptimalityScore(
                        solution = itemQuantities,
                        totalCost = totalCost,
                        kitchenLoadPercent = (totalLoad * 100.0) / kitchenCap
                    ),
                    findingTimeMs = System.currentTimeMillis() - startTime
                )
            }
            else -> null
        }
    }

    /**
     * Calculates an optimality score (0-100) for a solution based on:
     * - Cost efficiency (25 points): How close to the budget without exceeding
     * - Popular items (20 points): Percentage of popular items used
     * - Highly rated items (20 points): Percentage of highly rated items
     * - Kitchen efficiency (15 points): How well kitchen capacity is utilized
     * - Item distribution (10 points): How evenly items are distributed
     * - Item diversity (10 points): How well distributed the items are
     */
    private fun calculateOptimalityScore(
        solution: Map<ItemUnit, Int>,
        totalCost: Int,
        kitchenLoadPercent: Double
    ): Int {
        val maxBudget = req.maxPricePerPersonCents * req.people
        val costScore = when {
            totalCost > maxBudget -> 0.0
            totalCost == 0 -> 0.0
            else -> (totalCost.toDouble() / maxBudget) * 25  // Reduced from 30 to make room for distribution score
        }

        val totalItems = solution.values.sum()
        val highlyRatedItems = solution.entries.count { (item, _) -> item.isHighlyRated() }
        val popularItems = solution.entries.count { (item, _) -> item.isPopular() }

        val ratingScore = (highlyRatedItems.toDouble() / totalItems) * 20
        val popularityScore = (popularItems.toDouble() / totalItems) * 20
        
        val kitchenScore = when {
            kitchenLoadPercent > 100 -> 0.0
            kitchenLoadPercent == 0.0 -> 0.0
            else -> (kitchenLoadPercent / 100.0) * 15
        }

        // Calculate distribution score based on how evenly items are distributed
        val quantities = solution.values.map { it.toDouble() / totalItems }
        val maxQty = quantities.maxOrNull() ?: 0.0
        val minQty = quantities.minOrNull() ?: 0.0
        val qtyRange = maxQty - minQty
        
        val minItems = min(MIN_DIFFERENT_ITEMS, req.people)
        val maxItemPercentage = if (req.people <= 5) MAX_SINGLE_ITEM_PERCENTAGE else 0.25
        
        val distributionScore = when {
            solution.size < minItems -> 0.0
            qtyRange > maxItemPercentage -> 0.0
            else -> 10.0 * (1.0 - (qtyRange / maxItemPercentage))
        }

        // Calculate diversity score based on number of different items
        val diversityScore = when {
            solution.size < minItems -> 0.0
            else -> 10.0 * (solution.size.toDouble() / (minItems * 2))
        }

        return (costScore + popularityScore + ratingScore + kitchenScore + distributionScore + diversityScore).toInt()
    }
} 