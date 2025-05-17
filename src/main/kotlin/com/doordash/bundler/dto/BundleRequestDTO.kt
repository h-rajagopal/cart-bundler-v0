package com.doordash.bundler.dto

import com.doordash.bundler.model.DietaryTag
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class BundleRequestDTO @JsonCreator constructor(
    @JsonProperty("people") val people: Int,
    @JsonProperty("budgetCents") val maxPricePerPersonCents: Int,
    @JsonProperty("requiredByDiet") val requiredByDiet: Map<DietaryTag, Int>,
    @JsonProperty("topN") val topN: Int = 1,
    @JsonProperty("kitchenCap") val kitchenCap: Int = 10_000,
    @JsonProperty("menu") val menu: List<MenuItemDTO>,
) 