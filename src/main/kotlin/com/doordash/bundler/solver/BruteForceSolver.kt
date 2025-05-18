package com.doordash.bundler.solver

import com.doordash.bundler.model.BundleRequest
import com.doordash.bundler.model.DietaryTag
import com.doordash.bundler.model.ItemUnit
import org.slf4j.LoggerFactory
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The Exhaustive Menu Bundle Explorer! 🧐🍽️
 *
 * Business View:
 * A thorough food planner that tries every valid combination of menu items to ensure
 * you get the absolute best menu bundle for your group order. It's like having a chef
 * who considers every possible dish combination before deciding on the perfect menu.
 *
 * Technical View:
 * A recursive backtracking algorithm that performs an optimized exhaustive search to:
 * - Explore the full solution space of possible menu combinations
 * - Prune invalid branches early to improve performance
 * - Apply constraint validation at each step
 * - Score and rank all valid solutions
 *
 * Example Business Scenario:
 * Finding the best menu for a small corporate lunch with:
 * - 8 people with mixed dietary requirements
 * - $15 per person budget
 * - Need to consider kitchen load and food popularity
 *
 * How This Solver Works:
 * 1. Pre-Processing:
 *    - Sort items to prioritize based on:
 *    - Items needed for required dietary constraints
 *    - Popular items
 *    - Items with high ratings
 * 2. Recursive Exploration:
 *    - For each menu item, try different quantities (0 to max possible)
 *    - Check constraints at each step (budget, kitchen capacity)
 *    - Backtrack when constraints are violated
 *    - Store valid solutions as they're found
 *
 * 3. Solution Validation:
 *    - Ensure all dietary requirements are met
 *    - Verify sufficient servings for all people
 *    - Check fair distribution of items
 *    - Validate item diversity and reasonable portion sizes
 *
 * 4. Solution Scoring:
 *    - Calculate optimality score based on multiple factors
 *    - Return the best solutions found
 *
 * When To Use:
 * ✓ Smaller menus with fewer items (<15 items)
 * ✓ Smaller group sizes (<10 people)
 * ✓ Need guaranteed optimal solutions
 * ✓ Have time for more thorough computation
 *
 * Limitations:
 * - Exponential time complexity (O(n^m) where n is items and m is max quantity)
 * - Not suitable for very large menus or large group sizes
 * - Performance degrades rapidly as menu size increases
 *
 * @param items Available menu items after splitting into individual servings
 * @param req Requirements for number of people, dietary needs, budget, etc.
 * @param kitchenCap Maximum kitchen preparation capacity
 */
