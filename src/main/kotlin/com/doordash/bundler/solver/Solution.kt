package com.doordash.bundler.solver

import com.doordash.bundler.model.ItemUnit

/**
 * A Menu Bundle Solution!
 * 
 * This represents one way to combine menu items that satisfies all requirements.
 * Think of it as a complete order that:
 * - Has enough food for everyone
 * - Meets all dietary needs
 * - Stays within budget
 * - Won't overwhelm the kitchen
 * 
 * For each solution, we track:
 * 
 * @property items What items to order and how many of each
 * 
 * @property totalCost How much it costs in total (in cents)
 *                     Example: 5000 means $50.00
 * 
 * @property averageCostPerPerson Cost per person (in cents)
 *                                Example: 1000 means $10.00 per person
 * 
 * @property popularItemsPercent How many crowd favorites are included (0-100%)
 *                               Example: 75.5 means 75.5% of items are popular choices
 * 
 * @property kitchenLoadPercent How much of the kitchen's capacity this uses (0-100%)
 *                              Example: 60.0 means using 60% of available kitchen capacity
 * 
 * @property optimalityScore How good this solution is overall (0-100)
 *                          Higher is better! The score considers:
 *                          - Cost efficiency (40 points)
 *                            → Using budget well without going over
 *                          - Popular items (30 points)
 *                            → Including items people love
 *                          - Kitchen efficiency (30 points)
 *                            → Using kitchen capacity wisely
 * 
 * @property findingTimeMs How long it took to find this solution (in milliseconds)
 *                         Example: 150 means it took 0.15 seconds
 */
data class Solution(
    val items: Map<ItemUnit, Int>,
    val totalCost: Int,
    val averageCostPerPerson: Int,
    val popularItemsPercent: Double,
    val kitchenLoadPercent: Double,
    val optimalityScore: Int,
    val findingTimeMs: Long,
) : Comparable<Solution> {
    /**
     * Solutions are sorted by their quality score (highest first).
     * This means when you get multiple solutions:
     * - The best one (highest score) comes first
     * - The second-best comes next
     * - And so on...
     */
    override fun compareTo(other: Solution): Int =
        other.optimalityScore.compareTo(optimalityScore)
} 