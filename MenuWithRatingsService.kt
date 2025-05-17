import com.doordash.menu.v1.GetMenuResponse
import org.springframework.stereotype.Service

@Service
class MenuWithRatingsService(
    private val menuService: MenuService,
    private val ratingService: RatingService
) {
    /**
     * Fetches menu items with their ratings for a given store
     * @param storeId The ID of the store to fetch menu items for
     * @return List of menu items with their ratings
     */
    suspend fun getMenuItemsWithRatings(storeId: Int): List<MenuItemWithRating> {
        // Fetch menu and ratings in parallel
        val menu = menuService.getMenu(storeId)
        val ratings = ratingService.getRatings(storeId)
            .associateBy { rating -> rating.itemId }

        // Combine menu items with ratings
        return MenuItemWithRatingFactory.createFromProtoWithRatings(
            menu = menu.storeMenu,
            ratings = ratings
        )
    }

    /**
     * Gets menu items sorted by rating (highest first)
     */
    suspend fun getTopRatedItems(storeId: Int): List<MenuItemWithRating> {
        return getMenuItemsWithRatings(storeId)
            .filter { item -> item.voteCount >= MIN_VOTES_THRESHOLD }
            .sortedByDescending { item -> item.ratingPercentage }
    }

    /**
     * Gets menu items grouped by category
     */
    suspend fun getMenuItemsByCategory(storeId: Int): Map<String, List<MenuItemWithRating>> {
        return getMenuItemsWithRatings(storeId)
            .groupBy { item -> item.categoryName }
    }

    /**
     * Gets popular menu items (items marked as popular with good ratings)
     */
    suspend fun getPopularItems(storeId: Int): List<MenuItemWithRating> {
        return getMenuItemsWithRatings(storeId)
            .filter { item -> item.isPopular && item.hasGoodRating() }
            .sortedByDescending { item -> item.ratingPercentage }
    }

    companion object {
        private const val MIN_VOTES_THRESHOLD = 50
    }
} 