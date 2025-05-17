package com.doordash.bundler.dto

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
 * Complete response showing different ways to bundle menu items.
 * Includes solutions from both approaches and smart recommendations.
 * 
 * The Two Approaches:
 * 
 * 1. Advanced Optimizer (MILP Solutions):
 *    ✓ Finds the mathematically best combinations
 *    ✓ Considers all possible item combinations
 *    ✓ Usually produces better solutions
 *    → But takes longer (typically 0.2-2 seconds)
 * 
 * 2. Quick Optimizer (Greedy Solutions):
 *    ✓ Uses practical "common sense" rules
 *    ✓ Very fast (typically 0.01-0.05 seconds)
 *    ✓ Good for testing or simple menus
 *    → But might miss some clever combinations
 * 
 * Recommendation System:
 * 
 * The service suggests which solutions to use based on:
 * - Quality difference between approaches
 * - Time difference between approaches
 * - Success rate of each approach
 * 
 * Possible Recommendations:
 * - "MILP": Use advanced solutions (significantly better)
 * - "GREEDY": Use quick solutions (almost as good, much faster)
 * - "BOTH": Consider both (trade-off between quality and speed)
 * - "NONE": No valid solutions found
 * 
 * Example Response:
 * ```json
 * {
 *   "milpSolutions": [
 *     { "items": [...], "metrics": {...} }
 *   ],
 *   "greedySolutions": [
 *     { "items": [...], "metrics": {...} }
 *   ],
 *   "recommendedApproach": "MILP",
 *   "recommendation": "Use the advanced optimizer solutions. 
 *                     They're significantly better (15 points) 
 *                     and only took 5x longer."
 * }
 * ```
 */
data class BundleResponse(
    val milpSolutions: List<BundleSolution>,
    val greedySolutions: List<BundleSolution>,
    val recommendedApproach: String,
    val recommendation: String
) 