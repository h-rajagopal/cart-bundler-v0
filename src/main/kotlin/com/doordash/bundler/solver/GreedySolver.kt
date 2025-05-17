package com.doordash.bundler.solver

import com.doordash.bundler.exception.BundleException
import com.doordash.bundler.model.BundleRequest
import com.doordash.bundler.model.DietaryTag
import com.doordash.bundler.model.ItemUnit
import kotlin.random.Random

/**
 * The Quick and Smart Bundle Finder!
 * 
 * Imagine you're a chef planning meals for a large group:
 * 1. First, you make sure everyone with dietary requirements gets their food
 * 2. Then, you fill in the rest with good value items that people like
 * 3. Finally, you try different variations to give options
 * 
 * Real-World Example:
 * ```
 * Order for 20 people:
 * - 5 need vegan meals
 * - 7 need vegetarian meals
 * - 8 can eat anything
 * 
 * Quick Solution #1:
 * → First, handle dietary needs:
 *   - 5 vegan Buddha bowls (for vegan requirement)
 *   - 7 vegetarian pizzas (for vegetarian requirement)
 * → Then fill remaining 8 seats:
 *   - 6 popular chicken dishes
 *   - 2 more vegetarian pizzas (good value)
 * 
 * Quick Solution #2:
 * → Different mix for variety:
 *   - 5 vegan stir-fries
 *   - 7 vegetarian pastas
 *   - 4 chicken dishes
 *   - 4 beef dishes
 * ```
 * 
 * How It Makes Decisions:
 * 
 * 1. Dietary Requirements First
 *    - Starts with most restrictive (vegan)
 *    - Then vegetarian
 *    - Finally regular items
 *    - For each type, prefers:
 *      → Popular items (people like them!)
 *      → Cost-effective items (stay in budget)
 * 
 * 2. Smart Filling Strategy
 *    - After dietary needs are met
 *    - Looks for items that:
 *      → Are popular (happy customers)
 *      → Fit the remaining budget
 *      → Won't overwork the kitchen
 * 
 * 3. Multiple Solutions
 *    - Uses controlled randomness
 *    - Tries different popular items
 *    - Varies the mix of items
 *    - Each solution is valid but different
 * 
 * Scoring System:
 * - Base score: 60 points (for any valid solution)
 * - Bonus points: Up to 20 extra for:
 *   → Using budget efficiently (8 points)
 *   → Including popular items (6 points)
 *   → Balancing kitchen load (6 points)
 * 
 * When to Use This Solver:
 * ✓ Need solutions very quickly
 * ✓ Simple menu without complex interactions
 * ✓ Testing different scenarios
 * ✓ Budget is a major concern
 * 
 * Limitations to Keep in Mind:
 * - Might miss some clever combinations
 * - Focuses on one item at a time
 * - May not find the absolute best deal
 * - Solutions are good, but maybe not perfect
 * 
 * @param items Available menu items after splitting into individual servings
 * @param req Requirements for number of people, dietary needs, etc.
 * @param kitchenCap Maximum kitchen preparation capacity
 */
