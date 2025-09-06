package com.example.eatclub

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.eatclub.data.FoodItemV2
import com.example.eatclub.databinding.AddItemBottomSheetLayoutBinding // ViewBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

// Define a new data class that includes all the new fields
data class FoodItemV2(
    val id: Long,
    val name: String,
    val category: String,
    val price: Double,
    val description: String,
    var imageUri: String?, // Store as String URI
    val isActive: Boolean,
    val dateAdded: Long // Store as milliseconds (timestamp)
)

class AddItemBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private var _binding: AddItemBottomSheetLayoutBinding? = null
    private val binding get() = _binding!!

    interface AddItemListener {
        fun onItemAdded(item: FoodItemV2)
    }

    private var listener: AddItemListener? = null
    private var selectedImageUri: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private val calendar: Calendar = Calendar.getInstance()

    // Sample categories - replace with your actual categories
    private val categories = arrayOf("Appetizer", "Main Course", "Dessert", "Beverage", "Snack")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    selectedImageUri = uri
                    Glide.with(this).load(uri).into(binding.imageViewItemImage)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AddItemBottomSheetLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCategorySpinner()
        setInitialDateTime()

        binding.buttonSheetClose.setOnClickListener { dismiss() }
        binding.buttonSheetCancel.setOnClickListener { dismiss() }

        binding.buttonSelectItemImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            imagePickerLauncher.launch(intent)
        }

        binding.editTextDateAdded.setOnClickListener {
            showDateTimePickerDialog()
        }
        binding.layoutDateAdded.setEndIconOnClickListener { // If you want the icon to trigger it too
            showDateTimePickerDialog()
        }


        binding.buttonSheetSubmit.setOnClickListener {
            if (validateInput()) {
                val newItem = FoodItemV2(
                    id = binding.editTextItemId.text.toString().toLong(),
                    name = binding.editTextItemName.text.toString(),
                    category = binding.autoCompleteCategory.text.toString(),
                    price = binding.editTextItemPrice.text.toString().toDouble(),
                    description = binding.editTextItemDescription.text.toString(),
                    imageUri = selectedImageUri?.toString(),
                    isActive = binding.switchIsActive.isChecked,
                    dateAdded = calendar.timeInMillis
                )
                listener?.onItemAdded(newItem)
                dismiss()
            }
        }
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.autoCompleteCategory.setAdapter(adapter)
    }

    private fun setInitialDateTime() {
        // Set current date and time as default
        calendar.time = Date()
        updateDateEditText()
    }

    private fun updateDateEditText() {
        val sdf = SimpleDateFormat("EEE, MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        binding.editTextDateAdded.setText(sdf.format(calendar.time))
    }

    private fun showDateTimePickerDialog() {
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            TimePickerDialog(requireContext(), { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                updateDateEditText()
            }, currentHour, currentMinute, false).show() // false for 12-hour format with AM/PM

        }, currentYear, currentMonth, currentDay).show()
    }


    private fun validateInput(): Boolean {
        var isValid = true
        if (binding.editTextItemId.text.isNullOrBlank()) {
            binding.layoutItemId.error = "Required"
            isValid = false
        } else {
            binding.layoutItemId.error = null
        }
        // Add more validation for other fields (name, price, category selected etc.)
        if (binding.editTextItemName.text.isNullOrBlank()) {
            binding.layoutItemName.error = "Required"
            isValid = false
        } else {
            binding.layoutItemName.error = null
        }
        if (binding.autoCompleteCategory.text.isNullOrBlank()) {
            binding.layoutItemCategory.error = "Please select a category"
            isValid = false
        } else {
            binding.layoutItemCategory.error = null
        }
        if (binding.editTextItemPrice.text.isNullOrBlank() || binding.editTextItemPrice.text.toString().toDoubleOrNull() == null) {
            binding.layoutItemPrice.error = "Valid price required"
            isValid = false
        } else {
            binding.layoutItemPrice.error = null
        }
        if (selectedImageUri == null) {
            Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
            // You might want to highlight the image selection area or show an error there
            isValid = false
        }

        return isValid
    }


    fun setAddItemListener(listener: AddItemListener) {
        this.listener = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AddItemBottomSheetDialog"
        fun newInstance(): AddItemBottomSheetDialogFragment {
            return AddItemBottomSheetDialogFragment()
        }
    }
}