class BruteForceSolver(
    private val items: List<ItemUnit>,
    private val req: BundleRequest,
    private val kitchenCap: Int,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        /** Maximum number of items per type to avoid excessive quantities of one item */
        private const val MAX_ITEMS_PER_TYPE = 20
        
        /** Minimum portion size (as percentage) for small groups (≤5 people) */
        private const val MIN_ITEM_PERCENTAGE_SMALL = 0.1   // For small groups
        
        /** Maximum portion size (as percentage) for small groups (≤5 people) */
        private const val MAX_ITEM_PERCENTAGE_SMALL = 0.5   // For small groups
        
        /** Minimum portion size (as percentage) for large groups (>5 people) */
        private const val MIN_ITEM_PERCENTAGE_LARGE = 0.05  // For large groups
        
        /** Maximum portion size (as percentage) for large groups (>5 people) */
        private const val MAX_ITEM_PERCENTAGE_LARGE = 0.25  // For large groups
        
        /** Minimum number of different items to include for variety */
        private const val MIN_DIFFERENT_ITEMS = 2
        
        /** Maximum allowed distribution range between item quantities for small groups */
        private const val TARGET_DISTRIBUTION_RANGE_SMALL = 0.3  // For small groups
        
        /** Maximum allowed distribution range between item quantities for large groups */
        private const val TARGET_DISTRIBUTION_RANGE_LARGE = 0.15 // For large groups
        
        /** Threshold that determines what constitutes a small vs. large group */
        private const val SMALL_GROUP_THRESHOLD = 5
    }

    /**
     * Finds the best possible combinations of menu items through exhaustive search.
     *
     * Process:
     * 1. First sorts items to prioritize based on:
     *    - Items needed for required dietary constraints
     *    - Popular items
     *    - Items with high ratings
     * 2. Recursively explores all valid combinations
     * 3. Sorts solutions by optimality score
     * 
     * Complexity:
     * - Time: O(n^m) where n = number of items, m = max quantity per item
     * - Space: O(n*s) where s = number of solutions stored
     *
     * @param k How many different combinations to find
     * @return List of solutions, sorted from best to worst
     */
    fun solve(k: Int): List<Solution> {
        val solutions = mutableListOf<Solution>()
        val startTime = System.currentTimeMillis()

        // First, sort items to prioritize important ones:
        // 1. Items needed for dietary requirements first
        // 2. Popular items next
        // 3. Highly rated items next
        val sortedItems = items.sortedWith(compareBy(
            { req.requiredByDiet[it.diet] ?: 0 },
            { -it.isPopular().compareTo(false) },
            { -it.ratingPercentage }
        ))

        // Start the recursive exploration with an empty combination
        generateAndCheckCombinations(
            currentIndex = 0,
            currentCombination = mutableMapOf(),
            solutions = solutions,
            k = k,
            startTime = startTime,
            items = sortedItems
        )

        // Return the k best solutions, sorted by score
        return solutions
            .sortedDescending()
            .take(k)
    }

    /**
     * Core recursive function that explores all possible item combinations.
     *
     * This is where the backtracking algorithm is implemented:
     * 1. Base cases:
     *    - We've found enough solutions (k)
     *    - We've processed all items (reached the end of the list)
     * 2. For the current item, try all possible quantities (0 to max)
     * 3. For each quantity, check if it's valid to add
     * 4. If valid, add to the current combination and recurse
     * 5. After recursion, backtrack by removing the item (if added)
     *
     * @param currentIndex Index of the current item being processed
     * @param currentCombination Current combination being built
     * @param solutions List to store valid solutions
     * @param k Number of solutions needed
     * @param startTime Start time for performance measurement
     * @param items Sorted list of available items
     */
    private fun generateAndCheckCombinations(
        currentIndex: Int,
        currentCombination: MutableMap<ItemUnit, Int>,
        solutions: MutableList<Solution>,
        k: Int,
        startTime: Long,
        items: List<ItemUnit>
    ) {
        // Stop if we have enough solutions
        if (solutions.size >= k) return

        // If we've processed all items, check if the current combination is valid
        if (currentIndex >= items.size) {
            if (isValidCombination(currentCombination)) {
                solutions.add(createSolution(currentCombination, startTime))
            }
            return
        }

        val currentItem = items[currentIndex]
        // Calculate the maximum quantity we can include of this item
        val maxQuantity = calculateMaxQuantity(currentItem, currentCombination)

        // Try each possible quantity (0 to maxQuantity)
        for (quantity in 0..maxQuantity) {
            // Check if we can add this quantity (early pruning)
            if (!canAdd(currentItem, quantity, currentCombination)) break

            // Add the item to the combination if quantity > 0
            if (quantity > 0) currentCombination[currentItem] = quantity

            // Recurse to the next item
            generateAndCheckCombinations(
                currentIndex + 1,
                currentCombination,
                solutions,
                k,
                startTime,
                items
            )

            // Backtrack: remove the item if it was added
            if (quantity > 0) currentCombination.remove(currentItem)
            
            // Early termination if we found enough solutions
            if (solutions.size >= k) return
        }
    }

    /**
     * Calculates the maximum quantity of an item that can be added.
     * 
     * This function applies multiple limits:
     * 1. Item availability (stock)
     * 2. Budget constraints
     * 3. Kitchen capacity constraints
     * 4. Maximum items per type limit
     *
     * @param item The item to calculate maximum quantity for
     * @param currentCombination Current combination of items
     * @return Maximum quantity that can be added
     */
    private fun calculateMaxQuantity(
        item: ItemUnit,
        currentCombination: Map<ItemUnit, Int>
    ): Int {
        // Calculate used budget so far
        val usedBudget = currentCombination.entries.sumOf { it.key.priceCents * it.value }
        // Calculate remaining budget
        val remainingBudget = req.maxPricePerPersonCents * req.people - usedBudget
        // Maximum quantity allowed by budget
        val maxByBudget = remainingBudget / item.priceCents

        // Calculate used kitchen load so far
        val usedLoad = currentCombination.entries.sumOf { it.key.prepLoad * it.value }
        // Calculate remaining kitchen capacity
        val remainingLoad = kitchenCap - usedLoad
        // Maximum quantity allowed by kitchen capacity
        val maxByCapacity = remainingLoad / item.prepLoad

        // Return the most restrictive limit
        return minOf(
            item.availableQty,                  // Don't exceed available stock
            maxByBudget.coerceAtLeast(0),      // Don't exceed budget
            maxByCapacity.coerceAtLeast(0),    // Don't exceed kitchen capacity
            MAX_ITEMS_PER_TYPE                 // Don't use too many of one item
        )
    }

    /**
     * Checks if a specific quantity of an item can be added to the current combination.
     * 
     * This is an early pruning optimization that quickly checks basic constraints
     * to avoid unnecessary recursion.
     *
     * @param item Item to check
     * @param quantity Quantity to add
     * @param currentCombination Current combination of items
     * @return True if the item can be added, false otherwise
     */
    private fun canAdd(
        item: ItemUnit,
        quantity: Int,
        currentCombination: Map<ItemUnit, Int>
    ): Boolean {
        // If not adding any, it's always valid
        if (quantity == 0) return true

        // Check kitchen capacity constraint
        val currentLoad = currentCombination.entries.sumOf { it.key.prepLoad * it.value }
        if (currentLoad + item.prepLoad * quantity > kitchenCap) return false

        // Check budget constraint
        val currentCost = currentCombination.entries.sumOf { it.key.priceCents * it.value }
        val maxBudget = req.maxPricePerPersonCents * req.people
        if (currentCost + item.priceCents * quantity > maxBudget) return false

        // If passed all checks, we can add this item
        return true
    }

    /**
     * Validates if a combination meets all requirements and constraints.
     * 
     * Performs comprehensive validation including:
     * 1. Serving sufficient quantity for all people
     * 2. Meeting all dietary requirements
     * 3. Having minimum item diversity
     * 4. Ensuring fair distribution of items
     * 5. Enforcing minimum and maximum portion sizes
     *
     * @param combination The combination to validate
     * @return True if the combination is valid, false otherwise
     */
    private fun isValidCombination(combination: Map<ItemUnit, Int>): Boolean {
        // Empty combinations are invalid
        if (combination.isEmpty()) return false

        // Must have enough total servings
        val totalServings = combination.values.sum()
        if (totalServings < req.people) return false

        // Must have minimum item diversity
        if (combination.size < MIN_DIFFERENT_ITEMS) return false

        // Check dietary requirements
        val byDiet = combination.entries
            .groupBy { it.key.diet }
            .mapValues { it.value.sumOf { e -> e.value } }

        // Each dietary requirement must be satisfied
        req.requiredByDiet.forEach { (diet, needed) ->
            val have = byDiet[diet] ?: 0
            if (have < needed) return false
        }

        // Calculate appropriate distribution parameters based on group size
        val minItemPercentage = if (req.people <= SMALL_GROUP_THRESHOLD) 
            MIN_ITEM_PERCENTAGE_SMALL else MIN_ITEM_PERCENTAGE_LARGE
        val maxItemPercentage = if (req.people <= SMALL_GROUP_THRESHOLD) 
            MAX_ITEM_PERCENTAGE_SMALL else MAX_ITEM_PERCENTAGE_LARGE
        val targetDistributionRange = if (req.people <= SMALL_GROUP_THRESHOLD) 
            TARGET_DISTRIBUTION_RANGE_SMALL else TARGET_DISTRIBUTION_RANGE_LARGE

        // Calculate item distribution percentages
        val quantities = combination.values.map { it.toDouble() / totalServings }
        val maxQty = quantities.maxOrNull() ?: 0.0
        val minQty = quantities.minOrNull() ?: 0.0
        val qtyRange = maxQty - minQty

        // Enforce fair distribution range
        if (qtyRange > targetDistributionRange) return false

        // Enforce per-item min/max percentages
        for (qty in quantities) {
            if (qty < minItemPercentage || qty > maxItemPercentage) {
                return false
            }
        }

        // If all checks pass, the combination is valid
        return true
    }

    /**
     * Creates a Solution object from a valid combination.
     * 
     * Calculates all relevant metrics for the solution:
     * - Total cost
     * - Average cost per person
     * - Popularity percentage
     * - Kitchen load percentage
     * - Overall optimality score
     *
     * @param combination The validated combination
     * @param startTime Start time for performance tracking
     * @return A Solution object with all metrics calculated
     */
    private fun createSolution(
        combination: Map<ItemUnit, Int>,
        startTime: Long
    ): Solution {
        // Calculate total cost and kitchen load
        val totalCost = combination.entries.sumOf { it.key.priceCents * it.value }
        val totalLoad = combination.entries.sumOf { it.key.prepLoad * it.value }

        // Create and return the solution with all metrics
        return Solution(
            items = combination.toMap(),
            totalCost = totalCost,
            averageCostPerPerson = totalCost / req.people,
            popularItemsPercent = (combination.count { it.key.isPopular() } * 100.0) / combination.size,
            kitchenLoadPercent = totalLoad * 100.0 / kitchenCap,
            optimalityScore = calculateOptimalityScore(
                combination,
                totalCost,
                totalLoad * 100.0 / kitchenCap
            ),
            findingTimeMs = System.currentTimeMillis() - startTime
        )
    }

    /**
     * Calculates an optimality score (0-100) for a solution.
     * 
     * The score is based on multiple weighted factors:
     * - Cost efficiency (25 points): How close to the budget without exceeding
     * - Highly rated items (20 points): Percentage of highly rated items used
     * - Popular items (20 points): Percentage of popular items used
     * - Kitchen efficiency (15 points): How well kitchen capacity is utilized
     * - Item distribution (10 points): How evenly items are distributed
     * - Item diversity (10 points): How many different items are included
     *
     * @param combination The item combination
     * @param totalCost Total cost of the combination
     * @param kitchenLoadPercent Percentage of kitchen capacity used
     * @return Optimality score (0-100)
     */
    private fun calculateOptimalityScore(
        combination: Map<ItemUnit, Int>,
        totalCost: Int,
        kitchenLoadPercent: Double
    ): Int {
        // Budget utilization score (25 points max)
        val maxBudget = req.maxPricePerPersonCents * req.people
        val costScore = if (totalCost in 1..maxBudget) (totalCost.toDouble()
                / maxBudget) * 25 else 0.0

        // Item quality statistics
        val uniqueItems = combination.size
        val highlyRated = combination.count { it.key.isHighlyRated() }
        val popular = combination.count { it.key.isPopular() }

        // Item quality scores (40 points max)
        val ratingScore = if (uniqueItems > 0) (highlyRated.toDouble() / uniqueItems) * 20 else 0.0
        val popularityScore = if (uniqueItems > 0) (popular.toDouble() / uniqueItems) * 20 else 0.0
        
        // Kitchen utilization score (15 points max)
        val kitchenScore = if (kitchenLoadPercent in 1.0..100.0) (kitchenLoadPercent / 100) * 15 else 0.0

        // Distribution fairness score (10 points max)
        val totalServings = combination.values.sum()
        val fracs = combination.values.map { it.toDouble() / totalServings }
        val range = (fracs.maxOrNull() ?: 0.0) - (fracs.minOrNull() ?: 0.0)
        val distributionScore = if (range <= TARGET_DISTRIBUTION_RANGE_SMALL) {
            10 * (1 - range / TARGET_DISTRIBUTION_RANGE_SMALL)
        } else {
            0.0
        }

        // Item diversity score (10 points max)
        val minNeeded = max(MIN_DIFFERENT_ITEMS, req.people / 5)
        val diversityScore = if (uniqueItems >= MIN_DIFFERENT_ITEMS) {
            10 * (uniqueItems.toDouble() / minNeeded).coerceAtMost(1.0)
        } else {
            0.0
        }

        // Total score (0-100 points)
        return (costScore + ratingScore + popularityScore + kitchenScore + distributionScore + diversityScore)
            .roundToInt()
    }
}
