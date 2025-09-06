package com.example.eatclub.network

import com.example.eatclub.data.FoodItemV2
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface FoodApiService {
    // Note: The structure of FoodItemV2 in Android should match
    // what the Python backend expects/returns for JSON.
    // The Python backend currently uses "_id" from MongoDB, but Android FoodItemV2 has "id: Long".
    // You'll need to decide how to map these or if the backend should also return "id".
    // For now, let's assume the Python backend is modified to accept/return "id" as well,
    // or your Android FoodItemV2 can handle "_id" (e.g., by adding @SerializedName("_id") String? mongoId)

    @GET("fooditems")
    fun getFoodItems(): Call<List<FoodItemV2>> // Expects a list of FoodItemV2

    @POST("fooditems")
    fun addFoodItem(@Body foodItem: FoodItemV2): Call<FoodItemV2> // Send FoodItemV2, expect FoodItemV2 back

    @GET("fooditems/{id}") // Assuming your backend can handle your FoodItemV2 'id' if not MongoDB's _id
    fun getFoodItemById(@Path("id") itemId: String): Call<FoodItemV2>
    // If using MongoDB's _id directly:
    // fun getFoodItemById(@Path("id") itemId: String): Call<FoodItemV2> and pass MongoDB's _id as string

    @PUT("fooditems/{id}")
    fun updateFoodItem(@Path("id") itemId: String, @Body foodItem: FoodItemV2): Call<FoodItemV2>

    @DELETE("fooditems/{id}")
    fun deleteFoodItem(@Path("id") itemId: String): Call<Void> // Or a custom response
}