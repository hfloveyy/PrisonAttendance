package com.xzx.hf.prisonattendance

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast

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
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.progressDialog


class MainActivity : AppCompatActivity() , OnBMClickListener{
    private val preferences by lazy { SharedPreferencesUtils(this) }
    private var appno:String = ""
    private var area:String = ""
    private lateinit var app: MyApplication
    private lateinit var dialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        app  = application as MyApplication
        init()
        initBoomMenu()


        //syncDB()

    }





    fun init(){

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager
                .PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 100)
            return
        }


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
        PreferencesUtil.putInt("TYPE_LIVENSS", 1)//2
        //显示当前活体检测设置
        //livnessTypeTip()
        //初始化DB
        DBManager.getInstance().init(this)
        Log.e("Test",FaceSDKManager.initStatus.toString() )
        //初始化SDK
        if (FaceSDKManager.initStatus != FaceSDKManager.SDK_INITED){
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

        val serviceClassName = NettyService::class.java
        val intent = Intent(applicationContext, serviceClassName)
        //startService(intent)

        if (!isServiceRunning(serviceClassName)) {
            // Start the service

            startService(intent)
        } else {
            //toast("Service already running.")
        }

    }

    override fun onResume() {
        super.onResume()
        app.detectStatus = MyApplication.DETECT_IDEL
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

        Thread(object : Runnable{
            override fun run() {
                try {
                    HttpApi.syncDB()
                }catch (e:Exception){
                    Log.e("DBSync",e.toString())
                }
            }
        }).start()



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
        bmb2!!.setPiecePlaceEnum(PiecePlaceEnum.HAM_4)
        bmb2!!.setButtonPlaceEnum(ButtonPlaceEnum.HAM_4)
        var builder5 = BuilderManager.getHamButtonBuilder(R.string.register,R.string.register)
        bmb2!!.addBuilder(builder5)
        var builder6 = BuilderManager.getHamButtonBuilder(R.string.setting,R.string.setting)
        bmb2!!.addBuilder(builder6)
        var builder7 = BuilderManager.getHamButtonBuilder(R.string.sync_db,R.string.sync_db)
        bmb2!!.addBuilder(builder7)
        var builder8 = BuilderManager.getHamButtonBuilder("保留按钮","保留按钮")
        bmb2!!.addBuilder(builder8)
        builder5.listener{
            //startActivity<UserGroupManagerActivity>()
            startActivity<RegActivity>()
        }
        builder6.listener{
            //mytoast("设置Activity")
            startActivity<SettingActivity>()
        }
        builder7.listener{
            mytoast("同步数据库")
            handler.post(Runnable { syncDB() })

        }
        builder8.listener{
            //mytoast("其他用途")


        }
    }



    override fun onBoomButtonClick(index: Int){
        //val taskName = area + index.toString() + appno
        //app.detectStatus = MyApplication.DETECT_RUNNING
        //app.taskName = taskName
        Log.e("PA", "index: " + index)
        startActivity<VideoIdentityActivity>("callnameType" to (index+1).toString(),
            "cmdFrom" to "main"
        )
    }
    private fun mytoast(text: String) {
        handler.post(Runnable { Toast.makeText(this@MainActivity, text, Toast.LENGTH_LONG).show() })
    }

    private val handler = Handler(Looper.getMainLooper())

    private fun livnessTypeTip() {
        val type = PreferencesUtil.getInt(
            LivenessSettingActivity.TYPE_LIVENSS, LivenessSettingActivity
                .TYPE_NO_LIVENSS
        )

        if (type == LivenessSettingActivity.TYPE_NO_LIVENSS) {
            Toast.makeText(this, "当前活体策略：无活体, 请选用普通USB摄像头", Toast.LENGTH_LONG).show()
        } else if (type == LivenessSettingActivity.TYPE_RGB_LIVENSS) {
            Toast.makeText(this, "当前活体策略：单目RGB活体, 请选用普通USB摄像头", Toast.LENGTH_LONG).show()
        } else if (type == LivenessSettingActivity.TYPE_RGB_IR_LIVENSS) {
            Toast.makeText(
                this, "当前活体策略：双目RGB+IR活体, 请选用RGB+IR摄像头",
                Toast.LENGTH_LONG
            ).show()
        } else if (type == LivenessSettingActivity.TYPE_RGB_DEPTH_LIVENSS) {
            Toast.makeText(this, "当前活体策略：双目RGB+Depth活体，请选用RGB+Depth摄像头", Toast.LENGTH_LONG).show()
        }
    }
}
