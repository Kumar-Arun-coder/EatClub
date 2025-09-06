package com.example.eatclub.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.util.Locale


@Entity(tableName = "food_items") // Defines the table name for Room
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0, // Primary key for Room, auto-generated
    val name: String,
    val description: String,
    val price: Double, // Using Double for price, format later for display
    val imageResId: Int // Placeholder for now, can be a URL or local resource name/path

)

// Example of how you might want to display the price:
fun FoodItem.getFormattedPrice(currencySymbol: String = "₹"): String {
    return String.format("%s%.2f", currencySymbol, this.price)
}
