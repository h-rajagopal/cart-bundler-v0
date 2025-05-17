package com.doordash.bundler.model

import com.doordash.bundler.rating.StoreItemRating

data class ItemUnit(
    val id: String,
    val name: String,
    val priceCents: Int,
    val diet: DietaryTag,
    val availableQty: Int,
    val prepLoad: Int,
    // Rating information
    val upvoteCount: Long,
    val downvoteCount: Long,
    val reviewCount: Long,
    val ratingPercentage: Float
) {
    companion object {
        private const val GOOD_RATING_THRESHOLD = 0.85f
        private const val POPULAR_VOTES_THRESHOLD = 100L
        private const val HIGHLY_RATED_VOTES_THRESHOLD = 50L

        fun fromStoreItemRating(
            id: String,
            name: String,
            priceCents: Int,
            diet: DietaryTag,
            availableQty: Int,
            prepLoad: Int,
            rating: StoreItemRating
        ): ItemUnit {
            val voteCount = rating.upvoteCount + rating.downvoteCount
            val ratingPercentage = when (voteCount == 0L) {
                true -> 0.0F
                false -> rating.upvoteCount.toFloat() / voteCount
            }

            return ItemUnit(
                id = id,
                name = name,
                priceCents = priceCents,
                diet = diet,
                availableQty = availableQty,
                prepLoad = prepLoad,
                upvoteCount = rating.upvoteCount,
                downvoteCount = rating.downvoteCount,
                reviewCount = rating.reviewCount,
                ratingPercentage = ratingPercentage
            )
        }

        fun getDietaryTag(tags: List<String>): DietaryTag {
            return when {
                tags.contains("Vegan") -> DietaryTag.VEGAN
                tags.contains("Vegetarian") -> DietaryTag.VEGETARIAN
                else -> DietaryTag.MEAT
            }
        }
    }

    // Helper functions
    fun hasGoodRating(): Boolean = ratingPercentage >= GOOD_RATING_THRESHOLD
    
    fun getTotalVotes(): Long = upvoteCount + downvoteCount
    
    fun isHighlyRated(): Boolean = hasGoodRating() && getTotalVotes() >= HIGHLY_RATED_VOTES_THRESHOLD

    /**
     * An item is considered popular if it has:
     * 1. A good rating (>= 85%)
     * 2. A significant number of votes (>= 100)
     */
    fun isPopular(): Boolean = hasGoodRating() && getTotalVotes() >= POPULAR_VOTES_THRESHOLD
} 