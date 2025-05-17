data class MenuItemWithRating(
    // Basic item information
    val itemId: String,
    val name: String,
    val description: String?,
    val price: Int,  // in cents
    val imageUrl: String?,
    
    // Rating information
    val upvoteCount: Long,
    val downvoteCount: Long,
    val reviewCount: Long,
    val voteCount: Long,
    val ratingPercentage: Float,
    
    // Additional item metadata
    val isPopular: Boolean,
    val dietaryTags: List<String>,
    val categoryId: String,
    val categoryName: String
) {
    companion object {
        // Example McDonald's menu items with ratings
        fun getMcDonaldsMenuExample() = listOf(
            MenuItemWithRating(
                itemId = "big_mac",
                name = "Big Mac®",
                description = "Two 100% beef patties, special sauce, lettuce, cheese, pickles, onions on a sesame seed bun",
                price = 699,  // $6.99
                imageUrl = "https://img.cdn4dd.com/cdn-cgi/image/fit=contain,width=1200,height=672,format=auto/https://doordash-static.s3.amazonaws.com/media/photos/6af42a53-ea44-40b1-acd4-daa8ba2d3431-retina-large.jpg",
                upvoteCount = 1250,
                downvoteCount = 150,
                reviewCount = 800,
                voteCount = 1400,
                ratingPercentage = 0.89f,
                isPopular = true,
                dietaryTags = listOf(),
                categoryId = "burgers",
                categoryName = "Burgers"
            ),
            
            MenuItemWithRating(
                itemId = "mcnuggets_10pc",
                name = "10 Piece McNuggets®",
                description = "Our tender, juicy Chicken McNuggets® are made with 100% white meat chicken and no artificial colors, flavors or preservatives",
                price = 599,  // $5.99
                imageUrl = "https://img.cdn4dd.com/cdn-cgi/image/fit=contain,width=1200,height=672,format=auto/https://doordash-static.s3.amazonaws.com/media/photos/f5f5ea6c-d7e9-43d5-9e90-497e73d67c47-retina-large.jpg",
                upvoteCount = 980,
                downvoteCount = 120,
                reviewCount = 650,
                voteCount = 1100,
                ratingPercentage = 0.89f,
                isPopular = true,
                dietaryTags = listOf(),
                categoryId = "chicken_sandwiches",
                categoryName = "Chicken & Sandwiches"
            ),
            
            MenuItemWithRating(
                itemId = "large_fries",
                name = "Large French Fries",
                description = "Our World Famous Fries® are made with premium potatoes such as the Russet Burbank and the Shepody",
                price = 399,  // $3.99
                imageUrl = "https://img.cdn4dd.com/cdn-cgi/image/fit=contain,width=1200,height=672,format=auto/https://doordash-static.s3.amazonaws.com/media/photos/f6de6096-38a9-46b7-9c9c-d2f45284a576-retina-large.jpg",
                upvoteCount = 1500,
                downvoteCount = 100,
                reviewCount = 900,
                voteCount = 1600,
                ratingPercentage = 0.94f,
                isPopular = true,
                dietaryTags = listOf("Vegetarian"),
                categoryId = "snacks_sides",
                categoryName = "Snacks & Sides"
            ),
            
            MenuItemWithRating(
                itemId = "oreo_mcflurry",
                name = "OREO® McFlurry®",
                description = "The McDonald's McFlurry® with OREO® cookies is an popular combination of vanilla soft serve with OREO® cookie pieces mixed throughout",
                price = 449,  // $4.49
                imageUrl = "https://img.cdn4dd.com/cdn-cgi/image/fit=contain,width=1200,height=672,format=auto/https://doordash-static.s3.amazonaws.com/media/photos/e5a8b02d-2b37-4031-aa23-fd0a6c684a24-retina-large.jpg",
                upvoteCount = 890,
                downvoteCount = 80,
                reviewCount = 500,
                voteCount = 970,
                ratingPercentage = 0.92f,
                isPopular = true,
                dietaryTags = listOf("Vegetarian"),
                categoryId = "desserts",
                categoryName = "Desserts & Shakes"
            ),
            
            MenuItemWithRating(
                itemId = "quarter_pounder",
                name = "Quarter Pounder®* with Cheese",
                description = "Fresh beef quarter pound* patty, two slices of melty American cheese, slivered onions and tangy pickles on a sesame seed bun",
                price = 649,  // $6.49
                imageUrl = "https://img.cdn4dd.com/cdn-cgi/image/fit=contain,width=1200,height=672,format=auto/https://doordash-static.s3.amazonaws.com/media/photos/f6de6096-38a9-46b7-9c9c-d2f45284a576-retina-large.jpg",
                upvoteCount = 1100,
                downvoteCount = 150,
                reviewCount = 700,
                voteCount = 1250,
                ratingPercentage = 0.88f,
                isPopular = true,
                dietaryTags = listOf(),
                categoryId = "burgers",
                categoryName = "Burgers"
            )
        )
    }

    // Helper functions
    fun getFormattedPrice(): String = "$${price / 100.0}"
    
    fun getFormattedRating(): String = "${(ratingPercentage * 100).toInt()}%"
    
    fun getTotalVotes(): Long = upvoteCount + downvoteCount
    
    fun hasGoodRating(): Boolean = ratingPercentage >= 0.85f
} 