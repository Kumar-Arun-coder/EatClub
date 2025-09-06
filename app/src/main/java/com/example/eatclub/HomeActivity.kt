//package com.example.eatclub
//
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.inputmethod.InputMethodManager
//import android.widget.EditText
//import android.widget.ImageView
//import android.widget.Toast
//import androidx.activity.enableEdgeToEdge
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.widget.SearchView
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.bumptech.glide.Glide
//import com.example.eatclub.adapter.MenuAdapter
//import com.example.eatclub.data.FoodItem // Old data model, kept for reference or other uses
//import com.example.eatclub.data.FoodItemV2 // New data model for the RecyclerView
//import com.example.eatclub.databinding.ActivityHomeBinding
//import com.google.android.material.appbar.MaterialToolbar
//import com.google.firebase.auth.FirebaseAuth
//import java.io.File
//import java.io.FileOutputStream
//import java.io.InputStream
//import java.util.UUID // For generating unique IDs if needed
//import com.example.eatclub.network.RetrofitClient // Import
//import retrofit2.Call // Import
//import retrofit2.Callback // Import
//import retrofit2.Response // Import
//
//
//class HomeActivity : AppCompatActivity(), AddItemBottomSheetDialogFragment.AddItemListener {
//
//    private lateinit var binding: ActivityHomeBinding
//    private lateinit var menuAdapter: MenuAdapter // Will handle FoodItemV2
//    private lateinit var firebaseAuth: FirebaseAuth
//    private var profileImageUri: Uri? = null
//
//    // Old list, kept if you have other logic depending on it, but not for the main RecyclerView
//    private var allFoodItems: List<FoodItem> = listOf()
//
//    // Primary data source for the RecyclerView
//    private var allFoodItemsV2: MutableList<FoodItemV2> = mutableListOf()
//
//    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
//        uri?.let {
//            profileImageUri = it
//            loadProfileImage(it)
//            saveProfileImagePath(it)
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        binding = ActivityHomeBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        firebaseAuth = FirebaseAuth.getInstance()
//
//        val desiredDarkColor = Color.parseColor("#1E1F22")
//        window.navigationBarColor = desiredDarkColor
//
//        setupToolbarAndSearch()
//        setupRecyclerView() // Initializes menuAdapter for FoodItemV2
//        // loadSampleData() // Call this if you still need to populate 'allFoodItems' for other reasons
//        loadSampleDataV2()    // This populates 'allFoodItemsV2' and updates the adapter
//        setupLogoutButton()
//        loadSavedProfileImage()
//    }
//
//    private fun setupToolbarAndSearch() {
//        val toolbar: MaterialToolbar = binding.toolbar // Ensure binding.toolbar is correct ID
//        val profileIcon: ImageView? = toolbar.findViewById(R.id.toolbar_profile_icon)
//        val addIcon: ImageView? = toolbar.findViewById(R.id.toolbar_add_icon)
//
//        profileIcon?.setOnClickListener {
//            pickImageLauncher.launch("image/*")
//        }
//
//        addIcon?.setOnClickListener {
//            val bottomSheet = AddItemBottomSheetDialogFragment.newInstance()
//            bottomSheet.setAddItemListener(this)
//            bottomSheet.show(supportFragmentManager, AddItemBottomSheetDialogFragment.TAG)
//        }
//
//        binding.imageViewFilter.setOnClickListener {
//            Toast.makeText(this, "Filter icon clicked!", Toast.LENGTH_SHORT).show()
//            // TODO: Implement filter logic (e.g., show a filter dialog/bottom sheet)
//        }
//
//        val searchView = binding.searchView
//        searchView.setOnClickListener {
//            if (!searchView.isIconified) {
//                searchView.requestFocus()
//                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
//                searchEditText?.let { imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
//            } else {
//                searchView.isIconified = false
//            }
//        }
//
//        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean {
//                filterFoodItems(query)
//                searchView.clearFocus()
//                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
//                return true
//            }
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                filterFoodItems(newText)
//                return true
//            }
//        })
//    }
//
//    override fun onItemAdded(item: FoodItemV2) {
//        if (allFoodItemsV2.any { it.id == item.id }) {
//            Toast.makeText(this, "Item with ID ${item.id} already exists.", Toast.LENGTH_LONG).show()
//            return
//        }
//
//        allFoodItemsV2.add(0, item)
//        // For ListAdapter, submit a new copy of the list for efficient diffing
//        menuAdapter.submitList(allFoodItemsV2.toList())
//        binding.recyclerViewMenuItems.smoothScrollToPosition(0)
//
//        Toast.makeText(this, "${item.name} added!", Toast.LENGTH_SHORT).show()
//        // TODO: Persist this new FoodItemV2 to a database or backend
//    }
//
//    private fun filterFoodItems(query: String?) {
//        val filteredListV2 = if (query.isNullOrBlank()) {
//            allFoodItemsV2 // If query is empty, show all FoodItemV2 items
//        } else {
//            val lowerCaseQuery = query.lowercase().trim()
//            allFoodItemsV2.filter { foodItemV2 ->
//                foodItemV2.name.lowercase().contains(lowerCaseQuery) ||
//                        foodItemV2.description.lowercase().contains(lowerCaseQuery) ||
//                        foodItemV2.category.lowercase().contains(lowerCaseQuery)
//                // Add more fields to search from FoodItemV2 if needed
//            }
//        }
//        menuAdapter.submitList(filteredListV2.toList()) // Submit List<FoodItemV2>
//    }
//
//
//    // This function now populates allFoodItemsV2 and updates the MenuAdapter
//    private fun loadSampleDataV2() {
//        if (allFoodItemsV2.isEmpty()) { // Only load if the list is currently empty
//            val sampleItemsV2 = mutableListOf(
//                FoodItemV2(
//                    id = System.currentTimeMillis(), // Or use a proper ID generation scheme
//                    name = "Spicy Ramen",
//                    category = "Main Course",
//                    price = 220.00,
//                    description = "Authentic spicy ramen with rich broth and fresh toppings.",
//                    imageUri = null, // You would set actual URIs if available, or handle null in adapter
//                    isActive = true,
//                    dateAdded = System.currentTimeMillis() - (2 * 86400000L) // 2 days ago
//                ),
//                FoodItemV2(
//                    id = System.currentTimeMillis() + 1,
//                    name = "Mango Lassi",
//                    category = "Beverage",
//                    price = 80.00,
//                    description = "Cool and refreshing yogurt-based mango drink.",
//                    imageUri = null,
//                    isActive = true,
//                    dateAdded = System.currentTimeMillis() - (1 * 86400000L) // Yesterday
//                ),
//                FoodItemV2(
//                    id = System.currentTimeMillis() + 2,
//                    name = "Veggie Spring Rolls",
//                    category = "Appetizer",
//                    price = 120.00,
//                    description = "Crispy fried spring rolls filled with fresh vegetables.",
//                    imageUri = null,
//                    isActive = false, // Example of an inactive item
//                    dateAdded = System.currentTimeMillis() // Today
//                )
//                // Add more FoodItemV2 samples as needed
//            )
//            allFoodItemsV2.addAll(sampleItemsV2)
//            Log.d("HomeActivity", "loadSampleDataV2 - Populated ${allFoodItemsV2.size} FoodItemV2 items.")
//        } else {
//            Log.d("HomeActivity", "loadSampleDataV2 - allFoodItemsV2 already populated.")
//        }
//        menuAdapter.submitList(allFoodItemsV2.toList()) // Submit the current list to the adapter
//    }
//
//    // Kept for reference or if you use `allFoodItems` for something not related to the main RecyclerView
//    private fun loadSampleData() {
//        val sampleFoodItems = listOf(
//            FoodItem(id = 1, name = "Old Cheeseburger", description = "...", price = 199.00, imageResId = R.drawable.cheeseburger),
//            FoodItem(id = 2, name = "Old Margherita Pizza", description = "...", price = 250.00, imageResId = R.drawable.margheritapizza)
//        )
//        allFoodItems = sampleFoodItems
//        Log.d("HomeActivity", "loadSampleData (Old) - allFoodItems size : ${allFoodItems.size}")
//        // DO NOT submit allFoodItems to the menuAdapter meant for FoodItemV2
//        // menuAdapter.submitList(allFoodItems) // THIS WOULD CAUSE AN ERROR
//    }
//
//
//    private fun loadProfileImage(uri: Uri) {
//        // Assuming binding.toolbar.toolbar_profile_icon is the ImageView in your custom toolbar layout
//        val profileIconImageView: ImageView? = binding.toolbar.findViewById(R.id.toolbar_profile_icon)
//        profileIconImageView?.let {
//            Glide.with(this)
//                .load(uri)
//                .circleCrop()
//                .placeholder(R.drawable.profile_icon)
//                .error(R.drawable.profile_icon)
//                .into(it)
//        }
//    }
//
//    private fun saveProfileImagePath(uri: Uri) {
//        try {
//            val internalFile = copyUriToInternalStorage(uri, "profile_image.jpg")
//            internalFile?.let {
//                val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
//                with(sharedPreferences.edit()) {
//                    putString("profile_image_path", it.absolutePath)
//                    apply()
//                }
//                // Toast.makeText(this, "Profile image saved", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            Toast.makeText(this, "Failed to save profile image", Toast.LENGTH_SHORT).show()
//            Log.e("HomeActivity", "saveProfileImagePath Error", e)
//        }
//    }
//
//    private fun loadSavedProfileImage() {
//        val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
//        val imagePath = sharedPreferences.getString("profile_image_path", null)
//        imagePath?.let {
//            val imageFile = File(it)
//            if (imageFile.exists()) {
//                val uri = Uri.fromFile(imageFile)
//                loadProfileImage(uri)
//            }
//        }
//    }
//
//    private fun copyUriToInternalStorage(uri: Uri, fileName: String): File? {
//        return try {
//            val inputStream: InputStream? = contentResolver.openInputStream(uri)
//            val file = File(filesDir, fileName)
//            val outputStream = FileOutputStream(file)
//            inputStream?.copyTo(outputStream)
//            inputStream?.close()
//            outputStream.close()
//            file
//        } catch (e: Exception) {
//            Log.e("HomeActivity", "copyUriToInternalStorage Error", e)
//            null
//        }
//    }
//
//    private fun setupRecyclerView() {
//        menuAdapter = MenuAdapter { clickedFoodItemV2 -> // Lambda now receives FoodItemV2
//            Toast.makeText(this, "${clickedFoodItemV2.name} clicked!", Toast.LENGTH_SHORT).show()
//            // TODO: Handle item click, e.g., navigate to a detail screen for FoodItemV2
//        }
//        binding.recyclerViewMenuItems.apply {
//            adapter = menuAdapter
//            layoutManager = LinearLayoutManager(this@HomeActivity)
//            setHasFixedSize(true) // Consider if your item sizes change
//        }
//    }
//
//    private fun setupLogoutButton() {
//        binding.buttonLogout.setOnClickListener {
//            firebaseAuth.signOut()
//            val intent = Intent(this, LoginActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            startActivity(intent)
//            finish()
//            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
//        }
//    }
//}
//
package com.example.eatclub

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View // Import View for visibility changes
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.eatclub.adapter.MenuAdapter
import com.example.eatclub.data.FoodItem // Old data model
import com.example.eatclub.data.FoodItemV2 // New data model
import com.example.eatclub.databinding.ActivityHomeBinding
import com.example.eatclub.network.AppPreferences // For saving/getting Firebase token
import com.example.eatclub.network.RetrofitClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
// Removed: import java.util.UUID as it's not used in this version

