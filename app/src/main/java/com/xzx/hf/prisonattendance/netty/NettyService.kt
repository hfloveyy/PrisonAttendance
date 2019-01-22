package com.xzx.hf.prisonattendance.netty

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.xzx.hf.prisonattendance.utils.SharedPreferencesUtils

import org.jetbrains.anko.doAsync

import org.json.JSONException
import org.json.JSONObject
import java.io.Serializable
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.PowerManager
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import com.xzx.hf.prisonattendance.*
import com.xzx.hf.prisonattendance.utils.DBUtil
import com.xzx.hf.prisonattendance.utils.HttpApi
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import java.util.*


class NettyService : Service(),NettyListener{
    private val preferences by lazy { SharedPreferencesUtils(this) }
    private lateinit var nettyClient:NettyClient
    private lateinit var app:MyApplication
    private var groupId:String? = ""
    private var area:String? = ""

    lateinit var mKeyguardManager:KeyguardManager
    // 键盘锁
    private  lateinit var mKeyguardLock: KeyguardManager.KeyguardLock
    // 电源管理器
    private  lateinit var mPowerManager:PowerManager
    // 唤醒锁
    private  lateinit var mWakeLock:PowerManager.WakeLock

    private var netReceiver:NetReceiver = NetReceiver()


    override fun onMessageResponse(msg: Any) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        //Log.e("NettyService", "onMessageResponse:" + msg)
        //Log.e("Netty","detectStatus: ${app.detectStatus}")
        handleMsg(msg)

    }
    fun handleMsg(msg: Any){
        try {
            val jsonObject = JSONObject(msg.toString())
            val cmd = jsonObject.getString("cmd")
            if (cmd == "nettest") return
            Log.e("NettyService", "onMessageResponse:" + msg)
            val localarea = jsonObject.getString("area")
            if(localarea!=area) {
                Log.e("Netty","监区不匹配！")
                return
            }

            val callnameType = jsonObject.getString("callnametype")
            val appno = jsonObject.getString("appno")
            val taskName = jsonObject.getString("taskname")
            when(cmd){
                "start-callname" ->{
                    wakeUp()
                    if (appno == preferences.appno)
                        return
                    if (app.detectStatus == MyApplication.DETECT_RUNNING)
                        return
                    app.taskName = taskName
                    app.detectStatus = MyApplication.DETECT_RUNNING
                    val policeId = jsonObject.getString("peopleid")
                    val policeName= jsonObject.getString("userinfo")
                    Log.e("Netty",policeName)
                    val intent = Intent()
                    intent.setClass(app,VideoIdentityActivity::class.java)
                    intent.putExtra("callnameType",callnameType)
                    intent.putExtra("taskname",taskName)
                    intent.putExtra("taskappno",appno)
                    intent.putExtra("cmdFrom","other")
                    intent.putExtra("policeId",policeId)
                    intent.putExtra("policeName",policeName)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                        /*
                        startActivity<VideoIdentityActivity>("callnameType" to callnameType,
                        "taskname" to {app.taskName},"cmdFrom" to "other","peopleId" to policeId,"userinfo" to userinfo
                        )*/


                }
                "end-callname" ->{
                    Log.e("Netty",jsonObject.toString())
                    if(taskName == app.taskName) {
                        if (app.detectStatus == MyApplication.DETECT_RUNNING) {
                            app.detectStatus = MyApplication.DETECT_IDEL
                            app.taskName = ""
                        }
                        val policeId = jsonObject.getString("peopleid")
                        val policeName= jsonObject.getString("userinfo")
                        val notCount = jsonObject.getJSONArray("crimialslist")
                        val callnameType = jsonObject.getString("callnametype")
                        //val total = jsonObject.getString("total")
                        val appno = jsonObject.getString("appno")
                        Log.e("TestFuel",notCount.toString())
                        val countCriminals = mutableListOf<String>()
                        if (notCount != null) {
                            val len = notCount.length()
                            for (i in 0 until len) {
                                countCriminals.add(notCount.get(i).toString())
                            }
                        }
                        val intent = Intent()
                        intent.setClass(app,DetectResultActivity::class.java)
                        intent.putExtra("msg",msg.toString())
                        intent.putExtra("callnametype",callnameType)
                        //intent.putExtra("total",total)
                        intent.putExtra("area",area)
                        intent.putExtra("taskName",taskName)
                        intent.putExtra("crimialslist",countCriminals as Serializable)
                        intent.putExtra("policeId",policeId)
                        intent.putExtra("policeName",policeName)
                        intent.putExtra("appno",appno)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    }

                }
                "callname-success" ->{
                    //intentReceiver.putExtra("msg",msg.toString())
                    //sendBroadcast(intentReceiver)
                }

            }

        } catch (e: JSONException) {
            e.printStackTrace()
            Log.e("Netty","$e")
        }

    }

    override fun onServiceStatusConnectChanged(statusCode: Int) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        doAsync {
            if (statusCode == NettyListener.STATUS_CONNECT_SUCCESS) {
                Log.e("Netty", "STATUS_CONNECT_SUCCESS:")
                val logs = DBUtil.getCallLog(preferences.netUnavailableTime)

                Log.e("Netty","${preferences.netUnavailableTime}: "+logs.toString())
                for (log in logs){
                    try {
                        val jsonObject = JSONObject(log)
                        val cmd = jsonObject.getString("cmd")
                        if (cmd != "start-callname") {
                            Log.e("Netty","log:"+log)
                            sendMsg(log)
                        }
                    }catch (e:Exception){
                        Log.e("Netty",e.toString())
                    }

                }
                preferences.netUnavailableTime = System.currentTimeMillis()
            } else {
                Log.e("Netty", "onServiceStatusConnectChanged:" + statusCode)
            }
            /*
            uiThread {
                toast("Service Status Changed！")
            }*/
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(netReceiver)
        nettyClient.disconnect()
        if (mWakeLock != null) {
            Log.e("Netty","----> 终止服务,释放唤醒锁")
            try {
                mWakeLock.release()
            }catch (e:Exception){
                Log.e("Netty",e.toString())
            }

        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        app  = application as MyApplication

        initNetty()

        mPowerManager =  getSystemService(Context.POWER_SERVICE) as PowerManager
        mKeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        mWakeLock = mPowerManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.SCREEN_DIM_WAKE_LOCK, "NettyService")

        return super.onStartCommand(intent, flags, startId)

    }

    override fun onCreate() {
        super.onCreate()
        Log.e("Netty","NettyService start")
        val mFilter = IntentFilter()
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(netReceiver, mFilter)

        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, 0)
        val notification = NotificationCompat.Builder(this)
            .setContentTitle("服务运行中...")
            //.setContentText("This is content text")
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setContentIntent(pi)
            .build()
        startForeground(1, notification)

        alarm()




    }

    fun alarm(){
        Log.e("TestFuel","Alarm")
        val manager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MINUTE, 10)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.set(Calendar.HOUR_OF_DAY, 1)

        val interval = 1000L * 60L * 60L * 24//一个小时的间隔
        val triggerAtTime = System.currentTimeMillis()
        val i = Intent(this, AlarmReceiver::class.java)

        val pii= PendingIntent.getBroadcast(this, 0, i, 0)
        manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), interval, pii)
    }



    fun wakeUp(){
        mWakeLock.acquire()
    }
    fun initNetty(){
        groupId = preferences.groupId
        area = preferences.area
        val host = preferences.serverIP
        val port = preferences.port

        nettyClient = NettyClient(host!!,port!!.toInt())
        connect()
    }
    private fun connect() {
        Log.d("Netty", "connect")
        if (!nettyClient.connectStatus) {
            nettyClient.setListener(this)
            nettyClient.connect()//连接服务器
        } else {
            nettyClient.disconnect()
        }
    }
    override fun onBind(intent: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun sendMsg(msg:String){
        Thread(object : Runnable{
            override fun run() {
                try {

                    Log.e("Netty","NetRecevier:"+msg)
                    nettyClient.sendMsgToServer(msg,object : ChannelFutureListener {
                        override fun operationComplete(channelFuture: ChannelFuture) {
                            if (channelFuture.isSuccess()) {                //4
                                Log.e("Netty", "Write auth successful")
                            } else {
                                Log.e("Netty", "Write auth error")
                            }
                        }
                    })

                } catch (e: Exception) {
                    Log.d("", e.message)
                }
            }
        }).start()


    }

    inner class NetReceiver : BroadcastReceiver(){
        //private val preferences by lazy { SharedPreferencesUtils(MyApplication.getContext()) }



        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (context != null) {
                    val app = context.applicationContext as MyApplication
                    val mConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val mNetworkInfo = mConnectivityManager.getActiveNetworkInfo()
                    if (mNetworkInfo != null) {
                        val logs = DBUtil.getCallLog(preferences.netUnavailableTime)

                        Log.e("TestFuel","${preferences.netUnavailableTime}: "+logs.toString())
                        for (log in logs){
                            val jsonObject = JSONObject(log)
                            val cmd = jsonObject.getString("cmd")
                            if (cmd == "end-callname") {
                                Log.e("Netty","log:"+log)
                                HttpApi.postDataSync(log,"/BatchUploadRecord")
                            }else if (cmd == "save") {
                                Log.e("Netty","log:"+log)
                                HttpApi.postDataSync(log,"/UpdateUserStatus")
                            }
                        }
                        //preferences.netUnavailableTime = System.currentTimeMillis()
                        Log.e("TestFuel","恢复网络时间："+System.currentTimeMillis())
                        //Toast.makeText(context, "网络可用", Toast.LENGTH_LONG).show()
                    }else{

                        preferences.netUnavailableTime = System.currentTimeMillis()
                        Log.e("Netty","断网时间："+preferences.netUnavailableTime.toString())
                    }
                }
            }catch (e:Exception){
                Log.e("TestFuel",e.toString())
            }

        }

    }


}