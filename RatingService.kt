import com.doordash.menu.rating.ItemRating

interface RatingService {
    /**
     * Gets ratings for all items in a store
     * @param storeId The ID of the store to fetch ratings for
     * @return List of item ratings
     */
    suspend fun getRatings(storeId: Int): List<ItemRating>
    
    /**
     * Gets rating for a specific item
     * @param storeId The ID of the store
     * @param itemId The ID of the item
     * @return The item's rating or null if not found
     */
    suspend fun getRating(storeId: Int, itemId: String): ItemRating?
} 