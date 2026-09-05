package com.example.facultyworkbench.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用自动更新工具。
 *
 * 使用方式：
 * 1. 在远程服务器托管 version.json（格式见 [VersionInfo]）。
 * 2. 修改 [UPDATE_JSON_URL] 指向你的 version.json。
 * 3. 调用 [checkForUpdate] 获取最新版本信息，若有更新则调用 [downloadAndInstall]。
 */
object UpdateChecker {

    /**
     * 远程 version.json 地址。请替换为你自己的服务器地址。
     * version.json 中的 downloadUrl 指向最新 APK 的下载地址。
     */
    const val UPDATE_JSON_URL = "https://your-server.com/version.json"

    /** 获取当前应用的 versionCode */
    fun getCurrentVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            0
        }
    }

    /** 获取当前应用的 versionName */
    fun getCurrentVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 检查是否有新版本。
     * @return 有新版本时返回 [VersionInfo]，否则返回 null；网络异常也返回 null。
     */
    suspend fun checkForUpdate(updateUrl: String = UPDATE_JSON_URL): VersionInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(updateUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            conn.connect()

            if (conn.responseCode != 200) return@withContext null

            val json = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            VersionInfo.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 下载 APK 并触发安装。
     * 使用系统 DownloadManager，下载完成后通过 FileProvider 唤起安装界面。
     *
     * @return 下载任务 ID，可用于查询状态；失败返回 -1。
     */
    suspend fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        versionName: String
    ): Long = withContext(Dispatchers.IO) {
        try {
            val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("$appName v$versionName")
                setDescription("正在下载更新…")
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    "facultyworkbench-v$versionName.apk"
                )
            }

            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val downloadId = dm.enqueue(request)

            // 轮询等待下载完成
            var apkFile: File? = null
            while (true) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(query)
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            val uriStr = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            apkFile = uriStr?.let { File(Uri.parse(it).path!!) }
                            cursor.close()
                            break
                        }
                        DownloadManager.STATUS_FAILED -> {
                            cursor.close()
                            return@withContext -1L
                        }
                    }
                }
                cursor.close()
                delay(1000)
            }

            apkFile?.let { installApk(context, it) }
            downloadId
        } catch (e: Exception) {
            -1L
        }
    }

    /** 通过 FileProvider 唤起系统安装界面 */
    private fun installApk(context: Context, apkFile: File) {
        val apkUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
