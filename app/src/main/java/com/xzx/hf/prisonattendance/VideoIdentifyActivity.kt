package com.xzx.hf.prisonattendance

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import java.io.File
import java.util.concurrent.Executors
import kotlin.collections.Map.Entry
import com.baidu.aip.ImageFrame
import com.baidu.aip.api.FaceApi
import com.baidu.aip.db.DBManager
import com.baidu.aip.entity.IdentifyRet
import com.baidu.aip.entity.User
import com.baidu.aip.face.CameraImageSource
import com.baidu.aip.face.FaceDetectManager
import com.baidu.aip.face.PreviewView
import com.baidu.aip.face.camera.CameraView
import com.baidu.aip.face.camera.ICameraControl
import com.baidu.aip.manager.FaceEnvironment
import com.baidu.aip.manager.FaceLiveness
import com.baidu.aip.manager.FaceSDKManager
import com.xzx.hf.prisonattendance.baidu.utils.GlobalFaceTypeModel
import com.baidu.aip.utils.FileUitls
import com.baidu.aip.utils.PreferencesUtil
import com.baidu.idl.facesdk.FaceInfo
import com.baidu.idl.facesdk.FaceTracker
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.TextureView
import android.view.View
import android.view.Window
import android.widget.TextView
import android.widget.Toast
import com.baidu.aip.face.TexturePreviewView
import com.baidu.aip.face.camera.ICameraControl.FLASH_MODE_TORCH
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.kittinunf.fuel.core.FuelManager
import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.put
import com.google.gson.JsonObject
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
import com.xzx.hf.prisonattendance.baidu.LivenessSettingActivity
import com.xzx.hf.prisonattendance.baidu.utils.Utils
import com.xzx.hf.prisonattendance.entity.CallLog
import com.xzx.hf.prisonattendance.entity.UserPlus
import com.xzx.hf.prisonattendance.netty.NettyClient
import com.xzx.hf.prisonattendance.netty.NettyListener
import com.xzx.hf.prisonattendance.utils.*
import kotlinx.android.synthetic.main.activity_video_identify.*
import java.io.Serializable
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import org.json.JSONException
import org.json.JSONObject
import org.jetbrains.anko.*
import org.litepal.LitePal


class VideoIdentityActivity : AppCompatActivity(), View.OnClickListener  ,NettyListener{
    private val preferences by lazy { SharedPreferencesUtils(this) }

    // 用于检测人脸。
    private var faceDetectManager: FaceDetectManager? = null

    private var countSet = mutableSetOf<String>()
    private var calllist = mutableListOf<String>()
    //本监区罪犯编号
    private var countCriminals = mutableListOf<String>()
    //本机点名人数
    private var countInt:Int = 0
    //人脸库总数
    private var countTotal:Int? = 0
    //本监区罪犯人数
    private var countCriminalsTotal:Int? = 0
    private var isPolice = false
    private var policeId:String = ""
    private var policeName:String = ""

    private val handler = Handler()
    private var groupId = "1"
    //设备编号
    private var appno = ""
    //任务开始的设备编号
    private var taskAppno = ""
    //点名类型
    private var callnameType = ""
    //监区
    protected var area = ""
    //任务名称
    protected var taskName = ""
    @Volatile
    private var identityStatus = FEATURE_DATAS_UNREADY


    private val es = Executors.newSingleThreadExecutor()

    //用于画人脸框
    private val paint = Paint()
    internal var rectF = RectF()
    private var heigth = 0
    private var width = 0

    //Netty客户端 用于TCP长连接通信
    private lateinit var nettyClient:NettyClient

    private lateinit var  mCustomToast: CustomToast
    private lateinit var cameraImageSource:CameraImageSource

    //app实例 用于获取全局变量
    private lateinit var app: MyApplication

    //用于判断识别由谁发起
    private var cmdFrom:String = ""

    //同步各app之间人数
    private var appSet:MutableSet<Pair<String,String>> = mutableSetOf()
    private var appnoSet:MutableSet<String> = mutableSetOf()
    private var countNetTotal:Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        initPermisson()
        supportActionBar!!.hide()


        setContentView(R.layout.activity_video_identify)


        app  = application as MyApplication
        init()

        DBManager.getInstance().init(this)
        loadFeature2Memery()
        //syncDB()

        connect()