class GreedySolver(
    private val items: List<ItemUnit>,
    private val req: BundleRequest,
    private val kitchenCap: Int,
) {
    /**
     * Finds multiple valid combinations of menu items.
     * Uses smart randomization to create different but still good solutions.
     *
     * Example Output:
     * ```
     * Solution 1 (Score: 75):
     * - 5 Vegan Bowls ($12 each)
     * - 7 Veggie Pizzas ($15 each)
     * - 8 Chicken Dishes ($18 each)
     * → Total: $330, 80% popular items
     * 
     * Solution 2 (Score: 72):
     * - 5 Vegan Stir-Fries ($10 each)
     * - 7 Veggie Pastas ($14 each)
     * - 4 Chicken Dishes ($18 each)
     * - 4 Beef Dishes ($20 each)
     * → Total: $338, 70% popular items
     * ```
     *
     * @param k How many different combinations to try to find
     * @return List of solutions, sorted by their quality score
     */
    fun solve(k: Int): List<Solution> {
        val solutions = mutableListOf<Solution>()
        val random = Random(System.currentTimeMillis())

        // Try to find k different solutions
        for (i in 0 until k) {
            try {
                val startTime = System.currentTimeMillis()
                
                // Introduce some randomness in item selection to get different solutions
                val randomizedItems = items.map { it to random.nextDouble() }
                    .sortedBy { (item, rand) -> 
                        // Mix of sorting criteria and randomness:
                        // 1. First by dietary requirement (VEGAN > VEGETARIAN > MEAT)
                        // 2. Then by rating (highly rated first)
                        // 3. Then by popularity
                        // 4. Then by price
                        // 5. Finally add some randomness
                        val ratingScore = if (item.isHighlyRated()) "0" else "1"
                        val popularityScore = if (item.isPopular()) "0" else "1"
                        item.diet.toString() + 
                        ratingScore +
                        popularityScore +
                        String.format("%03d", item.priceCents) +
                        String.format("%03d", (rand * 100).toInt())
                    }
                    .map { it.first }

                val solution = findOneSolution(randomizedItems)
                
                // Calculate metrics for this solution
                val totalCost = solution.entries.sumOf { it.key.priceCents * it.value }
                val totalLoad = solution.entries.sumOf { it.key.prepLoad * it.value }
                val popularItems = solution.entries.count { it.key.isPopular() }
                val highlyRatedItems = solution.entries.count { it.key.isHighlyRated() }
                val totalItems = solution.values.sum()

                solutions.add(Solution(
                    items = solution,
                    totalCost = totalCost,
                    averageCostPerPerson = totalCost / req.people,
                    popularItemsPercent = (popularItems * 100.0) / totalItems,
                    kitchenLoadPercent = (totalLoad * 100.0) / kitchenCap,
                    // Greedy solutions get a base score of 60, plus up to 20 points for efficiency
                    optimalityScore = 60 + calculateEfficiencyBonus(
                        totalCost = totalCost,
                        popularItemsPercent = (popularItems * 100.0) / totalItems,
                        highlyRatedItemsPercent = (highlyRatedItems * 100.0) / totalItems,
                        kitchenLoadPercent = (totalLoad * 100.0) / kitchenCap
                    ),
                    findingTimeMs = System.currentTimeMillis() - startTime
                ))
            } catch (e: BundleException) {
                // If we can't find more solutions, return what we have
                break
            }
        }

        return solutions
    }

    /**
     * Finds one valid combination of menu items.
     * Uses the provided randomized item list to make this solution different.
     *
     * Example:
     * ```
     * Input:
     * - Need 5 vegan, 7 vegetarian meals
     * - Budget $20 per person
     * - Kitchen capacity 100 units
     * 
     * Process:
     * 1. First, vegan meals:
     *    → Found Buddha Bowls ($12, load 2)
     *    → Can add 5 within budget/capacity
     * 
     * 2. Then vegetarian:
     *    → Found Veggie Pizza ($15, load 3)
     *    → Can add 7 within budget/capacity
     * 
     * 3. Fill remaining:
     *    → Found Chicken ($18, load 2)
     *    → Can add 8 within budget/capacity
     * ```
     *
     * @param randomizedItems List of items in a random order
     * @return Map of items to their quantities
     * @throws BundleException if no valid solution can be found
     */
    private fun findOneSolution(randomizedItems: List<ItemUnit>): Map<ItemUnit, Int> {
        val remaining = req.requiredByDiet.toMutableMap()
        val plan = mutableMapOf<ItemUnit, Int>()
        var people = 0
        var cost = 0
        var load = 0

        fun canAdd(it: ItemUnit) = 
            (plan[it] ?: 0) < it.availableQty && // Don't exceed stock
            load + it.prepLoad <= kitchenCap && // Respect kitchen capacity
            (cost + it.priceCents) <= req.maxPricePerPersonCents * (people + 1) // Stay within budget

        // Step 1: Satisfy dietary requirements
        for (diet in listOf(DietaryTag.VEGAN, DietaryTag.VEGETARIAN, DietaryTag.MEAT)) {
            var need = remaining[diet] ?: 0
            val pool = randomizedItems.filter { it.diet == diet }

            for (u in pool) {
                while (need > 0 && canAdd(u)) {
                    plan[u] = plan.getOrDefault(u, 0) + 1
                    need--
                    people++
                    cost += u.priceCents
                    load += u.prepLoad
                }
            }
            if (need > 0) throw BundleException("diet $diet unsatisfied")
        }

        // Step 2: Fill remaining capacity
        while (people < req.people) {
            val p = randomizedItems.firstOrNull { canAdd(it) } 
                ?: throw BundleException("capacity/budget lock")
            plan[p] = plan.getOrDefault(p, 0) + 1
            people++
            cost += p.priceCents
            load += p.prepLoad
        }

        return plan
    }

    /**
     * Calculates bonus points (0-20) for solution efficiency.
     * Makes greedy solutions comparable with MILP solutions.
     *
     * Scoring Breakdown:
     * 1. Budget Usage (0-6 points)
     *    - Using 90-100% of budget → 6 points
     *    - Using 80-90% of budget → 4 points
     *    - Using 70-80% of budget → 2 points
     *    - Using <70% of budget → 1 point
     * 
     * 2. Popular Items (0-5 points)
     *    - Linear scale: 0% → 0 points, 100% → 5 points
     *    - Example: 80% popular → 4 points
     * 
     * 3. Highly Rated Items (0-5 points)
     *    - Linear scale: 0% → 0 points, 100% → 5 points
     *    - Example: 60% highly rated → 3 points
     * 
     * 4. Kitchen Efficiency (0-4 points)
     *    - Linear scale: 0% → 0 points, 100% → 4 points
     *    - Example: 75% capacity → 3 points
     */
    private fun calculateEfficiencyBonus(
        totalCost: Int,
        popularItemsPercent: Double,
        highlyRatedItemsPercent: Double,
        kitchenLoadPercent: Double
    ): Int {
        val maxBudget = req.maxPricePerPersonCents * req.people
        val costScore = when {
            totalCost > maxBudget -> 0.0
            totalCost == 0 -> 0.0
            else -> (totalCost.toDouble() / maxBudget) * 6 // Up to 6 points
        }

        val popularityScore = (popularItemsPercent / 100.0) * 5 // Up to 5 points
        
        val ratingScore = (highlyRatedItemsPercent / 100.0) * 5 // Up to 5 points
        
        val kitchenScore = when {
            kitchenLoadPercent > 100 -> 0.0
            kitchenLoadPercent == 0.0 -> 0.0
            else -> (kitchenLoadPercent / 100.0) * 4 // Up to 4 points
        }

        return (costScore + popularityScore + ratingScore + kitchenScore).toInt()
    }
} 