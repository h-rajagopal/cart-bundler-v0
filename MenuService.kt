import com.doordash.menu.v1.GetMenuResponse

interface MenuService {
    /**
     * Gets the menu for a store
     * @param storeId The ID of the store to fetch the menu for
     * @return The store's menu
     */
    suspend fun getMenu(storeId: Int): GetMenuResponse
} 