package com.doordash.bundler.model

import com.doordash.bundler.rating.StoreItemRating
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ItemUnitTest {
    @Test
    fun `test rating calculations`() {
        // Test case 1: High rating, many votes
        val rating1 = StoreItemRating(
            storeId = 1,
            itemId = "item1",
            upvoteCount = 900,
            downvoteCount = 100,
            reviewCount = 500
        )
        val item1 = ItemUnit.fromStoreItemRating(
            id = "item1",
            name = "Test Item 1",
            priceCents = 1000,
            diet = DietaryTag.MEAT,
            availableQty = 10,
            prepLoad = 10,
            rating = rating1
        )
        assertTrue(item1.hasGoodRating(), "Item with 90% rating should have good rating")
        assertTrue(item1.isPopular(), "Item with 90% rating and 1000 votes should be popular")
        assertTrue(item1.isHighlyRated(), "Item with 90% rating and 1000 votes should be highly rated")
        assertEquals(0.9f, item1.ratingPercentage, "Rating percentage should be 0.9")

        // Test case 2: High rating, few votes
        val rating2 = StoreItemRating(
            storeId = 1,
            itemId = "item2",
            upvoteCount = 45,
            downvoteCount = 5,
            reviewCount = 30
        )
        val item2 = ItemUnit.fromStoreItemRating(
            id = "item2",
            name = "Test Item 2",
            priceCents = 1000,
            diet = DietaryTag.MEAT,
            availableQty = 10,
            prepLoad = 10,
            rating = rating2
        )
        assertTrue(item2.hasGoodRating(), "Item with 90% rating should have good rating")
        assertFalse(item2.isPopular(), "Item with few votes should not be popular despite good rating")
        assertTrue(item2.isHighlyRated(), "Item with 90% rating and >50 votes should be highly rated")

        // Test case 3: Low rating
        val rating3 = StoreItemRating(
            storeId = 1,
            itemId = "item3",
            upvoteCount = 600,
            downvoteCount = 400,
            reviewCount = 500
        )
        val item3 = ItemUnit.fromStoreItemRating(
            id = "item3",
            name = "Test Item 3",
            priceCents = 1000,
            diet = DietaryTag.MEAT,
            availableQty = 10,
            prepLoad = 10,
            rating = rating3
        )
        assertFalse(item3.hasGoodRating(), "Item with 60% rating should not have good rating")
        assertFalse(item3.isPopular(), "Item with low rating should not be popular despite many votes")
        assertFalse(item3.isHighlyRated(), "Item with low rating should not be highly rated")

        // Test case 4: No votes
        val rating4 = StoreItemRating(
            storeId = 1,
            itemId = "item4",
            upvoteCount = 0,
            downvoteCount = 0,
            reviewCount = 0
        )
        val item4 = ItemUnit.fromStoreItemRating(
            id = "item4",
            name = "Test Item 4",
            priceCents = 1000,
            diet = DietaryTag.MEAT,
            availableQty = 10,
            prepLoad = 10,
            rating = rating4
        )
        assertFalse(item4.hasGoodRating(), "Item with no votes should not have good rating")
        assertFalse(item4.isPopular(), "Item with no votes should not be popular")
        assertFalse(item4.isHighlyRated(), "Item with no votes should not be highly rated")
        assertEquals(0.0f, item4.ratingPercentage, "Rating percentage should be 0 for no votes")
    }
} 