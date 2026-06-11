package com.example.data.net

import retrofit2.http.*
import retrofit2.Response

interface SupabaseApi {
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body request: SignUpRequest
    ): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Header("apikey") apiKey: String,
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(
        @Header("apikey") apiKey: String,
        @Body request: RecoverRequest
    ): Response<Unit>

    // REST Database interactions
    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Query("id") id: String
    ): Response<List<ProfileRow>>

    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") token: String,
        @Body profile: ProfileRow
    ): Response<Unit>
}

data class SignUpRequest(val email: String, val password: Any)
data class LoginRequest(val email: String, val password: Any)
data class RecoverRequest(val email: String)

data class UserToken(val id: String, val email: String)
data class AuthResponse(
    val access_token: String?,
    val token_type: String?,
    val expires_in: Int?,
    val user: UserToken?
)

data class ProfileRow(
    val id: String,
    val display_name: String,
    val phone: String,
    val bio: String,
    val profile_photo: String = "",
    val online_status: Boolean = false,
    val last_seen: Long = System.currentTimeMillis()
)
