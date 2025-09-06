package com.example.eatclub.adapter // Or your preferred package name

import android.net.Uri // Import for Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eatclub.R
import com.example.eatclub.data.FoodItemV2 // Import your FoodItemV2 data class
// Remove: import com.example.eatclub.data.FoodItem
// Remove: import com.example.eatclub.data.getFormattedPrice (unless you adapt it for FoodItemV2)
import com.example.eatclub.databinding.ItemMenuFoodBinding // Import ViewBinding class
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MenuAdapter(private val onItemClicked: (FoodItemV2) -> Unit) :
    ListAdapter<FoodItemV2, MenuAdapter.FoodItemViewHolder>(FoodItemV2DiffCallback()) { // Use FoodItemV2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodItemViewHolder {
        val binding = ItemMenuFoodBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FoodItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodItemViewHolder, position: Int) {
        val currentItem = getItem(position) // currentItem is now FoodItemV2
        holder.bind(currentItem) // Pass FoodItemV2 to bind

        holder.itemView.setOnClickListener {
            onItemClicked(currentItem)
        }
        // The Glide logic is now correctly placed within the ViewHolder's bind method.
    }

    inner class FoodItemViewHolder(
        // Make binding accessible to the outer class if needed for the Glide part in onBindViewHolder,
        // but it's cleaner to keep all binding logic inside bind().
        // For this updated version, binding is private to the ViewHolder.
        private val binding: ItemMenuFoodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodItemV2) { // Parameter is FoodItemV2
            binding.apply {
                textViewFoodTitle.text = item.name
                textViewFoodDescription.text = item.description

                // Format price for FoodItemV2 (assuming item.price is Double)
                val format: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN")) // Example: India (Rupees)
                // Or a generic currency format:
                // val format: NumberFormat = NumberFormat.getCurrencyInstance()
                // format.maximumFractionDigits = 2 // Ensure two decimal places
                // format.currency = Currency.getInstance("USD") // Example: USD
                textViewFoodPrice.text = format.format(item.price)

                // Example: Displaying other FoodItemV2 fields (add TextViews to your item_menu_food.xml)
                // textViewCategory.text = "Category: ${item.category}"
                // switchIsActiveDisplay.isChecked = item.isActive // If you have a switch to display status
                // textViewDateAdded.text = "Added: ${formatDate(item.dateAdded)}"


                // Image loading logic using Glide for imageUri
                item.imageUri?.let { uriString ->
                    Glide.with(itemView.context) // itemView is a property of ViewHolder
                        .load(Uri.parse(uriString))
                        .placeholder(R.drawable.placeholder_image) // Ensure this drawable exists
                        .error(R.drawable.error_image)       // Ensure this drawable exists
                        .into(imageViewFoodItem) // Use the ID from your ViewBinding object
                } ?: run {
                    // Handle case where imageUri is null or item has imageResId (fallback or specific logic)
                    // For now, defaulting to placeholder if imageUri is null
                    imageViewFoodItem.setImageResource(R.drawable.placeholder_image)
                }
            }
        }

        private fun formatDate(timestamp: Long): String {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    // DiffUtil helps RecyclerView efficiently update items for FoodItemV2
    class FoodItemV2DiffCallback : DiffUtil.ItemCallback<FoodItemV2>() {
        override fun areItemsTheSame(oldItem: FoodItemV2, newItem: FoodItemV2): Boolean {
            return oldItem.id == newItem.id // Assuming 'id' is a unique identifier in FoodItemV2
        }

        override fun areContentsTheSame(oldItem: FoodItemV2, newItem: FoodItemV2): Boolean {
            return oldItem == newItem // Data class 'equals' checks all properties
        }
    }
}