        initCmd()
        addListener()

    }
    fun syncDB(){

        Thread(object : Runnable{
            override fun run() {
                try {
                    HttpApi.syncDB()
                    loadFeature2Memery()
                }catch (e:Exception){
                    Log.e("DBSync",e.toString())
                }
            }
        }).start()



    }
    private fun initCmd(){
        //获取参数
        val intent = intent
        try {
            if (intent != null) {
                callnameType = intent.getStringExtra("callnameType")
                when(callnameType){
                    "3" -> {
                        callnametype_tv.text = "点名类型:外出"
                        //callnametype_tv.textColor = getColor(R.color.fuchsia)
                    }
                    "4" -> {
                        callnametype_tv.text = "点名类型:收回"
                        //callnametype_tv.textColor = getColor(R.color.crimson)
                    }
                    "1" -> {
                        callnametype_tv.text = "点名类型:整点"
                        val values = ContentValues()
                        values.put("userworkstatus", "3")
                        LitePal.updateAll(UserPlus::class.java,values,"userworkstatus = 1")

                        //callnametype_tv.textColor = getColor(R.color.saddlebrown)
                    }
                    "2" -> {
                        callnametype_tv.text = "点名类型:随机"
                        val values = ContentValues()
                        values.put("userworkstatus", "3")
                        LitePal.updateAll(UserPlus::class.java,values,"userworkstatus = 1")
                        //callnametype_tv.textColor = getColor(R.color.indigo)
                    }
                }
                cmdFrom = intent.getStringExtra("cmdFrom")
                when(cmdFrom){
                    "main" ->{
                        displayTip("总数:"+intent.getStringExtra("all_count")+"人",total_tv)
                    }
                    "other" ->{
                        taskName = intent.getStringExtra("taskname")
                        policeId = intent.getStringExtra("policeId")
                        policeName = intent.getStringExtra("policeName")
                        taskAppno = intent.getStringExtra("taskappno")
                        isPolice = true
                        /*
                        alert("登陆民警为:$policeName(警号为$policeId)", "尊敬的警官") {
                            positiveButton("确认开始$area 监区点名") {}
                        }.show()*/
                        doAsync {
                            uiThread {
                                police_tv.text = "民警警号:$policeId\n民警姓名:$policeName\n监区:$area"

                            }
                        }
                        beginCount()
                    }
                }
            }else{
                Log.e("PA_VideoIdentifyActivity", "intent failed")
            }
        }catch (e:Exception){
            Log.e("Test",e.toString())
        }

    }

    private fun init() {

        mCustomToast = CustomToast(this)
        hideBottomUIMenu()






        //FuelManager.instance.basePath = "http://"+ preferences.serverIP+":8080/api/prison"//"https://httpbin.org"
        //获取本地设置
        groupId = preferences.groupId!!
        appno = preferences.appno!!
        area = preferences.area!!
        taskAppno = appno
        val host = preferences.serverIP
        val port = preferences.port

        //设置按钮监听
        //begin_btn.setOnClickListener(this)
        finish_btn.setOnClickListener(this)
        finish_btn.visibility = View.GONE

        Log.e("Netty",host)

        //初始化Netty
        nettyClient = NettyClient(host!!,port!!.toInt())


        displayTip("本机已点:0人", count_tv)
        displayTip("本次已点:0人", count_all_tv)


        //获取屏幕宽高
        val dm = resources.displayMetrics
        heigth = dm.heightPixels
        width = dm.widthPixels


        //初始化人脸识别功能
        faceDetectManager = FaceDetectManager(applicationContext)
        // 从系统相机获取图片帧。
        //val cameraImageSource = CameraImageSource(this)
        cameraImageSource = CameraImageSource(this)
        // 图片越小检测速度越快，闸机场景640 * 480 可以满足需求。实际预览值可能和该值不同。和相机所支持的预览尺寸有关。
        // 可以通过 camera.getParameters().getSupportedPreviewSizes()查看支持列表。
        cameraImageSource.cameraControl.setPreferredPreviewSize(1280, 720)
        // cameraImageSource.getCameraControl().setPreferredPreviewSize(640, 480);
        //val x = cameraImageSource.cameraControl.getFlashMode()
        // 设置最小人脸，该值越小，检测距离越远，该值越大，检测性能越好。范围为80-200
        FaceSDKManager.getInstance().faceDetector.setMinFaceSize(100)
        FaceSDKManager.getInstance().getFaceDetector().setNumberOfThreads(4)
        // 设置预览
        cameraImageSource.setPreviewView(preview_view)
        // 设置图片源
        faceDetectManager!!.imageSource = cameraImageSource
        // 设置人脸过滤角度，角度越小，人脸越正，比对时分数越高
        faceDetectManager!!.faceFilter.setAngle(20)

        texture_view!!.isOpaque = false
        // 不需要屏幕自动变黑。
        texture_view!!.keepScreenOn = true

        face_view!!.isOpaque = false
        face_view!!.keepScreenOn = true
        val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        if (isPortrait) {
            preview_view!!.scaleType = PreviewView.ScaleType.FIT_WIDTH
            // 相机坚屏模式
            cameraImageSource.cameraControl.setDisplayOrientation(CameraView.ORIENTATION_PORTRAIT)
        } else {
            preview_view!!.scaleType = PreviewView.ScaleType.FIT_HEIGHT
            // 相机横屏模式
            cameraImageSource.cameraControl.setDisplayOrientation(CameraView.ORIENTATION_HORIZONTAL)
        }

        setCameraType(cameraImageSource)

    }
    //画识别框
    private fun faceFrame(){
        val canvas = face_view!!.lockCanvas()?:return
        //Log.e("TestCanvas","here")
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        paint.color = Color.WHITE
        paint.strokeWidth = 5.0f
        //paint.style = Paint.Style.STROKE
        // 绘制框
        val len = 120f
        val pointX = 80f
        val pointY = 80f
        val x = width-pointX*2
        canvas.drawLine(pointX, pointY, pointX+len, pointY, paint)
        canvas.drawLine(width-pointX, pointY, width-(pointX+len), pointY, paint)
        canvas.drawLine(pointX, pointY, pointX, pointY+len, paint)
        canvas.drawLine(width-pointX, pointY, width-pointX, pointY+len, paint)

        canvas.drawLine(pointX, pointY+x, pointX+len, pointY+x, paint)
        canvas.drawLine(width-pointX, pointY+x, width-(pointX+len), pointY+x, paint)
        canvas.drawLine(pointX, pointY+x, pointX, pointY+x-len, paint)
        canvas.drawLine(width-pointX, pointY+x, width-pointX, pointY+x-len, paint)

        face_view!!.unlockCanvasAndPost(canvas)

    }
    //设置摄像头
    private fun setCameraType(cameraImageSource: CameraImageSource) {
        // TODO 选择使用前置摄像头
        // cameraImageSource.getCameraControl().setCameraFacing(ICameraControl.CAMERA_FACING_FRONT);

        // TODO 选择使用usb摄像头
        //cameraImageSource.cameraControl.setCameraFacing(ICameraControl.CAMERA_USB)
        // 如果不设置，人脸框会镜像，显示不准
        //previewView!!.textureView.scaleX = -1f

        // TODO 选择使用后置摄像头
        cameraImageSource.cameraControl.setCameraFacing(ICameraControl.CAMERA_FACING_BACK)

        //preview_view!!.textureView.scaleX = -1f
    }

    private fun addListener() {
        // 设置回调，回调人脸检测结果。
        faceDetectManager!!.setOnFaceDetectListener { retCode, infos, frame ->
            // TODO 显示检测的图片。用于调试，如果人脸sdk检测的人脸需要朝上，可以通过该图片判断
            /*
            val bitmap = Bitmap.createBitmap(
                frame.argb, frame.width, frame.height, Bitmap.Config
                    .ARGB_8888
            )
            handler.post { test_view!!.setImageBitmap(bitmap) }
            */
            if (retCode == FaceTracker.ErrCode.OK.ordinal && infos != null) {
                asyncIdentity(frame, infos)
            }
            faceFrame()
            //showFrame(frame, infos)


        }

    }
    //初始化授权
    fun initPermisson(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this, Manifest
                    .permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA),
                100
            )
            return
        }
    }
    override fun onStart() {
        super.onStart()
        beginCount()
        // 开始检测
        //faceDetectManager!!.start()
        //faceDetectManager!!.setUseDetect(true)
    }

    override fun onStop() {
        super.onStop()
        // 结束检测。
        faceDetectManager!!.stop()
        nettyClient.disconnect()
    }

    override fun onDestroy() {
        super.onDestroy()
        nettyClient.disconnect()
        faceDetectManager!!.stop()
        //unregisterReceiver(msgReceiver)
    }

    override fun onClick(v: View) {
        when(v){
            finish_btn -> {
                finishCount()
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if(event!!.keyCode == KeyEvent.KEYCODE_BACK ) {
            //do something.
            toast("点名期间返回键被禁用")
            return true
        }else {
            return super.dispatchKeyEvent(event)
        }
    }
    //接收Message

    override fun onMessageResponse(msg:Any) {

        //Log.e("Netty", "onMessageResponse:" + msg.toString())
        handleMsg(msg)

//        byte[] bytes = byteBuf.array();
//
//        String hexFun3 = bytesToHexFun3(bytes, byteBuf.writerIndex());
//        logRece(hexFun3);
    }

    fun handleMsg(msg: Any){
        try {
            val jsonObject = JSONObject(msg.toString())
            val cmd = jsonObject.getString("cmd")
            if (cmd == "nettest") return
            Log.e("Netty","handleMsg:"+msg.toString())
            val areaMsg = jsonObject.getString("area")
            val taskNameMsg = jsonObject.getString("taskname")
            val appnoMsg = jsonObject.getString("appno")
            if(areaMsg!=area) {
                Log.e("Netty","监区不匹配！")
                return
            }
            if(taskNameMsg!=taskName) {
                Log.e("Netty","任务不匹配！$taskNameMsg")
                return
            }
            when(cmd){

                "callname-success" ->{
                    Log.e("Netty","handleMsg:"+msg.toString())
                    if(appnoMsg == appno) {
                        Log.e("Netty","不接受自己的msg！$taskNameMsg")
                        return
                    }
                    var netTotal = 0
                    val appno = jsonObject.getString("appno")
                    val total = jsonObject.getString("total")
                    val userId = jsonObject.getString("peopleid")
                    countSet.add(userId)
                    appnoSet.add(appno)
                    Log.e("Netty","appno:$appno ,total:$total")
                    for (value in appSet){
                        if (value.first in appnoSet )
                            appSet.remove(value)
                    }
                    appSet.add(Pair(appno,total))
                    for (value in appSet){
                        netTotal += value.second.toInt()
                        Log.e("Netty", "appno:$appno ,total:$netTotal")

                    }
                    netTotal += countInt
                    doAsync {
                        uiThread {
                            count_all_tv.text = "本次已点:$netTotal 人"
                            Log.e("Netty",netTotal.toString())
                        }
                    }
                    countNetTotal = netTotal

                }

            }

        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }
    //监听Service的Status
    override fun onServiceStatusConnectChanged(statusCode:Int) {
        doAsync {
            if (statusCode == NettyListener.STATUS_CONNECT_SUCCESS) {
                Log.e("Netty", "STATUS_CONNECT_SUCCESS:")
            } else {
                Log.e("Netty", "onServiceStatusConnectChanged:" + statusCode)
            }
            /*
            uiThread {
                toast("Service Status Changed！")
            }*/
        }


    }


    private fun loadFeature2Memery() {
        if (identityStatus != FEATURE_DATAS_UNREADY) {
            return
        }
        es.submit {
            Thread.currentThread().priority = Thread.MAX_PRIORITY
            // android.os.Process.setThreadPriority (-4);
            FaceApi.getInstance().loadFacesFromDB(groupId)
            //toast("人脸数据加载完成，即将开始1：N")
            countTotal = FaceApi.getInstance().group2Facesets[groupId]?.size//人脸库总数
            countCriminals = PaFaceApi.getCriminals(area)
            countCriminalsTotal = countCriminals.size
            Log.e("DBSync","总数:$countCriminalsTotal")
            //userList = FaceApi.getInstance().getUserList(groupId)
            displayTip("总数:$countCriminalsTotal 人", total_tv)
            //displayTip("底库人脸个数：$countTotal", facesets_count_tv)
            identityStatus = IDENTITY_IDLE
        }
    }

    private fun asyncIdentity(imageFrame: ImageFrame, faceInfos: Array<FaceInfo>?) {
        if (identityStatus != IDENTITY_IDLE) {
            return
        }

        es.submit(Runnable {
            if (faceInfos == null || faceInfos.size == 0) {
                return@Runnable
            }
            val liveType = PreferencesUtil.getInt(
                LivenessSettingActivity.TYPE_LIVENSS, LivenessSettingActivity
                    .TYPE_NO_LIVENSS
            )
            if (liveType == LivenessSettingActivity.TYPE_NO_LIVENSS) {
                identity(imageFrame, faceInfos[0])
            } else if (liveType == LivenessSettingActivity.TYPE_RGB_LIVENSS) {

                if (rgbLiveness(imageFrame, faceInfos[0]) > FaceEnvironment.LIVENESS_RGB_THRESHOLD) {
                    if (mCustomToast != null)
                        mCustomToast.hide()
                    identity(imageFrame, faceInfos[0])
                } else {
                    //if (mCustomToast != null)
                    //    mCustomToast.alwaysShow("rgb活体分数过低")
                     toast("rgb活体分数过低")
                }
            }
        })
    }

    private fun rgbLiveness(imageFrame: ImageFrame, faceInfo: FaceInfo): Float {

        val starttime = System.currentTimeMillis()
        val rgbScore = FaceLiveness.getInstance().rgbLiveness(
            imageFrame.argb, imageFrame
                .width, imageFrame.height, faceInfo.landmarks
        )
        val duration = System.currentTimeMillis() - starttime

        runOnUiThread {
            //rgb_liveness_duration_tv!!.visibility = View.VISIBLE
            //rgb_liveness_score_tv!!.visibility = View.VISIBLE
            //rgb_liveness_duration_tv!!.text = "RGB活体耗时：$duration"
            //rgb_liveness_score_tv!!.text = "RGB活体得分：$rgbScore"
            //toast("RGB活体得分：$rgbScore")
        }

        return rgbScore
    }

    private fun identity(imageFrame: ImageFrame, faceInfo: FaceInfo) {


        val raw = Math.abs(faceInfo.headPose[0])
        val patch = Math.abs(faceInfo.headPose[1])
        val roll = Math.abs(faceInfo.headPose[2])
        // 人脸的三个角度大于20不进行识别
        if (raw > 20 || patch > 20 || roll > 20) {
            return
        }

        identityStatus = IDENTITYING

        val starttime = System.currentTimeMillis()
        val argb = imageFrame.argb
        val rows = imageFrame.height
        val cols = imageFrame.width
        val landmarks = faceInfo.landmarks

        val type = PreferencesUtil.getInt(GlobalFaceTypeModel.TYPE_MODEL, GlobalFaceTypeModel.RECOGNIZE_LIVE)
        var identifyRet: IdentifyRet? = null
        if (type == GlobalFaceTypeModel.RECOGNIZE_LIVE) {
            identifyRet = FaceApi.getInstance().identity(argb, rows, cols, landmarks, groupId)
        } else if (type == GlobalFaceTypeModel.RECOGNIZE_ID_PHOTO) {
            identifyRet = FaceApi.getInstance().identityForIDPhoto(argb, rows, cols, landmarks, groupId)
        }
        if (identifyRet != null) {
            //synchronized(this){
                displayUserOfMaxScore(identifyRet.userId, identifyRet.score)
            //}
        }
        identityStatus = IDENTITY_IDLE
        //displayTip("特征抽取对比耗时:" + (System.currentTimeMillis() - starttime), feature_duration_tv)
    }


    //处理人脸对比结果
    private fun displayUserOfMaxScore(userId: String, score: Float) {

        handler.post(Runnable {
            //全点完 结束
            if (countSet.size == countCriminalsTotal){
                finishCount()
                return@Runnable
            }
            //不满90分 不识别
            if (score < 90) {
                //score_tv!!.text = ""
                //match_user_tv!!.text = ""
                //match_avator_iv!!.setImageBitmap(null)
                toast("未知人员！")
                return@Runnable
            }

            //score_tv!!.text = score.toString()
            val user = FaceApi.getInstance().getUserInfo(groupId, userId) ?: return@Runnable


            if (isPolice == false){
                isPolice = PaFaceApi.isPolice(user)
                if (isPolice){
                    MyUtils.playVoice(app.applicationContext)
//area +"-"+callnameType+"-"+ appno +"-"+ DateUtil.nowDateTime
                    Log.e("TestFuel","TaskName:" + taskName)
                    val userPlus = PaFaceApi.retUserPlus(user)
                    area = userPlus.area
                    preferences.area = area
                    countCriminals = PaFaceApi.getCriminals(area)
                    countCriminalsTotal = countCriminals.size
                    Log.e("Netty","$countCriminalsTotal")
                    displayTip("总数:$countCriminalsTotal 人", total_tv)

                    val dialog = alert("警员为:${user.userInfo}(警号为${user.userId})", "尊敬的警官") {
                        positiveButton("确认开始点名") {
                            hideBottomUIMenu()
                            preferences.num += 1
                            taskName = DateUtil.nowDate + area.substring(7,9) + (appno.toLong()*10000 + preferences.num).toString()
                            val jsonCallList = jsonArray(calllist)
                            police_tv.text = "警员警号:${user.userId}\n警员姓名:${user.userInfo}\n监区:${userPlus.area}"
                            policeId = user.userId
                            policeName = user.userInfo
                            //发送开始识别Msg
                            val json = jsonObject(
                                "from" to "app",
                                "to" to "all",
                                "cmd" to "start-callname",
                                "area" to area,
                                "appno" to appno,
                                "callnametype" to callnameType,
                                "taskname" to taskName,
                                "peopleid" to policeId,
                                "userinfo" to policeName,
                                "calllist" to jsonCallList,
                                "type" to "",
                                "time" to DateUtil.nowDateTime)

                            sendMsg(json.toString())
                            if (callnameType == "1"||callnameType == "2"){
                                val values = ContentValues()
                                values.put("userworkstatus", "3")
                                LitePal.updateAll(UserPlus::class.java,values,"userworkstatus = 1")
                            }
                        }
                        negativeButton("取消"){
                            startActivity<MainActivity>()
                        }
                     }.show()
                    dialog.setCancelable(false)
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20f)
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20f)

                }else{
                    toast("请民警先识别身份！")
                }
            }


            val userPlus = PaFaceApi.retUserPlus(user)
            if (userPlus.area != area){
                toast("非本监区人员")
                return@Runnable
            }

            if (isPolice && !PaFaceApi.isPolice(user)){
                if (countSet.contains(userId)){

                    toast("$userId 已点过！")
                    return@Runnable
                }else{
                    if (callnameType == "1"||callnameType == "2"||callnameType == "4") {
                        userPlus.userWorkStatus = "1"
                    } else if (callnameType == "3"){
                        userPlus.userWorkStatus = "3"
                    }
                    userPlus.save()
                    countSet.add(userId)
                    countInt += 1
                    countNetTotal += 1
                    count_tv.text = "本机已点人数:$countInt 人"
                    count_all_tv.text = "本次已点:$countNetTotal 人"
                    val jsonCallList = jsonArray(calllist)
                    //{“from”:”app”,”to”:”all”, "cmd":"callname-success",”area”:”4”, ”appno”:”1” , ”callnametype”:”1”,”peopleid”:”xxxxxx”,"time":"2018-11-01 17:48:06"}
                    val json = jsonObject(
                        "from" to "app",
                        "to" to "all",
                        "cmd" to "callname-success",
                        "area" to area,
                        "appno" to appno,
                        "callnametype" to callnameType,
                        "peopleid" to userId,
                        "userinfo" to user.userInfo,
                        "taskname" to taskName,
                        "type" to "1",//0 警察 1 罪犯
                        "total" to countInt.toString(),
                        "calllist" to jsonCallList,
                        "time" to DateUtil.nowDateTime)
                    Log.e("TestFuel",json.toString())
                    sendMsg(json.toString())



                }

                match_user_tv!!.text = "编号:${user.userId}\n姓名:${user.userInfo}"
                //在线加载图片
                val requestOption = RequestOptions()
                //设置不缓存
                //requestOption.diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                requestOption.error(R.drawable.avatar)
                Glide.with(this@VideoIdentityActivity).applyDefaultRequestOptions(requestOption)
                    .load("http://${preferences.serverIP}:8080/Uploads/HeadPicture/${user.userId}/${user.userId}.jpg").into(match_avator_iv)
            }
        })
    }

    private fun toast(text: String) {
        //handler.post {
            //Toast.makeText(this@VideoIdentityActivity, text, Toast.LENGTH_SHORT).show()
            MyUtils.showToast(this@VideoIdentityActivity, text, Toast.LENGTH_SHORT)
            //mCustomToast.alwaysShow(text)

        //}

    }

    //同步显示信息
    private fun displayTip(text: String, textView: TickerView?) {
        handler.post {
            textView!!.setCharacterLists(TickerUtils.provideNumberList())
            textView.text = text }
    }


    //发送消息 to MsgServer
    private fun sendMsg(msg:String){
        val callLog = CallLog()
        callLog.calllog = msg
        callLog.updatetime = System.currentTimeMillis()
        callLog.save()
        MyUtils.playVoice(app.applicationContext)
        Thread(object : Runnable{
            override fun run() {
                try {
                    val retmsg = HttpApi.postDataSync(msg)
                    //Log.e("TestFuel",retmsg)
                    nettyClient.sendMsgToServer(msg,object :ChannelFutureListener {
                        override fun operationComplete(channelFuture: ChannelFuture) {
                            if (channelFuture.isSuccess()) {                //4
                                Log.d("Netty", "Write auth successful")
                            } else {
                                Log.d("Netty", "Write auth error")
                            }
                        }
                    })

                } catch (e: Exception) {
                    Log.d("", e.message)
                }
            }
        }).start()


    }


    //开始点名
    private fun beginCount(){
        MyUtils.playVoice(app.applicationContext)
        //begin_btn.visibility = View.GONE
        finish_btn.visibility = View.VISIBLE
        police_tv.text = ""

        //app.taskName = taskName
        app.detectStatus = MyApplication.DETECT_RUNNING

        faceDetectManager!!.start()
        faceDetectManager!!.setUseDetect(true)
        cameraImageSource.cameraControl.setFlashMode(FLASH_MODE_TORCH)
    }

    //结束点名
    private fun finishCount(){
        if (isPolice == false){
            finish()
            startActivity<MainActivity>()
        }
        //begin_btn.visibility = View.VISIBLE
        finish_btn.visibility = View.GONE
        calllist = countSet.toMutableList()
        //外出传递点过的人员名单
        if(callnameType == "3" || callnameType == "4"){
            countCriminals = countSet.toMutableList()
        }else{
            val not_count = countCriminals.removeAll(countSet.orEmpty())
        }


        //重置，结束点名

        val dialog = alert("点名结束！本机已点人数$countInt", "$area 监区") {
            positiveButton("结束点名") {

                hideBottomUIMenu()
                if (taskAppno == appno){
                    val countCrimialsList = countCriminals.toMutableList()
                    val jsonCountCrimials = jsonArray(countCrimialsList)
                    val jsonCallList = jsonArray(calllist)
                    match_avator_iv!!.setImageBitmap(null)
                    faceDetectManager!!.stop()
                    val json = jsonObject(
                        "from" to "app",
                        "to" to "all",
                        "cmd" to "end-callname",
                        "area" to area,
                        "appno" to appno,
                        "callnametype" to callnameType,
                        "taskname" to taskName,
                        "peopleid" to policeId,
                        "userinfo" to policeName,
                        //"total" to countNetTotal,
                        "type" to "",
                        "calllist" to jsonCallList,
                        "crimialslist" to jsonCountCrimials,
                        "time" to DateUtil.nowDateTime)
                    //{“from”:”app”,”to”:”web”, "cmd":"end-callname",”area”:”4”, ”appno”:”1” , ”callnametype”:”1”,"time":"2018-11-01 17:48:06"}

                    sendMsg(json.toString())

                    //score_tv!!.text = ""
                    match_user_tv!!.text = ""
                    //countSet.clear()
                    //nettyClient.disconnect()

                    app.detectStatus = MyApplication.DETECT_IDEL
                    startActivity<DetectResultActivity>(
                        "msg" to json.toString(),
                        "policeId" to policeId,
                        "policeName" to policeName,
                        "area" to area,
                        "crimialslist" to countCriminals as Serializable,
                        "taskName" to taskName,
                        "total" to countNetTotal.toString(),
                        "callnametype" to callnameType,
                        "appno" to appno
                    )
                }else{
                    faceDetectManager!!.stop()
                    //begin_btn.visibility = View.GONE
                    finish_btn.visibility = View.GONE
                    //toast("等待主机结束点名！")
                    alert ("请等待主机结束点名！！","提示"){  }.show().setCancelable(false)
                    //startActivity<MainActivity>()
                }

                //countInt = 0

                //count_tv!!.text = "已点人数:0人"
                //total_tv!!.text = "总数:$countTotal 人"

                //startActivity<MainActivity>()
            }
            negativeButton("继续点名"){
                finish_btn.visibility = View.VISIBLE
                hideBottomUIMenu()
            }
        }.show()
        dialog.setCancelable(false)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextSize(20f)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextSize(20f)


    }

    init {
        paint.color = Color.YELLOW
        paint.style = Paint.Style.STROKE
        paint.textSize = 50f
        paint.strokeWidth = 6f
    }


    /**
     * 绘制人脸框。
     */
    private fun showFrame(imageFrame: ImageFrame, faceInfos: Array<FaceInfo>?) {

        val canvas = texture_view!!.lockCanvas() ?: return

        if (faceInfos == null || faceInfos.size == 0) {
            // 清空canvas
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            texture_view!!.unlockCanvasAndPost(canvas)
            return
        }
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        //canvas.drawRect(200f,200f,500f,800f, paint2)
        val faceInfo = faceInfos[0]


        rectF.set(getFaceRect(faceInfo, imageFrame))

        // 检测图片的坐标和显示的坐标不一样，需要转换。
        preview_view!!.mapFromOriginalRect(rectF)

        val yaw = Math.abs(faceInfo.headPose[0])
        val patch = Math.abs(faceInfo.headPose[1])
        val roll = Math.abs(faceInfo.headPose[2])
        if (yaw > 20 || patch > 20 || roll > 20) {
            // 不符合要求，绘制黄框
            paint.color = Color.YELLOW

            val text = "请正视屏幕"
            val width = paint.measureText(text) + 50
            val x = rectF.centerX() - width / 2
            paint.color = Color.RED
            paint.style = Paint.Style.FILL
            canvas.drawText(text, x + 25, rectF.top - 20, paint)
            paint.color = Color.YELLOW

        } else {
            // 符合检测要求，绘制绿框
            paint.color = Color.WHITE
        }
        paint.style = Paint.Style.STROKE
        // 绘制框
        canvas.drawRect(rectF, paint)
        texture_view!!.unlockCanvasAndPost(canvas)

    }



    /**
     * 获取人脸框区域。
     *
     * @return 人脸框区域
     */
    // TODO padding?
    fun getFaceRect(faceInfo: FaceInfo, frame: ImageFrame): Rect {
        val rect = Rect()
        val points = IntArray(8)
        faceInfo.getRectPoints(points)

        var left = points[2]
        var top = points[3]
        val right = points[6]
        val bottom = points[7]

        //            int width = (right - left) * 4 / 3;
        //            int height = (bottom - top) * 4 / 3;
        //
        //            left = getInfo().mCenter_x - width / 2;
        //            top = getInfo().mCenter_y - height / 2;
        //
        //            rect.top = top;
        //            rect.left = left;
        //            rect.right = left + width;
        //            rect.bottom = top + height;

        //            int width = (right - left) * 4 / 3;
        //            int height = (bottom - top) * 5 / 3;
        val width = right - left
        val height = bottom - top

        //            left = getInfo().mCenter_x - width / 2;
        //            top = getInfo().mCenter_y - height * 2 / 3;
        left = (faceInfo.mCenter_x - width / 2).toInt()
        top = (faceInfo.mCenter_y - height / 2).toInt()


        rect.top = if (top < 0) 0 else top
        rect.left = if (left < 0) 0 else left
        rect.right = if (left + width > frame.width) frame.width else left + width
        rect.bottom = if (top + height > frame.height) frame.height else top + height

        return rect
    }
    //Netty Connect
    private fun connect() {
        Log.d("Netty", "connect")
        if (!nettyClient.connectStatus) {
            nettyClient.setListener(this)
            nettyClient.connect()//连接服务器
        } else {
            nettyClient.disconnect()
        }
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    fun hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            val v = this.getWindow().getDecorView()
            v.setSystemUiVisibility(View.GONE)
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            val decorView = getWindow().getDecorView()
            val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN)
            decorView.setSystemUiVisibility(uiOptions)
        }
    }
    companion object {

        private val FEATURE_DATAS_UNREADY = 1
        private val IDENTITY_IDLE = 2
        private val IDENTITYING = 3
    }
}

