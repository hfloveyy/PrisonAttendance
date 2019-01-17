package com.xzx.hf.prisonattendance

import android.Manifest
import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View

import android.widget.Toast
import com.baidu.aip.db.DBManager
import com.baidu.aip.manager.FaceSDKManager
import com.baidu.aip.utils.PreferencesUtil
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Request
//import com.github.kittinunf.fuel.core.Request.Companion.stringDeserializer
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.xzx.hf.prisonattendance.baidu.LivenessSettingActivity
import com.xzx.hf.prisonattendance.baidu.UserGroupManagerActivity
import kotlinx.android.synthetic.main.activity_main.*

import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum
import com.nightonke.boommenu.BoomButtons.OnBMClickListener
import com.nightonke.boommenu.Piece.PiecePlaceEnum
import com.nightonke.boommenu.ButtonEnum
import com.xzx.hf.prisonattendance.baidu.RegActivity
import com.xzx.hf.prisonattendance.utils.SharedPreferencesUtils
import org.litepal.LitePal
import com.github.kittinunf.result.Result
import com.xzx.hf.prisonattendance.utils.HttpApi

import com.github.kittinunf.fuel.coroutines.*
import com.xzx.hf.prisonattendance.netty.NettyService
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import android.app.ActivityManager
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.view.KeyEvent
import android.view.Window
import com.xzx.hf.prisonattendance.entity.UserPlus
import com.xzx.hf.prisonattendance.netty.NettyListener
import com.xzx.hf.prisonattendance.utils.DBUtil
import org.jetbrains.anko.*