class HomeActivity : AppCompatActivity(), AddItemBottomSheetDialogFragment.AddItemListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var firebaseAuth: FirebaseAuth
    private var profileImageUri: Uri? = null

    // Old list, kept for reference
    private var allFoodItems: List<FoodItem> = listOf()

    // Primary data source for the RecyclerView
    private var allFoodItemsV2: MutableList<FoodItemV2> = mutableListOf()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            profileImageUri = it
            loadProfileImage(it)
            saveProfileImagePath(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        // Check if user is logged in, if not, redirect to LoginActivity
        if (firebaseAuth.currentUser == null) {
            navigateToLogin()
            return // Important to prevent rest of onCreate from running
        }

        val desiredDarkColor = Color.parseColor("#1E1F22")
        window.navigationBarColor = desiredDarkColor

        setupToolbarAndSearch()
        setupRecyclerView()
        setupLogoutButton()
        loadSavedProfileImage()

        // Get Firebase ID token and then load data
        getFirebaseIdTokenAndLoadData()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun getFirebaseIdTokenAndLoadData() {
        binding.progressBar.visibility = View.VISIBLE // Show progress bar early
        firebaseAuth.currentUser?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val idToken: String? = task.result?.token
                if (idToken != null) {
                    Log.d("HomeActivity", "Firebase ID Token obtained successfully.")
                    AppPreferences.saveFirebaseToken(this, idToken) // Save token for AuthInterceptor
                    loadFoodItemsFromServer() // Now load data with the token set
                } else {
                    Log.e("HomeActivity", "Firebase ID Token is null.")
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Authentication error. Please try logging in again.", Toast.LENGTH_LONG).show()
                    navigateToLogin() // Or handle re-authentication
                }
            } else {
                Log.e("HomeActivity", "Failed to get Firebase ID token: ${task.exception?.message}")
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Failed to authenticate. Please try logging in again.", Toast.LENGTH_LONG).show()
                navigateToLogin() // Or handle re-authentication
            }
        } ?: run {
            // This case (currentUser is null) should have been caught by the check in onCreate
            Log.e("HomeActivity", "User is not logged in when trying to get token.")
            binding.progressBar.visibility = View.GONE
            navigateToLogin()
        }
    }

    private fun setupToolbarAndSearch() {
        val toolbar: MaterialToolbar = binding.toolbar
        val profileIcon: ImageView? = toolbar.findViewById(R.id.toolbar_profile_icon)
        val addIcon: ImageView? = toolbar.findViewById(R.id.toolbar_add_icon)

        profileIcon?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        addIcon?.setOnClickListener {
            val bottomSheet = AddItemBottomSheetDialogFragment.newInstance()
            bottomSheet.setAddItemListener(this)
            bottomSheet.show(supportFragmentManager, AddItemBottomSheetDialogFragment.TAG)
        }

        binding.imageViewFilter.setOnClickListener {
            Toast.makeText(this, "Filter icon clicked!", Toast.LENGTH_SHORT).show()
            // TODO: Implement filter logic
        }

        val searchView = binding.searchView
        searchView.setOnClickListener {
            if (!searchView.isIconified) {
                searchView.requestFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
                searchEditText?.let { imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT) }
            } else {
                searchView.isIconified = false
            }
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterFoodItems(query)
                searchView.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterFoodItems(newText)
                return true
            }
        })
        // Handle search view close button
        try {
            val closeButton: ImageView? = searchView.findViewById(androidx.appcompat.R.id.search_close_btn)
            closeButton?.setOnClickListener {
                searchView.setQuery("", false)
                searchView.clearFocus()
                menuAdapter.submitList(allFoodItemsV2.toList()) // Show all items when search is cleared
            }
        } catch (e: Exception) {
            Log.e("HomeActivity", "Error finding search_close_btn", e)
        }
    }

    // --- Network Operations ---
    private fun loadFoodItemsFromServer() {
        Log.d("HomeActivity", "Attempting to load food items from server...")
        // ProgressBar visibility is handled by getFirebaseIdTokenAndLoadData initially,
        // but ensure it's shown if this method is called independently later.
        if (binding.progressBar.visibility == View.GONE) {
            binding.progressBar.visibility = View.VISIBLE
        }

        RetrofitClient.getInstance(this).getFoodItems().enqueue(object : Callback<List<FoodItemV2>> {
            override fun onResponse(call: Call<List<FoodItemV2>>, response: Response<List<FoodItemV2>>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    response.body()?.let { items ->
                        allFoodItemsV2.clear()
                        allFoodItemsV2.addAll(items)
                        menuAdapter.submitList(allFoodItemsV2.toList())
                        Log.d("HomeActivity", "Items loaded from server: ${items.size}")
                        if (items.isEmpty()) {
                            Toast.makeText(this@HomeActivity, "No food items found.", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Log.w("HomeActivity", "Response body is null, though successful.")
                        Toast.makeText(this@HomeActivity, "No data received.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMsg = "Failed to load items: ${response.code()} - ${response.message()}"
                    Toast.makeText(this@HomeActivity, errorMsg, Toast.LENGTH_LONG).show()
                    Log.e("HomeActivity", "API Load Error: $errorMsg, Body: ${response.errorBody()?.string()}")
                }
            }

            override fun onFailure(call: Call<List<FoodItemV2>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                val errorMsg = "Network Error loading items: ${t.message}"
                Toast.makeText(this@HomeActivity, errorMsg, Toast.LENGTH_LONG).show()
                Log.e("HomeActivity", "Network Load Failure: ${t.message}", t)
            }
        })
    }

    override fun onItemAdded(itemFromDialog: FoodItemV2) {
        binding.progressBar.visibility = View.VISIBLE
        Log.d("HomeActivity", "Attempting to add item: ${itemFromDialog.name}")

        // Ensure the client-generated ID is part of the object sent to the server
        // The server backend Python code expects 'id' for client_id
        val itemToSend = itemFromDialog.copy(id = itemFromDialog.id) // Ensure 'id' is present

        RetrofitClient.getInstance(this).addFoodItem(itemToSend).enqueue(object : Callback<FoodItemV2> {
            override fun onResponse(call: Call<FoodItemV2>, response: Response<FoodItemV2>) {
                binding.progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    response.body()?.let { createdItemFromServer ->
                        Log.d("HomeActivity", "Item added successfully via API. Server ID: ${createdItemFromServer.mongoId}")
                        allFoodItemsV2.add(0, createdItemFromServer) // Add the server-confirmed item
                        menuAdapter.submitList(allFoodItemsV2.toList())
                        binding.recyclerViewMenuItems.smoothScrollToPosition(0)
                        Toast.makeText(this@HomeActivity, "${createdItemFromServer.name} added!", Toast.LENGTH_SHORT).show()
                    } ?: run {
                        Log.w("HomeActivity", "Add item response body is null, though successful.")
                        Toast.makeText(this@HomeActivity, "Item added, but no confirmation data.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = "Failed to add item: ${response.code()} - ${errorBody ?: response.message()}"
                    Toast.makeText(this@HomeActivity, errorMsg, Toast.LENGTH_LONG).show()
                    Log.e("HomeActivity", "API Add Error: ${response.code()} - $errorBody")
                }
            }

            override fun onFailure(call: Call<FoodItemV2>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                val errorMsg = "Network Error adding item: ${t.message}"
                Toast.makeText(this@HomeActivity, errorMsg, Toast.LENGTH_LONG).show()
                Log.e("HomeActivity", "Network Add Failure: ${t.message}", t)
            }
        })
    }
    // --- End Network Operations ---

    private fun filterFoodItems(query: String?) {
        val filteredListV2 = if (query.isNullOrBlank()) {
            allFoodItemsV2
        } else {
            val lowerCaseQuery = query.lowercase().trim()
            allFoodItemsV2.filter { foodItemV2 ->
                foodItemV2.name.lowercase().contains(lowerCaseQuery) ||
                        foodItemV2.description.lowercase().contains(lowerCaseQuery) ||
                        foodItemV2.category.lowercase().contains(lowerCaseQuery)
            }
        }
        menuAdapter.submitList(filteredListV2.toList())
    }

    // This function is now effectively replaced by loadFoodItemsFromServer for RecyclerView data
    // It can be kept if you need local sample data for testing without a backend,
    // or removed if all data comes from the server.
    private fun loadSampleDataV2() {
        // If you want to show local sample data *before* server data loads,
        // or if server fails, you can populate allFoodItemsV2 here.
        // For now, let's assume server is the primary source.
        // If you want to keep this for offline/initial state:
        if (allFoodItemsV2.isEmpty()) {
            val sampleItemsV2 = mutableListOf(
                FoodItemV2( // Ensure these match the FoodItemV2 constructor
                    id = 1L, // Example client ID
                    name = "Local Spicy Ramen", category = "Main Course", price = 220.00,
                    description = "Authentic spicy ramen.", imageUri = null,
                    isActive = true, dateAdded = System.currentTimeMillis() - (2 * 86400000L)
                ),
                // Add more local samples if needed
            )
            // allFoodItemsV2.addAll(sampleItemsV2)
            // menuAdapter.submitList(allFoodItemsV2.toList())
            Log.d("HomeActivity", "loadSampleDataV2 called, but server data is preferred.")
        }
        // Data loading is now primarily handled by loadFoodItemsFromServer()
    }


    // Kept for reference or other uses, not for RecyclerView data
    private fun loadSampleData() {
        val sampleFoodItems = listOf(
            FoodItem(id = 1, name = "Old Burger", description = "...", price = 1.0, imageResId = R.drawable.cheeseburger)
        )
        allFoodItems = sampleFoodItems
        Log.d("HomeActivity", "loadSampleData (Old Model) - Size: ${allFoodItems.size}")
    }

    private fun loadProfileImage(uri: Uri) {
        val profileIconImageView: ImageView? = binding.toolbar.findViewById(R.id.toolbar_profile_icon)
        profileIconImageView?.let {
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .placeholder(R.drawable.profile_icon)
                .error(R.drawable.profile_icon)
                .into(it)
        }
    }

    private fun saveProfileImagePath(uri: Uri) {
        try {
            val internalFile = copyUriToInternalStorage(uri, "profile_image.jpg")
            internalFile?.let {
                val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("profile_image_path", it.absolutePath)
                    apply()
                }
            }
        } catch (e: Exception) {
            Log.e("HomeActivity", "saveProfileImagePath Error", e)
            Toast.makeText(this, "Failed to save profile image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedProfileImage() {
        val sharedPreferences = getSharedPreferences("UserProfile", Context.MODE_PRIVATE)
        val imagePath = sharedPreferences.getString("profile_image_path", null)
        imagePath?.let {
            val imageFile = File(it)
            if (imageFile.exists()) {
                val uri = Uri.fromFile(imageFile)
                loadProfileImage(uri)
            }
        }
    }

    private fun copyUriToInternalStorage(uri: Uri, fileName: String): File? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            Log.e("HomeActivity", "copyUriToInternalStorage Error", e)
            null
        }
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuAdapter { clickedFoodItemV2 ->
            Toast.makeText(this, "Clicked: ${clickedFoodItemV2.name} (ServerID: ${clickedFoodItemV2.mongoId ?: "N/A"})", Toast.LENGTH_SHORT).show()
            // TODO: Handle item click, e.g., navigate to detail screen using mongoId
        }
        binding.recyclerViewMenuItems.apply {
            adapter = menuAdapter
            layoutManager = LinearLayoutManager(this@HomeActivity)
            // setHasFixedSize(true) // Only if item sizes are truly fixed. Can cause issues if not.
        }
    }

    private fun setupLogoutButton() {
        binding.buttonLogout.setOnClickListener {
            firebaseAuth.signOut()
            AppPreferences.saveFirebaseToken(this, null) // Clear the saved token
            navigateToLogin()
            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
        }
    }
}
