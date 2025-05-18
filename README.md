# Smart Menu Bundle Creator 🍽️

> **Authors:** Hari Ohm Prasath Rajagopal  
> **Date:** April 17, 2024  
> **Status:** Updated

---

## Table of Contents
1. [Overview](#1-overview)
2. [Understanding MILP for Beginners](#2-understanding-milp-for-beginners)
3. [System Components](#3-system-components)
4. [How It Works - From Order to Solution](#4-how-it-works)
5. [Technical Deep Dive](#5-technical-details)
6. [Real-World Examples with Explanations](#6-real-world-examples)
7. [When to Use What - A Performance Guide](#7-performance-guide)
8. [Future Improvements](#8-future-roadmap)

---

## 1. Overview

The Smart Menu Bundle Creator is like having a super-smart restaurant manager who can instantly plan perfect group orders. Imagine trying to plan a meal for 50 people with different dietary needs, budget constraints, and preferences - that's what our system does automatically!

### What Problems Does It Solve?

Real-World Problem | Technical Term | How We Solve It | Example
------------------|----------------|------------------|--------
"I need to check thousands of menu combinations" | Combinatorial Optimization | Mixed Integer Linear Programming (MILP) | Instead of manually checking 1000s of combinations, our system finds the best one in seconds
"Some people are vegetarian, others want meat" | Constraint Satisfaction | Multi-constraint Programming | We tell the system "need 5 vegetarian meals" and it ensures this requirement is met
"We have a strict budget of $500" | Linear Constraints | Linear Optimization | The system adds up all item costs and ensures total ≤ $500
"Our kitchen can only make 50 dishes at once" | Resource Constraints | Capacity Planning | We set a maximum limit that matches kitchen capacity
"People love our pizzas, include them if possible" | Preference Optimization | Weighted Objective Functions | We give popular items a bonus score in our calculations

---

## 2. Understanding MILP for Beginners

Let's break down Mixed Integer Linear Programming using real-world examples that anyone can understand:

### What is MILP? Think of Party Planning!

When You Say... | MILP Term | What It Means | Real Example
----------------|-----------|---------------|-------------
"I need 3 pizzas" | Decision Variable (Integer) | A number we need to figure out | x₁ = 3 (we'll order 3 pizzas)
"Should we get salad?" | Binary Variable (Yes/No) | A yes/no decision | y₁ = 1 (yes, get salad) or 0 (no salad)
"Total cost must be under $500" | Linear Constraint | A rule using simple math | If pizza=$20: 3 pizzas = $60 ≤ $500
"Get the best value for money" | Objective Function | What we're trying to achieve | Find lowest cost while getting good food

### Types of Numbers We Work With (Variables)

In Real Life | Technical Term | What It Means | Example
-------------|----------------|---------------|--------
"How many pizzas?" | Integer Variable | Whole numbers only | x₁ = 3 pizzas (can't order 2.5 pizzas!)
"Include salad?" | Binary Variable | Yes (1) or No (0) only | y₁ = 1 (yes, include salad)
"Total portions" | Linear Expression | Simple math with our numbers | 3 pizzas × 8 slices = 24 portions
"Pizza + Salad combo?" | Auxiliary Variable | Helper number for combinations | z₁₂ = 1 (yes, we want both)

### Types of Rules (Constraints)

Common Requirement | Technical Term | Math Behind It | Real Example
------------------|----------------|----------------|-------------
"Must feed 20 people" | Capacity Constraint | Sum of all portions ≥ 20 | 3 pizzas × 8 slices = 24 ≥ 20
"Need 5 vegetarian meals" | Categorical Constraint | Sum of veg items ≥ 5 | 2 veg pizzas × 8 = 16 ≥ 5
"Budget is $300" | Budget Constraint | Sum of (items × prices) ≤ 300 | (3 × $20) + (2 × $10) = $80 ≤ $300
"Kitchen limit: 50 items" | Resource Constraint | Sum of all items ≤ 50 | 3 pizzas + 2 salads = 5 ≤ 50
"Not too much pizza" | Distribution Constraint | Pizza ≤ 25% of total | 24 slices ≤ 0.25 × 100 total
"At least 4 different items" | Diversity Constraint | Count of selected items ≥ 4 | Selected: pizza, salad, pasta, drinks = 4

---

## 3. System Components

The system consists of four primary components, each handling specific aspects of the optimization process:

### REST API Layer
```kotlin
Core Functions:
- Request handling and validation
- Input parameter normalization
- Response transformation
- Error handling and validation

Technical Implementation:
- HTTP endpoints for bundle creation
- Request/Response DTOs
- Input validation rules
- Error response mapping
```

### Bundle Service
```kotlin
Core Functions:
- Optimization orchestration
- Solution evaluation
- Strategy determination
- Result aggregation

Technical Implementation:
- Algorithm selection logic
- Solution comparison metrics
- Multi-solution generation
- Performance monitoring
```

### MILP Solver
```kotlin
Core Functions:
- Global optimization
- Constraint satisfaction
- Solution optimality
- Multi-objective optimization

Technical Implementation:
- Variable generation
- Constraint modeling
- Objective function optimization
- Solution validation
```

### Greedy Solver
```kotlin
Core Functions:
- Fast approximation
- Iterative optimization
- Local decision making
- Rapid solution generation

Technical Implementation:
- Priority-based sorting
- Incremental selection
- Local optimization
- Solution verification
```

### Brute Force Solver
```kotlin
Core Functions:
- Complete enumeration
- Validation and filtering
- Solution scoring

Technical Implementation:
- Combination generation
- Constraint checking
- Quality calculation
```

---

## 4. How It Works - From Order to Solution

Let's walk through a real order step by step:

### Step 1: Taking the Order (Like a Restaurant Host)
```kotlin
// What the Customer Says:
// "I need lunch for 20 people"
// "Budget is $15 per person"
// "15 regular meals, 5 vegetarian"

// How We Record It (Technical Version):
val request = BundleRequest(
    people = 20,               // Total group size
    maxPricePerPersonCents = 1500,  // $15.00 in cents
    requiredByDiet = mapOf(
        DietaryTag.MEAT to 15,      // Regular meals
        DietaryTag.VEGETARIAN to 5   // Vegetarian meals
    )
)

// How We Process It (Behind the Scenes):
val constraints = listOf(
    TotalServingConstraint(20),     // Must feed everyone
    DietaryConstraint(MEAT, 15),    // Regular meal count
    DietaryConstraint(VEG, 5),      // Vegetarian meal count
    BudgetConstraint(1500 * 20)     // Total budget in cents
)
```

### Step 2: Planning the Menu (Like a Chef Planning)
```kotlin
// Available Menu Items:
val menuItems = listOf(
    MenuItem("Pizza", serves = 4, price = 2000),  // $20 pizza feeds 4
    MenuItem("Salad", serves = 1, price = 800)    // $8 individual salad
)

// Behind the Scenes Math:
// For each menu item, we create a variable 'x' that represents
// how many of that item we should order
val variables = items.map { item ->
    model.newIntVar(0, item.maxQuantity, "x${item.id}")
}
```

### Step 3: Finding the Perfect Combination
```kotlin
// What We Give Back to Customer:
val solution = Solution(
    items = mapOf(
        pizza to 4,  // Order 4 pizzas (feeds 16)
        salad to 4   // Order 4 salads (feeds 4)
    ),
    metrics = SolutionMetrics(
        totalCost = 9200,     // $92.00 total
        averageCost = 460     // $4.60 per person
    )
)

// How We Found This Solution:
// 1. Try different combinations
// 2. Keep track of best one found
// 3. Return when we find optimal solution
model.minimize(costObjective)
solver.solve(model)
```

---

## 5. Technical Deep Dive

Our system implements two distinct algorithmic approaches in [`OrToolsMilpSolver.kt`][milp-solver] and [`GreedySolver.kt`][greedy-solver]. Let's explore how each one works in detail.

### 5.1 The Master Chef (MILP Solver) - Perfect but Patient

The MILP solver is implemented in [`OrToolsMilpSolver.kt`][milp-solver]. Here's how it maps to the actual implementation:

#### Decision Variables

**DV-1: Primary Quantity Variables**
- What: How many units of each item to order
- Code: [`OrToolsMilpSolver.kt:95-98`][milp-quantity-vars]
- Implementation: `val itemVars = items.mapIndexed { idx, it -> model.newIntVar(0, it.availableQty.toLong(), "x$idx") }`

**DV-2: Selection Variables**
- What: Whether to include each item (0 or 1)
- Code: [`OrToolsMilpSolver.kt:182-185`][milp-selection-vars]
- Implementation: `val selectedItems = itemVars.mapIndexed { idx, itemVar -> model.newBoolVar("selected_$idx") }`

**DV-3: Combination Variables**
- What: Whether two items are used together
- Code: [`OrToolsMilpSolver.kt:156-159`][milp-combination-vars]
- Implementation: `val bothSelected = model.newBoolVar("both_selected_${i}_$j")`

#### Constraints

**C-1: Total Servings**
- Rule: Must have enough food for everyone
- Code: [`OrToolsMilpSolver.kt:106-108`][milp-total-servings]
- Implementation: `model.addGreaterOrEqual(totalServings, req.people.toLong())`
- Mathematical Form: ∑x[i] ≥ people

**C-2: Dietary Requirements**
- Rule: Must meet each dietary need exactly
- Code: [`OrToolsMilpSolver.kt:112-119`][milp-dietary-reqs]
- Implementation: 
```kotlin
req.requiredByDiet.forEach { (diet, need) ->
    val dietVars = items.withIndex()
        .filter { it.value.diet == diet }
        .map { itemVars[it.index] }
    model.addGreaterOrEqual(LinearExpr.sum(dietVars), need.toLong())
}
```

**C-3: Budget Limit**
- Rule: Total cost must not exceed budget
- Code: [`OrToolsMilpSolver.kt:124-128`][milp-budget-limit]
- Mathematical Form: ∑(price[i] × x[i]) ≤ budget_per_person × people

**C-4: Kitchen Capacity**
- Rule: Don't overwhelm the kitchen
- Code: [`OrToolsMilpSolver.kt:133-137`][milp-kitchen-capacity]
- Mathematical Form: ∑(load[i] × x[i]) ≤ kitchen_capacity

**C-5: Fair Distribution**
- Rule: Keep portions balanced between items
- Code: [`OrToolsMilpSolver.kt:142-171`][milp-fair-distribution]
- Mathematical Form: |x[i] - x[j]| ≤ range × people

**C-6: Minimum Variety**
- Rule: Must use at least 4 different items
- Code: [`OrToolsMilpSolver.kt:189-193`][milp-min-variety]
- Mathematical Form: ∑y[i] ≥ MIN_DIFFERENT_ITEMS

#### Optimization Goals

**OG-1: Cost Minimization**
- Primary Goal: Minimize total cost
- Code: [`OrToolsMilpSolver.kt:285-297`][milp-cost-min]
- Mathematical Form: Minimize: ∑(price[i] × BIG × x[i])

**OG-2: Quality Bonuses**
- Secondary Goals: Include popular and highly-rated items
- Code: [`OrToolsMilpSolver.kt:288-293`][milp-quality-bonus]
- Implementation:
```kotlin
val popularityBonus = if (item.isPopular()) 1 else 0
val ratingBonus = when {
    item.isHighlyRated() -> 2
    item.hasGoodRating() -> 1
    else -> 0
}
```

#### Solution Scoring

**S-1: Base Quality Metrics**
- What: Calculate basic solution metrics
- Code: [`OrToolsMilpSolver.kt:299-308`][milp-base-metrics]
- Components:
  * Total Cost
  * Average Cost Per Person
  * Popular Items Percentage
  * Kitchen Load Percentage

**S-2: Optimality Score**
- What: Calculate final solution score (0-100)
- Code: [`OrToolsMilpSolver.kt:346-397`][milp-optimality-score]
- Components:
  * Cost Efficiency (25 points)
  * Popular Items (20 points)
  * Highly Rated Items (20 points)
  * Kitchen Efficiency (15 points)
  * Distribution (10 points)
  * Diversity (10 points)

### 5.2 The Quick Chef (Greedy Solver) - Fast and Practical

The Greedy solver is implemented in [`GreedySolver.kt`][greedy-solver]. Here's how it maps to the code:

#### Priority Planning

**PP-1: Item Sorting**
- What: Sort items by importance
- Code: [`GreedySolver.kt:102-116`][greedy-sorting]
- Implementation:
```kotlin
val randomizedItems = items.map { it to random.nextDouble() }
    .sortedBy { (item, rand) -> 
        val ratingScore = if (item.isHighlyRated()) "0" else "1"
        val popularityScore = if (item.isPopular()) "0" else "1"
        item.diet.toString() + ratingScore + popularityScore + ...
    }
```

#### Solution Building

**SB-1: Dietary Requirements**
- What: Handle dietary needs first
- Code: [`GreedySolver.kt:196-214`][greedy-dietary]
- Implementation: Process VEGAN → VEGETARIAN → MEAT in order

**SB-2: Capacity Filling**
- What: Fill remaining portions
- Code: [`GreedySolver.kt:216-224`][greedy-capacity]
- Implementation: Add best available items until requirements met

#### Quality Scoring

**QS-1: Base Score**
- What: Start with 60 points base score
- Code: [`GreedySolver.kt:126-141`][greedy-base-score]
- Implementation: Calculate basic metrics

**QS-2: Efficiency Bonus**
- What: Add up to 40 bonus points
- Code: [`GreedySolver.kt:243-279`][greedy-bonus]
- Components:
  * Budget Usage (0-6 points)
  * Popular Items (0-5 points)
  * Highly Rated Items (0-5 points)
  * Kitchen Efficiency (0-4 points)

### 5.3 The Thorough Chef (Brute Force Solver) - Checks Everything!

The Brute Force solver is implemented in [`BruteForceSolver.kt`][brute-force-solver]. Here's how it works:

#### Combination Generation

**CG-1: Generate All Possibilities**
- What: Create every possible valid combination of items through exhaustive search
- Code: [`BruteForceSolver.kt:76-111`][brute-force-gen]
- Implementation:
```kotlin
private fun generateAndCheckCombinations(
    currentIndex: Int,
    currentCombination: MutableMap<ItemUnit, Int>,
    solutions: MutableList<Solution>,
    k: Int,
    startTime: Long,
    items: List<ItemUnit>
) {
    // Recursive backtracking to explore all combinations
    // Try different quantities (0 to max) for each item
    // Apply early pruning for efficiency
    // Store valid solutions as they're found
}
```

#### Smart Pruning and Validation

**SP-1: Early Constraint Checking**
- What: Prune invalid branches early to improve performance
- Code: [`BruteForceSolver.kt:115-131`][brute-force-validate]
- Implementation:
```kotlin
private fun canAdd(
    item: ItemUnit,
    quantity: Int,
    currentCombination: Map<ItemUnit, Int>
): Boolean {
    // Check kitchen capacity
    // Verify budget constraints
    // If any constraint fails, prune this branch
}
```

**VF-1: Comprehensive Validation**
- What: Validate each combination against all requirements
- Code: [`BruteForceSolver.kt:145-176`][brute-force-validate]
- Implementation:
```kotlin
private fun isValidCombination(combination: Map<ItemUnit, Int>): Boolean {
    // Check total servings
    // Verify dietary requirements
    // Validate portion sizes (adaptive based on group size)
    // Ensure fair distribution of items
}
```

#### Dynamic Group Size Adaptation

**DG-1: Adaptive Constraints**
- What: Adjust distribution constraints based on group size
- Implementation:
```kotlin
// For small groups (≤5 people)
val minItemPercentage = MIN_ITEM_PERCENTAGE_SMALL   // 10%
val maxItemPercentage = MAX_ITEM_PERCENTAGE_SMALL   // 50%
val targetDistributionRange = TARGET_DISTRIBUTION_RANGE_SMALL  // 30%

// For large groups (>5 people)
val minItemPercentage = MIN_ITEM_PERCENTAGE_LARGE   // 5%
val maxItemPercentage = MAX_ITEM_PERCENTAGE_LARGE   // 25%
val targetDistributionRange = TARGET_DISTRIBUTION_RANGE_LARGE  // 15%
```

#### Solution Scoring

**SS-1: Calculate Quality**
- What: Score each valid combination
- Code: [`BruteForceSolver.kt:198-226`][brute-force-score]
- Components:
  * Cost Efficiency (25 points): How well budget is utilized
  * Popular Items (20 points): Percentage of popular items
  * Highly Rated Items (20 points): Percentage of highly rated items
  * Kitchen Efficiency (15 points): Optimal kitchen capacity usage
  * Distribution Fairness (10 points): Even portion distribution
  * Item Diversity (10 points): Variety of different items

### 5.4 Choosing Your Chef

Now with three approaches, here's when to use each:

Master Chef (MILP) | Quick Chef (Greedy) | Thorough Chef (Brute Force)
------------------|-------------------|----------------------
Global optimization | Local decisions | Complete enumeration
Complex constraints | Simple rules | Exhaustive checking
Perfect but slower | Fast but approximate | Guaranteed optimal but slowest
100% optimal | 80-90% optimal | 100% optimal with all possibilities
0.2-2 seconds | 0.01-0.05 seconds | 1-60 seconds (menu size dependent)
Best for important orders | Best for quick orders | Best for small menus or testing

[milp-solver]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt
[greedy-solver]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt
[brute-force-solver]: src/main/kotlin/com/doordash/bundler/solver/BruteForceSolver.kt
[milp-quantity-vars]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L95-L98
[milp-selection-vars]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L182-L185
[milp-combination-vars]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L156-L159
[milp-total-servings]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L106-L108
[milp-dietary-reqs]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L112-L119
[milp-budget-limit]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L124-L128
[milp-kitchen-capacity]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L133-L137
[milp-fair-distribution]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L142-L171
[milp-min-variety]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L189-L193
[milp-cost-min]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L285-L297
[milp-quality-bonus]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L288-L293
[milp-base-metrics]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L299-L308
[milp-optimality-score]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L346-L397
[greedy-sorting]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt#L102-L116
[greedy-dietary]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt#L196-L214
[greedy-capacity]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt#L216-L224
[greedy-base-score]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt#L126-L141
[greedy-bonus]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt#L243-L279
[brute-force-gen]: src/main/kotlin/com/doordash/bundler/solver/BruteForceSolver.kt#L76-L111
[brute-force-validate]: src/main/kotlin/com/doordash/bundler/solver/BruteForceSolver.kt#L115-L131
[brute-force-score]: src/main/kotlin/com/doordash/bundler/solver/BruteForceSolver.kt#L198-L226

---

## 6. Real-World Examples with Explanations

### Example 1: Quick Office Lunch (Simple Case)
```kotlin
// The Situation:
// - Small office of 10 people
// - 3 vegetarians, 7 regular eaters
// - Budget: $12 per person
// - Need it quick!

// How We Solve It:
val solution = findOptimalCombination(
    people = 10,
    budget = 1200,  // $12.00 per person
    dietary = mapOf(VEG to 3, MEAT to 7),
    timeLimit = 1.seconds  // Quick solution needed
)

// What We Recommend:
// - 2 large pizzas (one veggie, one meat)
// - 3 salad bowls (all vegetarian)
// Total Cost: $105 ($10.50 per person)
// Solution Found: In just 0.3 seconds!
```

### Example 2: Corporate Event Catering (Complex Case)
```kotlin
// The Situation:
// - Big event for 50 people
// - Mixed dietary needs
// - Premium items preferred
// - Can take time to plan

// How We Solve It:
val solution = findOptimalCombination(
    people = 50,
    budget = 2000,  // $40 per person
    dietary = mapOf(
        VEGAN to 10,    // 10 vegan meals
        VEG to 15,      // 15 vegetarian meals
        MEAT to 25      // 25 regular meals
    ),
    preferences = PreferPremium  // High-quality items
)

// What We Recommend:
// - Perfect mix of 8 different premium items
// - 70% are premium menu items
// - Even distribution across dietary needs
// - Solution Found: In 1.5 seconds
```

---

## 7. When to Use What - A Performance Guide

### Time & Space Complexity Comparison

| Aspect | MILP Solver | Greedy Solver | Brute Force Solver |
|--------|-------------|---------------|-------------------|
| **Time Complexity** | Exponential worst-case (O(2^n)) but often performs well due to branch-and-bound | O(n log n) for sorting + O(n) for selection | O(n^m) where n = items, m = max quantity |
| **Space Complexity** | O(n) for variables and constraints | O(n) for item storage | O(n*s) where s = number of solutions stored |
| **Best Case** | Optimal solution for complex constraints | Near-optimal for simpler, well-structured problems | Optimal solution guaranteed (for small inputs) |
| **Worst Case** | May timeout on very large problems | May produce suboptimal solutions | Exponential explosion with large menus/quantities |
| **Suitable Input Size** | Medium-large (20-100 items, any group size) | Any size (scales well) | Small (5-15 items, <10 people) |

### Performance Characteristics

| Feature | MILP Solver | Greedy Solver | Brute Force Solver |
|---------|-------------|---------------|-------------------|
| **Solution Quality** | Globally optimal | Good approximation | Guaranteed optimal (for completable searches) |
| **Computation Speed** | Moderate-slow | Very fast | Slow for large inputs, fast for small inputs |
| **Handles Complex Constraints** | Excellent | Limited | Good |
| **Scalability** | Good with solver optimizations | Excellent | Poor |
| **Predictable Runtime** | Variable (harder to predict) | Very predictable | Predictable (but can be very long) |
| **Memory Usage** | Moderate | Low | High (for large inputs) |

### When to Use Each Approach

| Use Case | Recommended Approach |
|----------|---------------------|
| Standard catering (20-50 people) | MILP Solver |
| Very large orders (100+ people) | Greedy Solver |
| Small, important events (<10 people) | Brute Force Solver |
| Quick approximation needed | Greedy Solver |
| Complex dietary requirements | MILP Solver |
| Critical budget constraints | MILP or Brute Force Solver |
| Resource-limited environments | Greedy Solver |

### When to Use Thorough Chef (Brute Force Solver)
Best For:
- Small menus (<15 items) or testing
- Smaller group sizes (<10 people)
- When you need guaranteed optimal solutions
- When you have time for more computation

Technical Details:
```kotlin
// Processing Time: Slowest (O(n^m) where n = items, m = max quantity)
// Memory Needed: More (tracks all valid combinations)
// Result Quality: 100% optimal with exhaustive search
// Adapts constraints based on group size for better results with larger groups
```

---

## 8. Future Improvements

### Making Portions More Even
Business Need | Technical Solution | How It Helps
--------------|-------------------|-------------
Better portion sizes | Smart distribution rules | No more tiny portions mixed with huge ones
Custom serving sizes | Flexible portion rules | Handle special requests easily
Better item combinations | Smart mixing logic | Items that go well together

### Learning from Experience
Business Need | Technical Solution | How It Helps
--------------|-------------------|-------------
Remember popular orders | Machine learning | Suggest what worked before
Predict busy times | Time analysis | Be ready for rush hours
Seasonal menu planning | Smart menu weights | Right items for the season

### Making It Faster
Business Need | Technical Solution | How It Helps
--------------|-------------------|-------------
Instant solutions | Parallel processing | Use multiple computers
Handle huge menus | Smart filtering | Focus on relevant items
Quick alternatives | Save good solutions | Reuse what works 