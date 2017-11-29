package com.ethereummaker.freeether.ethereummining

import android.content.Context
import android.net.ConnectivityManager
import android.provider.Settings
import java.io.File
import java.io.FileFilter
import java.util.regex.Pattern

object AppTools {

    fun isNetworkAvaliable(context: Context): Boolean {
        var connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        var info = connectivity.activeNetworkInfo

        return info != null && info.isConnectedOrConnecting
    }

    fun uniqueId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun coresCount(): Int {
        class CpuFilter : FileFilter {
            override fun accept(pathname: File): Boolean {
                return Pattern.matches("cpu[0-9]+", pathname.name)
            }
        }
        return try {
            val dir = File("/sys/devices/system/cpu/")
            val files = dir.listFiles(CpuFilter())
            files.size
        } catch (e: Exception) {
            1
        }

    }
}