class MainActivity : AppCompatActivity() , OnBMClickListener{
    private val preferences by lazy { SharedPreferencesUtils(this) }
    private var appno:String = ""
    private var area:String = ""
    private var all_count:String = "0"
    private var netReceiver:NetReceiver = NetReceiver()
    private lateinit var app: MyApplication
    private lateinit var dialog: ProgressDialog
    private lateinit var dialog2: ProgressDialog
    private var notFirst = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().requestFeature(Window.FEATURE_LEFT_ICON)
        setContentView(R.layout.activity_main)
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.bat)
        app  = application as MyApplication
        initPermisson()
        initBoomMenu()
        init()
        val mFilter = IntentFilter()
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(netReceiver, mFilter)
    }


    val handler = object:Handler(Looper.getMainLooper()){
        override fun handleMessage(msg:Message){
            super.handleMessage(msg)
            try {
                when(msg?.what){
                    1 ->{
                        Log.e("TestFuel","msg:"+msg.obj.toString())
                        //Toast.makeText(applicationContext,"子线程消息",Toast.LENGTH_LONG).show()
                        val obj = msg.obj as JSONObject
                        if (obj != null) {
                            runOnUiThread {
                                val areaZH = when(preferences.area){
                                    "230001009" -> "九监区"
                                    else ->""
                                }
                                area_tv.text = "监区:" + areaZH
                                preferences.all_count = obj.getString("AllCount")
                                all_count_tv.text = "在押总数:" + preferences.all_count
                                all_count = obj.getString("AllCount")
                                preferences.actual_num = obj.getString("ActualNumber")
                                actual_num_tv.text = "工作区现场总数:" + preferences.actual_num
                                preferences.lack_num = obj.getString("LackNumber")
                                lack_num_tv.text = "离开工作区人数:" + preferences.lack_num
                            }

                        }
                    }
                    0 ->{
                        runOnUiThread {
                            val areaZH = when(preferences.area){
                                "230001009" -> "九监区"
                                else ->""
                            }
                            area_tv.text = "监区:" + areaZH
                            preferences.all_count = "0"
                            all_count_tv.text = "在押总数:" + preferences.all_count
                            preferences.actual_num = "0"
                            actual_num_tv.text = "工作区现场总数:" + preferences.actual_num
                            preferences.lack_num = "0"
                            lack_num_tv.text = "离开工作区人数:" + preferences.lack_num
                        }
                    }
                    2 ->{
                        Log.e("TestFuel",msg.obj.toString())
                        val d = msg.obj as ProgressDialog
                        d.dismiss()
                    }
                }
            }catch (e:Exception){
                Log.e("TestFuel",e.toString())
            }




        }
    }



    //主界面显示
    fun syncStatus(){
        try {
            syncDB()
            Log.e("TestFuel","SyncStatus")
            val areaZH = when(preferences.area){
                "230001009" -> "九监区"
                else ->""
            }
            area_tv.text = "监区:" + areaZH

            all_count_tv.text = "在押总数:" + preferences.all_count

            actual_num_tv.text = "工作区现场总数:" + preferences.actual_num

            lack_num_tv.text = "离开工作区人数:" + preferences.lack_num
            Thread(object : Runnable{
                override fun run() {
                    val message = handler.obtainMessage()
                    try {

                        val obj = HttpApi.getCount(preferences.area!!)
                        //Log.e("TestFuel",obj.toString())
                        message.what = 1
                        message.obj = obj
                        handler.sendMessage(message)
                    } catch (e: Exception) {
                        message.what = 0
                        handler.sendMessage(message)
                    }


                }
            }).start()
        }catch (e:Exception){
            Log.e("TestFuel",e.toString())
        }


    }
    //离线更新界面
    fun syncStatusOffLine(){
        try {
            Log.e("TestFuel","SyncStatus")
            val areaZH = when(preferences.area){
                "230001009" -> "九监区"
                else ->""
            }

            all_count = LitePal.where("usertype = 1 and area = ?",preferences.area).find(UserPlus::class.java).size.toString()

            val actual_num = LitePal.where("userworkstatus = 1 and usertype = 1 and area = ?",preferences.area).find(UserPlus::class.java).size

            val lack_num = LitePal.where("userworkstatus = 3 and usertype = 1 and area = ?",preferences.area).find(UserPlus::class.java).size

            area_tv.text = "监区:" + areaZH

            all_count_tv.text = "在押总数:" + all_count

            actual_num_tv.text = "工作区现场总数:" + actual_num.toString()//preferences.actual_num

            lack_num_tv.text = "离开工作区人数:" + lack_num.toString()//preferences.lack_num
        }catch (e:Exception){
            Log.e("TestFuel",e.toString())
        }


    }
    //申请权限
    fun initPermisson(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE     //写权限
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this, Manifest
                    .permission.CAMERA  //相机权限
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                100
            )
            return
        }
        /*
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.setData(Uri.parse("package:" + this.getPackageName()))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        }*/
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if(event!!.keyCode == KeyEvent.KEYCODE_BACK ) {
            //do something.
            //toast("点名期间返回键被禁用")
            return true
        }else {
            return super.dispatchKeyEvent(event)
        }
    }

    fun init(){
        /*
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager
                .PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
            return
        }*/


        //初始化groupId
        preferences.groupId = "1"
        appno = preferences.appno!!
        area = preferences.area!!
        app.detectStatus = MyApplication.DETECT_IDEL

        //初始化Fuel BaseURL
        FuelManager.instance.basePath = "http://"+ preferences.serverIP+":8080/api/prison"//"https://httpbin.org"
        //使用1：n人脸对比
        PreferencesUtil.initPrefs(this)
        //RGB单目活体，仅使用RGB活体（单目活体），可有效防止照片翻拍，屏幕等攻击
        //显示当前活体检测设置
        //livnessTypeTip()
        //初始化DB
        PreferencesUtil.putInt("TYPE_LIVENSS", 1)//2
        DBManager.getInstance().init(this)
        Log.e("TestFuel","FaceSDKStatus:"+FaceSDKManager.initStatus.toString() )



        val serviceClassName = NettyService::class.java
        val intent = Intent(applicationContext, serviceClassName)
        //startService(intent)
        if (FaceSDKManager.initStatus != FaceSDKManager.SDK_INITED){
            /*
            val mConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.getActiveNetworkInfo()
            if (mNetworkInfo == null) {
                mytoast("网络不可用,无法初始化,请到有网络的位置重启应用！！")
                //android.os.Process.killProcess(android.os.Process.myPid())    //获取PID
                //System.exit(0)
                finish()
            }*/
            dialog = indeterminateProgressDialog("正在登陆点名系统", "请稍候")
            dialog.show()
            dialog.setCancelable(false)

            Log.e("Test","init")
            FaceSDKManager.getInstance().init(this)

            FaceSDKManager.getInstance().setSdkInitListener(object : FaceSDKManager.SdkInitListener {
                override fun initStart() {
                    //mytoast("SDK开始初始化！")

                }

                override fun initSuccess() {
                    dialog.dismiss()
                    Log.e("Test","success")
                    //mytoast("SDK初始化成功！")
                }

                override fun initFail(errorCode: Int, msg: String) {
                    //mytoast("sdk init fail:$msg")
                }
            })
        }
        if (!isServiceRunning(serviceClassName)) {
            // Start the service
            startService(intent)
        } else {
            //初始化sdk

            //toast("Service already running.")
        }
        appno_tv.text = "设备编号:"+preferences.appno
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onRestart() {
        super.onRestart()
        Log.e("TestFuel","返回Restart")
        //app.detectStatus = MyApplication.DETECT_IDEL
        //syscStatus()
        //syncDB()
    }

    override fun onResume() {
        super.onResume()
        Log.e("TestFuel","返回Resume")
        app.detectStatus = MyApplication.DETECT_IDEL
        notFirst = true
        try {
            val mConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.getActiveNetworkInfo()
            if (mNetworkInfo != null) {
                syncStatus()
                //handler.post(Runnable {  })
            }else{
                mytoast("网络不可用,无法更新状态！")
            }
        }catch (e:Exception){
            Log.e("TestFuel",e.toString())
        }

    }



    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        // Loop through the running services
        for (service in activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                // If the service is running then return true
                return true
            }
        }
        return false
    }

    fun syncDB(){
        val dialog_temp = indeterminateProgressDialog("正在更新数据库！", "请稍候！")
        dialog_temp.show()
        dialog_temp.setCancelable(false)
        try{
            Thread(object : Runnable{
                override fun run() {

                    val message = handler.obtainMessage()
                    try {

                        val value = HttpApi.syncDB()
                        Log.e("TestFuel","消除dialog")
                        message.what = 2
                        message.obj = dialog_temp
                        handler.sendMessage(message)


                    }catch (e:Exception){
                        message.what = 2
                        handler.sendMessage(message)
                        message.obj = dialog_temp
                        Log.e("DBSync",e.toString())
                    }
                }
            }).start()
        }catch (e:Exception){
            Log.e("TestFuel",e.toString())
        }




    }



    fun initBoomMenu(){
        bmb!!.buttonRadius = 90 //调整按钮尺寸
        bmb!!.setButtonEnum(ButtonEnum.Ham)
        bmb!!.setPiecePlaceEnum(PiecePlaceEnum.HAM_4)
        bmb!!.setButtonPlaceEnum(ButtonPlaceEnum.HAM_4)
        var builder = BuilderManager.getHamButtonBuilder(R.string.time,R.string.time_sub)
        bmb!!.addBuilder(builder)
        var builder2 = BuilderManager.getHamButtonBuilder(R.string.random,R.string.random_sub)
        bmb!!.addBuilder(builder2)
        var builder3 = BuilderManager.getHamButtonBuilder(R.string.out,R.string.out_sub)
        bmb!!.addBuilder(builder3)
        var builder4 = BuilderManager.getHamButtonBuilder(R.string.back,R.string.back_sub)
        bmb!!.addBuilder(builder4)

        builder.listener(this)
        builder2.listener(this)
        builder3.listener(this)
        builder4.listener(this)


        bmb2!!.buttonRadius = 90
        bmb2!!.setButtonEnum(ButtonEnum.Ham)
        bmb2!!.setPiecePlaceEnum(PiecePlaceEnum.HAM_3)
        bmb2!!.setButtonPlaceEnum(ButtonPlaceEnum.HAM_3)
        var builder5 = BuilderManager.getHamButtonBuilder(R.string.register,R.string.register)
        bmb2!!.addBuilder(builder5)
        var builder6 = BuilderManager.getHamButtonBuilder(R.string.setting,R.string.setting)
        bmb2!!.addBuilder(builder6)
        var builder7 = BuilderManager.getHamButtonBuilder(R.string.sync_db,R.string.sync_db)
        bmb2!!.addBuilder(builder7)
        //var builder8 = BuilderManager.getHamButtonBuilder("保留按钮","保留按钮")
        //bmb2!!.addBuilder(builder8)
        builder5.listener{
            //startActivity<UserGroupManagerActivity>()
            //startActivity<RegActivity>()
            startActivity<UpdateListActivity>()
        }
        builder6.listener{
            //mytoast("设置Activity")
            startActivity<SettingActivity>()
        }
        builder7.listener{
            mytoast("同步数据库")
            handler.post(Runnable { syncStatus() })

        }

    }



    override fun onBoomButtonClick(index: Int){
        //val taskName = area + index.toString() + appno
        //app.detectStatus = MyApplication.DETECT_RUNNING
        //app.taskName = taskName
        Log.e("PA", "index: " + index)
        startActivity<VideoIdentityActivity>("callnameType" to (index+1).toString(),
            "cmdFrom" to "main","all_count" to all_count
        )
    }
    private fun mytoast(text: String) {
        handler.post(Runnable { Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show() })
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(netReceiver)
    }

    private fun livnessTypeTip() {
        val type = PreferencesUtil.getInt(
            LivenessSettingActivity.TYPE_LIVENSS, LivenessSettingActivity
                .TYPE_NO_LIVENSS
        )

        if (type == LivenessSettingActivity.TYPE_NO_LIVENSS) {
            Toast.makeText(this, "当前活体策略：无活体", Toast.LENGTH_LONG).show()
        } else if (type == LivenessSettingActivity.TYPE_RGB_LIVENSS) {
            Toast.makeText(this, "当前活体策略：单目RGB活体", Toast.LENGTH_LONG).show()
        } else if (type == LivenessSettingActivity.TYPE_RGB_IR_LIVENSS) {
            Toast.makeText(
                this, "当前活体策略：双目RGB+IR活体, 请选用RGB+IR摄像头",
                Toast.LENGTH_LONG
            ).show()
        } else if (type == LivenessSettingActivity.TYPE_RGB_DEPTH_LIVENSS) {
            Toast.makeText(this, "当前活体策略：双目RGB+Depth活体，请选用RGB+Depth摄像头", Toast.LENGTH_LONG).show()
        }
    }

    inner class NetReceiver : BroadcastReceiver(){
        //private val preferences by lazy { SharedPreferencesUtils(MyApplication.getContext()) }



        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (context != null) {
                    val app = context.applicationContext as MyApplication
                    val mConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val mNetworkInfo = mConnectivityManager.getActiveNetworkInfo()
                    if (mNetworkInfo != null && notFirst) {
                        Log.e("TestFuel","更新！")
                        syncStatus()
                        status_tv.text = "设备状态:在线"
                        //preferences.netUnavailableTime = System.currentTimeMillis()
                        //Toast.makeText(context, "网络可用", Toast.LENGTH_LONG).show()
                    }else{
                        status_tv.text = "设备状态:离线"
                        syncStatusOffLine()
                        Log.e("TestFuel","网络不可用")
                        //preferences.netUnavailableTime = System.currentTimeMillis()
                        Toast.makeText(context, "网络不可用", Toast.LENGTH_LONG).show()
                    }
                }
            }catch (e:Exception){
                Log.e("TestFuel",e.toString())
            }

        }

    }

}
