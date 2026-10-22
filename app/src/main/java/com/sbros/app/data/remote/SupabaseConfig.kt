package com.sbros.app.data.remote

object SupabaseConfig {
    /**
     * URL вашего проекта Supabase.
     * Замените на URL вашего реального проекта, созданного на supabase.com.
     * Например: "https://xyzcompany.supabase.co"
     */
    const val DEFAULT_URL: String = "https://your-project-id.supabase.co"

    /**
     * Публичный анонимный ключ (anon / public key) из настроек проекта Supabase -> Project Settings -> API.
     */
    const val DEFAULT_ANON_KEY: String = "sbp_demo_placeholder_anon_key"
}
