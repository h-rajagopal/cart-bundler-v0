package com.doordash.bundler.model

/**
 * Types of solvers available for menu bundling.
 * Each solver has different characteristics:
 * 
 * MILP (Mixed Integer Linear Programming):
 * - Finds globally optimal solutions
 * - Takes more time (0.2-2 seconds)
 * - Best for important orders
 * 
 * GREEDY:
 * - Finds good solutions quickly
 * - Very fast (0.01-0.05 seconds)
 * - Best for quick orders
 * 
 * BRUTE_FORCE:
 * - Checks every possible combination
 * - Slowest but guaranteed optimal
 * - Best for small menus or testing
 */
enum class SolverType {
    MILP,
    GREEDY,
    BRUTE_FORCE
} 