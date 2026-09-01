package com.dangerfield.movingeyes.libraries.core

import com.dangerfield.movingeyes.buildinfo.MovingEyesBuildConfig

object SupabaseInfo {
    val projectId: String
        get() = MovingEyesBuildConfig.SUPABASE_PROJECT_ID

    val url: String
        get() = MovingEyesBuildConfig.SUPABASE_URL

    val anonKey: String
        get() = MovingEyesBuildConfig.SUPABASE_ANON_KEY
}
