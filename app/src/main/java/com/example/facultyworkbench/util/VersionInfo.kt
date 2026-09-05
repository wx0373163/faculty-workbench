package com.example.facultyworkbench.util

import org.json.JSONObject

/**
 * 远程版本信息，由服务端 version.json 提供。
 *
 * version.json 示例：
 * {
 *   "versionCode": 3,
 *   "versionName": "1.2",
 *   "downloadUrl": "https://your-server.com/app-release.apk",
 *   "releaseNotes": "1. 新增 xxx 功能\n2. 修复 xxx 问题",
 *   "minSupportedVersion": 1
 * }
 */
data class VersionInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String,
    val releaseNotes: String = "",
    /** 当前版本低于此值时强制更新 */
    val minSupportedVersion: Int = 0
) {
    companion object {
        fun fromJson(json: String): VersionInfo? {
            return try {
                val obj = JSONObject(json)
                VersionInfo(
                    versionCode = obj.getInt("versionCode"),
                    versionName = obj.optString("versionName", ""),
                    downloadUrl = obj.getString("downloadUrl"),
                    releaseNotes = obj.optString("releaseNotes", ""),
                    minSupportedVersion = obj.optInt("minSupportedVersion", 0)
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
