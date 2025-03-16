package com.example.sims

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Define request data class
data class PredictionRequest(
    val sales_data: Map<String, List<Int>>
)



// Define response data class
data class PredictionResponse(
    val predictions: Map<String, Int>
)


// API Interface
interface ApiService {
    @POST("predict")
    fun getPrediction(@Body request: PredictionRequest): Call<PredictionResponse>
}
