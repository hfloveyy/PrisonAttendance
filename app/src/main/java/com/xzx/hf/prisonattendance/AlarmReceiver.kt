package com.xzx.hf.prisonattendance

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.xzx.hf.prisonattendance.utils.SharedPreferencesUtils

class AlarmReceiver : BroadcastReceiver(){
    private val preferences by lazy { SharedPreferencesUtils(MyApplication.getContext()) }
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.e("TestFuel","Num:"+preferences.num.toString())
        preferences.num = 0
        Log.e("TestFuel",preferences.num.toString())
    }
}