package com.doordash.bundler.dto

import com.doordash.bundler.model.DietaryTag
import com.doordash.bundler.rating.StoreItemRating
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

data class MenuItemDTO @JsonCreator constructor(
    @JsonProperty("id") val id: String,
    @JsonProperty("name") val name: String,
    @JsonProperty("price") val priceCents: Int,
    @JsonProperty("serves") val serves: Int,
    @JsonProperty("diet") val diet: DietaryTag,
    @JsonProperty("stock") val availableServings: Int = Int.MAX_VALUE,
    @JsonProperty("load") val prepLoadPerServing: Int = 1,
    @JsonProperty("rating") val rating: StoreItemRating? = null
) 