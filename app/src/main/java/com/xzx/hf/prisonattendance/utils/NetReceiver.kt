package com.xzx.hf.prisonattendance.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import com.xzx.hf.prisonattendance.MyApplication

class NetReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {

        if (context != null) {
            val app = context.applicationContext as MyApplication
            val mConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.getActiveNetworkInfo()
            if (mNetworkInfo != null) {
                val logs = DBUtil.getCallLog(app.netUnavailableTime)
                Log.e("DB",logs.toString())
                for (log in logs){
                    HttpApi.postDataSync(log,"/BatchUploadRecord")
                }
                //Toast.makeText(context, "网络可用", Toast.LENGTH_LONG).show()
            }else{

                app.netUnavailableTime = System.currentTimeMillis()
                Toast.makeText(context, "网络不可用", Toast.LENGTH_LONG).show()
            }
        }
    }
}