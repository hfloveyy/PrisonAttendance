package com.xzx.hf.prisonattendance.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class SharedPreferencesUtils(context: Context) {

    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    var groupId by SharedPreferenceDelegates.string(defaultValue = "1")

    var serverIP by SharedPreferenceDelegates.string(defaultValue = "47.92.2.211")

    var port by SharedPreferenceDelegates.string(defaultValue = "5660")

    var appno by SharedPreferenceDelegates.string(defaultValue = "101")

    var area by SharedPreferenceDelegates.string(defaultValue = "230001009")

    var all_count by SharedPreferenceDelegates.string(defaultValue = "0")

    var actual_num by SharedPreferenceDelegates.string(defaultValue = "0")

    var lack_num by SharedPreferenceDelegates.string(defaultValue = "0")

    var netUnavailableTime by SharedPreferenceDelegates.long(System.currentTimeMillis())

    var currentDateTime: Long by SharedPreferenceDelegates.long(System.currentTimeMillis())

    var num :Long by SharedPreferenceDelegates.long(defaultValue = 0)


    private object SharedPreferenceDelegates {

        fun int(defaultValue: Int = 0) = object : ReadWriteProperty<SharedPreferencesUtils, Int> {

            override fun getValue(thisRef: SharedPreferencesUtils, property: KProperty<*>): Int {
                return thisRef.preferences.getInt(property.name, defaultValue)
            }

            override fun setValue(thisRef: SharedPreferencesUtils, property: KProperty<*>, value: Int) {
                thisRef.preferences.edit().putInt(property.name, value).apply()
            }
        }

        fun long(defaultValue: Long = 0L) = object : ReadWriteProperty<SharedPreferencesUtils, Long> {

            override fun getValue(thisRef: SharedPreferencesUtils, property: KProperty<*>): Long {
                return thisRef.preferences.getLong(property.name, defaultValue)
            }

            override fun setValue(thisRef: SharedPreferencesUtils, property: KProperty<*>, value: Long) {
                thisRef.preferences.edit().putLong(property.name, value).apply()
            }
        }

        fun boolean(defaultValue: Boolean = false) = object : ReadWriteProperty<SharedPreferencesUtils, Boolean> {
            override fun getValue(thisRef: SharedPreferencesUtils, property: KProperty<*>): Boolean {
                return thisRef.preferences.getBoolean(property.name, defaultValue)
            }

            override fun setValue(thisRef: SharedPreferencesUtils, property: KProperty<*>, value: Boolean) {
                thisRef.preferences.edit().putBoolean(property.name, value).apply()
            }
        }

        fun float(defaultValue: Float = 0.0f) = object : ReadWriteProperty<SharedPreferencesUtils, Float> {
            override fun getValue(thisRef: SharedPreferencesUtils, property: KProperty<*>): Float {
                return thisRef.preferences.getFloat(property.name, defaultValue)
            }

            override fun setValue(thisRef: SharedPreferencesUtils, property: KProperty<*>, value: Float) {
                thisRef.preferences.edit().putFloat(property.name, value).apply()
            }
        }

        fun string(defaultValue: String? = null) = object : ReadWriteProperty<SharedPreferencesUtils, String?> {
            override fun getValue(thisRef: SharedPreferencesUtils, property: KProperty<*>): String? {
                return thisRef.preferences.getString(property.name, defaultValue)
            }

            override fun setValue(thisRef: SharedPreferencesUtils, property: KProperty<*>, value: String?) {
                thisRef.preferences.edit().putString(property.name, value).apply()
            }
        }

        fun setString(defaultValue: Set<String>? = null) = object : ReadWriteProperty<SharedPreferencesUtils, Set<String>?> {
            override fun getValue(thisRef: SharedPreferencesUtils, property: KProperty<*>): Set<String>? {
                return thisRef.preferences.getStringSet(property.name, defaultValue)
            }

            override fun setValue(thisRef: SharedPreferencesUtils, property: KProperty<*>, value: Set<String>?) {
                thisRef.preferences.edit().putStringSet(property.name, value).apply()
            }
        }
    }
}
