package com.dangerfield.movingeyes.libraries.ui

interface PhotoSaver {
    suspend fun savePhoto(photoData: ByteArray): String?
}
