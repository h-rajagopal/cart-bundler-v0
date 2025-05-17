package com.doordash.bundler.solver

import com.doordash.bundler.model.BundleRequest
import com.doordash.bundler.model.DietaryTag
import com.doordash.bundler.model.ItemUnit
import com.doordash.bundler.rating.StoreItemRating
import com.google.ortools.Loader
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.slf4j.LoggerFactory

class OrToolsMilpSolverTest {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        @JvmStatic
        @BeforeAll
        fun loadOrTools() {
            Loader.loadNativeLibraries()
        }
    }

    @Test
    fun `test even distribution of items`() {
        // Create test items with different properties
        val items = listOf(
            createTestItem("1", "Pizza", DietaryTag.MEAT, 1000, 1, 900, 100),
            createTestItem("2", "Salad", DietaryTag.VEGETARIAN, 800, 1, 450, 50),
            createTestItem("3", "Pasta", DietaryTag.MEAT, 900, 1, 850, 150),
            createTestItem("4", "Sandwich", DietaryTag.MEAT, 700, 1, 400, 100),
            createTestItem("5", "Sushi", DietaryTag.MEAT, 1200, 1, 950, 50),
            createTestItem("6", "Soup", DietaryTag.VEGETARIAN, 600, 1, 380, 120)
        )

        logger.info("Test items:")
        items.forEach { item ->
            logger.info("${item.name}: ${item.diet}, $${item.priceCents/100.0}, rating: ${item.ratingPercentage}")
        }

        // Create a request for 20 people with some dietary requirements
        val request = BundleRequest(
            people = 20,
            maxPricePerPersonCents = 1500,
            requiredByDiet = mapOf(
                DietaryTag.MEAT to 15,
                DietaryTag.VEGETARIAN to 5
            ),
            topN = 3
        )

        logger.info("Request: ${request.people} people, budget: $${request.maxPricePerPersonCents/100.0}/person")
        logger.info("Dietary requirements: ${request.requiredByDiet}")

        // Initialize solver with test configuration
        val solver = OrToolsMilpSolver(
            items = items,
            req = request,
            kitchenCap = 30,
            config = SolverConfig(
                maxTimePerSolutionMs = 5000,
                minSolutionDiversityPercent = 30,
                enableDetailedLogging = true
            )
        )

        // Find solutions
        val solutions = solver.solve(k = 3)
        
        logger.info("Found ${solutions.size} solutions")
        
        assertFalse(solutions.isEmpty(), "Should find at least one solution")
        
        // Test the first solution for distribution constraints
        val firstSolution = solutions.first()
        
        logger.info("\nFirst solution details:")
        logger.info("Items used: ${firstSolution.items.size}")
        firstSolution.items.forEach { (item, qty) ->
            logger.info("${item.name}: $qty (${String.format("%.1f", qty * 100.0 / firstSolution.items.values.sum())}%)")
        }
        
        // 1. Test minimum number of different items
        assertTrue(
            firstSolution.items.size >= 4,
            "Solution should use at least 4 different items, found: ${firstSolution.items.size}"
        )

        // 2. Test maximum percentage for any single item
        val totalItems = firstSolution.items.values.sum()
        val maxItemPercentage = firstSolution.items.values.maxOrNull()!! * 100.0 / totalItems
        
        logger.info("\nDistribution metrics:")
        logger.info("Total items: $totalItems")
        logger.info("Max item percentage: ${String.format("%.1f", maxItemPercentage)}%")
        
        assertTrue(
            maxItemPercentage <= 25.0,
            "No item should be more than 25% of total, found: $maxItemPercentage%"
        )

        // 3. Test distribution range
        val quantities = firstSolution.items.values
        val maxQty = quantities.maxOrNull()!!.toDouble()
        val minQty = quantities.minOrNull()!!.toDouble()
        val qtyRange = (maxQty - minQty) / totalItems * 100.0

        logger.info("Distribution range: ${String.format("%.1f", qtyRange)}%")
        logger.info("Max quantity: $maxQty")
        logger.info("Min quantity: $minQty")

        assertTrue(
            qtyRange <= 15.0,
            "Difference between max and min quantities should be at most 15%, found: $qtyRange%"
        )

        // 4. Test dietary requirements are met
        val regularItems = firstSolution.items.entries
            .filter { it.key.diet == DietaryTag.MEAT }
            .sumOf { it.value }
        val vegetarianItems = firstSolution.items.entries
            .filter { it.key.diet == DietaryTag.VEGETARIAN }
            .sumOf { it.value }

        logger.info("\nDietary requirements:")
        logger.info("Meat items: $regularItems (required: 15)")
        logger.info("Vegetarian items: $vegetarianItems (required: 5)")

        assertTrue(
            regularItems >= 15,
            "Should have at least 15 regular items, found: $regularItems"
        )
        assertTrue(
            vegetarianItems >= 5,
            "Should have at least 5 vegetarian items, found: $vegetarianItems"
        )

        // 5. Test budget constraint
        val totalCost = firstSolution.items.entries.sumOf { it.key.priceCents * it.value }
        logger.info("\nBudget:")
        logger.info("Total cost: $${totalCost/100.0}")
        logger.info("Budget limit: $${request.maxPricePerPersonCents * request.people/100.0}")

        assertTrue(
            totalCost <= request.maxPricePerPersonCents * request.people,
            "Total cost should not exceed budget"
        )

        logger.info("\nSolution metrics:")
        logger.info("Optimality score: ${firstSolution.optimalityScore}")
        logger.info("Popular items: ${firstSolution.popularItemsPercent}%")
        logger.info("Kitchen load: ${firstSolution.kitchenLoadPercent}%")
    }

    private fun createTestItem(
        id: String,
        name: String,
        diet: DietaryTag,
        priceCents: Int,
        prepLoad: Int,
        upvotes: Long,
        downvotes: Long
    ): ItemUnit {
        val rating = StoreItemRating(
            storeId = 1,
            itemId = id,
            upvoteCount = upvotes,
            downvoteCount = downvotes,
            reviewCount = upvotes + downvotes
        )
        return ItemUnit.fromStoreItemRating(
            id = id,
            name = name,
            priceCents = priceCents,
            diet = diet,
            availableQty = 30,
            prepLoad = prepLoad,
            rating = rating
        )
    }
} 