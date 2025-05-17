import com.doordash.menu.v1.GetMenuResponse.StoreMenu
import com.doordash.menu.v1.GetMenuResponse.StoreMenu.MenuStructure.Menu.Item
import com.doordash.menu.v1.GetMenuResponse.StoreMenu.MenuStructure.Menu.Category
import com.doordash.menu.v1.GetMenuResponse.StoreMenu.MenuStructure.Menu.CategoryEntity

class MenuItemWithRatingFactory {
    companion object {
        fun createFromProto(
            menuItem: Item,
            rating: ItemRating?
        ): MenuItemWithRating {
            return MenuItemWithRating(
                itemId = menuItem.id,
                name = menuItem.displayInfo.title.defaultOrResolvedValue,
                description = menuItem.displayInfo.description?.defaultOrResolvedValue,
                price = menuItem.paymentInfo.price.defaultOrResolvedValue.cents.toInt(),
                imageUrl = menuItem.displayInfo.image?.url,
                upvoteCount = rating?.upvoteCount ?: 0,
                downvoteCount = rating?.downvoteCount ?: 0,
                reviewCount = rating?.reviewCount ?: 0,
                voteCount = rating?.voteCount ?: 0,
                ratingPercentage = rating?.ratingPercentage ?: 0f,
                isPopular = menuItem.displayInfo.badges.any { badge -> badge.type == "POPULAR" },
                dietaryTags = menuItem.dishInfo.classificationInfo.dietaryTags.map { tag -> tag.name },
                categoryId = menuItem.categoryId,
                categoryName = menuItem.categoryName
            )
        }

        fun createFromProtoWithRatings(
            menu: StoreMenu,
            ratings: Map<String, ItemRating>
        ): List<MenuItemWithRating> {
            return menu.structure.menus.flatMap { menuSection: Menu ->
                menuSection.categories.flatMap { category: CategoryEntity ->
                    category.items.map { item: Item ->
                        createFromProto(
                            menuItem = item,
                            rating = ratings[item.id]
                        )
                    }
                }
            }
        }
    }
} 