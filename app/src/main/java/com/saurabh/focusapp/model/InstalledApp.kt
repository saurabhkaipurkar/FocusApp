package com.saurabh.focusapp.model

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Immutable

@Immutable
data class InstalledAppGeneral(
    val serialNo: Int = 0,
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val isSelected: Boolean = false
)

@Immutable
data class InstalledApp(
    val serialNo: Int = 0,
    val appName: String,
    val packageName: String,
    val icon: ByteArray?,
    val isSelected: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InstalledApp

        if (serialNo != other.serialNo) return false
        if (isSelected != other.isSelected) return false
        if (appName != other.appName) return false
        if (packageName != other.packageName) return false
        if (!icon.contentEquals(other.icon)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serialNo
        result = 31 * result + isSelected.hashCode()
        result = 31 * result + appName.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + (icon?.contentHashCode() ?: 0)
        return result
    }
}