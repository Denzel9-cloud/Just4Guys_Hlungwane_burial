package com.just4guys.hlungwaneburial.data.remote

import com.just4guys.hlungwaneburial.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Singleton Supabase client for the Hlungwane Burial app.
 *
 * Credentials come from `local.properties` → exposed via BuildConfig.
 * Never hardcode these values in source.
 */
object Supabase {
    fun configurationError(): String? {
        val url = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
        val key = BuildConfig.SUPABASE_ANON_KEY.trim()
        return when {
            url.isBlank() || key.isBlank() -> "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to local.properties, then rebuild the app."
            !url.startsWith("https://") || url.contains("localhost", ignoreCase = true) || !url.endsWith(".supabase.co") -> "Invalid Supabase URL. Use https://YOUR-PROJECT.supabase.co in local.properties, then rebuild."
            else -> null
        }
    }

    val client: SupabaseClient by lazy {
        require(configurationError() == null) { configurationError() ?: "Invalid Supabase configuration" }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                scheme = "hlungwaneburial"
                host = "auth"
            }
            install(Postgrest)
        }
    }
}