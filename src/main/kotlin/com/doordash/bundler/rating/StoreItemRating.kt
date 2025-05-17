package com.doordash.bundler.rating

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class StoreItemRating(
    val storeId: Int,
    val itemId: String,
    val upvoteCount: Long,
    val downvoteCount: Long,
    val reviewCount: Long
) {
    fun toItemRating(): ItemRating {
        val voteCount = upvoteCount + downvoteCount
        val ratingPercentage = when (voteCount == 0L) {
            true -> 0.0F
            false -> upvoteCount.toFloat() / voteCount
        }

        return ItemRating(
            itemId = this.itemId,
            upvoteCount = this.upvoteCount,
            downvoteCount = this.downvoteCount,
            reviewCount = this.reviewCount,
            voteCount = voteCount,
            ratingPercentage = ratingPercentage
        )
    }
}

data class ItemRating(
    val itemId: String,
    val upvoteCount: Long,
    val downvoteCount: Long,
    val reviewCount: Long,
    val voteCount: Long,
    val ratingPercentage: Float
)

data class MostLikedItem(
    val storeId: Int,
    val itemId: String,
    val rank: Long
) 