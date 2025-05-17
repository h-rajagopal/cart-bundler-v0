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

Let's understand each part of our system like a restaurant kitchen:

### Order Taker (REST API Layer)
```
What It Does:
- Takes customer orders (like a waiter)
- Checks if orders make sense (like a senior waiter)
- Passes orders to the kitchen (returns menu plans)

Technical Terms:
- HTTP endpoint = Digital order counter
- Input validation = Order check
- DTO transformation = Order ticket creation
```

### Kitchen Manager (Bundle Service)
```
What It Does:
- Coordinates between stations (planning)
- Compares different ways to make order (options)
- Suggests the best approach (recommendations)

Technical Terms:
- Solver orchestration = Kitchen coordination
- Solution comparison = Checking different methods
- Strategy selection = Choosing best cooking method
```

### Master Chef (MILP Solver)
```
What It Does:
- Plans perfect meal combinations
- Makes sure all requirements are met
- Finds the most efficient solution

Technical Terms:
- Variable creation = Listing all possible ingredients
- Constraint satisfaction = Following recipe rules
- Objective minimization = Getting best results
```

### Quick Chef (Greedy Solver)
```
What It Does:
- Makes fast decisions
- Uses simple, proven rules
- Gets good results quickly

Technical Terms:
- Priority sorting = Picking most important items first
- Iterative selection = Adding items one by one
- Local optimization = Making each choice good
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

### 5.3 Choosing Your Chef

The key differences between the solvers are reflected in their implementations:

Master Chef (MILP) | Quick Chef (Greedy)
------------------|-------------------
Global optimization ([Lines 285-297][milp-cost-min]) | Local decisions ([Lines 196-224][greedy-dietary])
Complex constraints ([Lines 106-193][milp-constraints]) | Simple rules ([Lines 102-116][greedy-sorting])
Perfect but slower (Full model solve) | Fast but approximate (Single pass)
100% optimal solution | 80-90% optimal solution

[milp-solver]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt
[greedy-solver]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt
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
[milp-constraints]: src/main/kotlin/com/doordash/bundler/solver/OrToolsMilpSolver.kt#L106-L193
[greedy-sorting]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt#L102-L116
[greedy-dietary]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt#L196-L214
[greedy-capacity]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt#L216-L224
[greedy-base-score]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt#L126-L141
[greedy-bonus]: src/main/kotlin/com/doordash/bundler/solver/GreedySolver.kt#L243-L279

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

### When to Use Master Chef (MILP Solver)
Best For:
- Important events needing perfect balance
- Complex dietary requirements
- Premium events where quality matters
- When you have 1-2 seconds to plan

Technical Details:
```kotlin
// Processing Time: Takes longer but gets perfect results
// Memory Needed: More (tracks many combinations)
// Result Quality: The absolute best possible
```

### When to Use Quick Chef (Greedy Solver)
Best For:
- Quick lunch orders
- Simple dietary needs
- Standard menu items
- When you need instant results

Technical Details:
```kotlin
// Processing Time: Lightning fast
// Memory Needed: Very little
// Result Quality: Good (but may not be perfect)
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