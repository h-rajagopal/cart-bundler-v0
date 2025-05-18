package com.doordash.bundler.dto

import com.doordash.bundler.model.SolverType

/**
 * One item in a bundle solution, showing what to order and how many.
 * 
 * Example:
 * ```json
 * {
 *   "id": "pizza#1",
 *   "name": "Margherita Pizza (slice 1/4)",
 *   "qty": 3,
 *   "priceCents": 500
 * }
 * ```
 * This means:
 * - Order 3 servings of the first slice of a Margherita Pizza
 * - Each serving costs $5.00 (500 cents)
 * - Total cost for this line: $15.00
 */
data class PlanLine(
    val id: String,
    val name: String,
    val qty: Int,
    val priceCents: Int
)

/**
 * Quality measurements for a bundle solution.
 * These help you understand how good each solution is.
 *
 * Scoring System:
 * - Advanced (MILP) Solutions: 0-100 points
 *   → 100 = Perfect solution (optimal cost, popular items, efficient kitchen use)
 *   → 80+ = Excellent solution
 *   → 60+ = Good solution
 *   → <60 = May need improvement
 *
 * - Quick (Greedy) Solutions: 60-80 points
 *   → Base score: 60 points (for finding any valid solution)
 *   → Bonus points: Up to 20 extra points for efficiency
 *   → 75+ = Very good for a quick solution
 *   → 65+ = Decent quick solution
 *   → <65 = Basic valid solution
 *
 * Example:
 * ```json
 * {
 *   "optimalityScore": 85,
 *   "totalCost": 25000,      // $250.00
 *   "averageCostPerPerson": 1250,  // $12.50 per person
 *   "popularItemsPercent": 75.5,   // 75.5% are crowd favorites
 *   "kitchenLoadPercent": 60.0,    // Using 60% of kitchen capacity
 *   "findingTimeMs": 150     // Found in 0.15 seconds
 * }
 * ```
 */
data class SolutionMetrics(
    val optimalityScore: Int,
    val totalCost: Int,
    val averageCostPerPerson: Int,
    val popularItemsPercent: Double,
    val kitchenLoadPercent: Double,
    val findingTimeMs: Long,
)

/**
 * A complete bundle solution with its items and quality measurements.
 * 
 * Example:
 * ```json
 * {
 *   "items": [
 *     {"id": "pizza#1", "name": "Margherita (1/4)", "qty": 3, "priceCents": 500},
 *     {"id": "salad#1", "name": "Caesar Salad", "qty": 2, "priceCents": 800}
 *   ],
 *   "metrics": {
 *     "optimalityScore": 85,
 *     "totalCost": 3100,
 *     "averageCostPerPerson": 1550,
 *     "popularItemsPercent": 75.5,
 *     "kitchenLoadPercent": 60.0,
 *     "findingTimeMs": 150
 *   }
 * }
 * ```
 * This solution:
 * - Orders 3 pizza slices and 2 salads
 * - Total cost: $31.00
 * - Average per person: $15.50
 * - 75.5% popular items
 * - Uses 60% of kitchen capacity
 * - Found in 0.15 seconds
 */
data class BundleSolution(
    val items: List<PlanLine>,
    val metrics: SolutionMetrics
)

/**
 * Complete response showing solutions found by the selected solver.
 * 
 * The Three Solvers:
 * 
 * 1. Advanced Optimizer (MILP):
 *    ✓ Finds the mathematically best combinations
 *    ✓ Considers all possible item combinations
 *    ✓ Usually produces better solutions
 *    → But takes longer (typically 0.2-2 seconds)
 * 
 * 2. Quick Optimizer (Greedy):
 *    ✓ Uses practical "common sense" rules
 *    ✓ Very fast (typically 0.01-0.05 seconds)
 *    ✓ Good for testing or simple menus
 *    → But might miss some clever combinations
 * 
 * 3. Thorough Optimizer (Brute Force):
 *    ✓ Checks every possible combination
 *    ✓ Guaranteed to find the best solution
 *    ✓ Perfect for small menus
 *    → But very slow for large menus
 * 
 * Example Response:
 * ```json
 * {
 *   "solutions": [
 *     { "items": [...], "metrics": {...} }
 *   ],
 *   "solverType": "BRUTE_FORCE",
 *   "findingTimeMs": 1500,
 *   "message": "Found 3 solutions using BRUTE_FORCE solver"
 * }
 * ```
 */
data class BundleResponse(
    val solutions: List<BundleSolution>,
    val solverType: SolverType,
    val findingTimeMs: Long,
    val message: String
) 