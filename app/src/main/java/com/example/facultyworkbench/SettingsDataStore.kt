package com.example.facultyworkbench

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * 应用级设置 DataStore，供 Activity 与 BroadcastReceiver 共享访问。
 */
val Context.settingsDataStore by preferencesDataStore(name = "settings")
