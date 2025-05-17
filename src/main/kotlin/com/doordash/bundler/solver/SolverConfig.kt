package com.doordash.bundler.solver

/**
 * Settings for the Smart Bundle Finder
 * 
 * These settings control how the service looks for menu combinations:
 * 
 * @property minSolutionDiversityPercent How Different Should Solutions Be? (1-100)
 *           - Higher numbers mean more different combinations
 *           - Lower numbers allow more similar combinations
 *           - Default: 30 (30% different)
 *           
 *           Example with 30%:
 *           If one solution uses 10 items, the next solution must change
 *           at least 3 of those items (30% of 10 = 3)
 * 
 * @property enableDetailedLogging Want to See What's Happening?
 *           - When true: Shows detailed information about the search process
 *           - When false: Only shows final results
 *           - Default: false (quiet mode)
 * 
 *           The detailed logs help you understand:
 *           - What combinations were tried
 *           - Why some combinations were rejected
 *           - How good each solution is
 * 
 * @property maxTimePerSolutionMs How Long to Search? (in milliseconds)
 *           - How much time to spend looking for each solution
 *           - Default: 300 (0.3 seconds)
 *           
 *           If you're getting too few solutions:
 *           - Try increasing this number
 *           - Remember: total time = this number × number of solutions wanted
 *           
 *           Example:
 *           - maxTimePerSolutionMs = 300
 *           - Looking for 3 solutions
 *           - Total time allowed = 900ms (0.9 seconds)
 */
data class SolverConfig(
    val minSolutionDiversityPercent: Int = 30,
    val enableDetailedLogging: Boolean = false,
    val maxTimePerSolutionMs: Long = 300,
) {
    init {
        require(minSolutionDiversityPercent in 1..100) {
            "Solution diversity percentage must be between 1 and 100"
        }
        require(maxTimePerSolutionMs > 0) {
            "Maximum time per solution must be positive"
        }
    }
} 