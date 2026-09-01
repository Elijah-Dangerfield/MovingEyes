package com.dangerfield.movingeyes.libraries.config.impl.data

import com.dangerfield.movingeyes.libraries.config.AppConfigMap
import com.dangerfield.movingeyes.libraries.core.Catching

interface RemoteConfigDataSource {
    suspend fun getConfig(): Catching<AppConfigMap>
}
