package com.saurabh.skipad.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.Toast
import com.saurabh.skipad.model.InstalledAppGeneral
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class ToolBox @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun getInstalledApps(): List<InstalledAppGeneral> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val ownPackage = context.packageName

        return apps
            .asSequence()
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .filter { it.packageName != ownPackage }
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map {
                InstalledAppGeneral(
                    appName = pm.getApplicationLabel(it).toString(),
                    packageName = it.packageName,
                    icon = pm.getApplicationIcon(it)
                )
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }


    fun openApp(packageName: String) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let {
            context.startActivity(it)
        } ?: Toast.makeText(context, "Cannot open app", Toast.LENGTH_SHORT).show()
    }
}