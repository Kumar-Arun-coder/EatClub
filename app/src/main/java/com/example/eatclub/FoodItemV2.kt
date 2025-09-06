package com.example.eatclub.data

import com.google.gson.annotations.SerializedName


// Already defined in the BottomSheetDialogFragment example, but good to have in its own file
 data class FoodItemV2(
    var id: Long = System.currentTimeMillis(),
    @SerializedName("_id")
    val mongoId: String? = null,
    val name: String,
    val category: String,
    val price: Double,
    val description: String,
    var imageUri: String?, // Store as String URI
    val isActive: Boolean,
    val dateAdded: Long, // Store as milliseconds (timestamp)
    val added_by_uid: String? = null,
    val added_at: String? = null
 )