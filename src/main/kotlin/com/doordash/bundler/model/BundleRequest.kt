package com.doordash.bundler.model

data class BundleRequest(
    val people: Int,
    val maxPricePerPersonCents: Int,
    val requiredByDiet: Map<DietaryTag, Int>,
    val topN: Int,
) 