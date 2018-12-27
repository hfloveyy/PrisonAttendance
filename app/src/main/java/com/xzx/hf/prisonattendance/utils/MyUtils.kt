package com.xzx.hf.prisonattendance.utils

import android.annotation.TargetApi
import android.content.Context
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.RingtoneManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.Log
import android.view.View
import java.lang.Exception
import android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
import android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION



object MyUtils {
    @TargetApi(Build.VERSION_CODES.N)
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    fun changeFlashLight(context:Context,openOrClose: Boolean) {
        //判断API是否大于24（安卓7.0系统对应的API）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                //获取CameraManager
                val mCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                //获取当前手机所有摄像头设备ID
                val ids = mCameraManager.getCameraIdList()
                for (id in ids) {
                    val c = mCameraManager.getCameraCharacteristics(id)
                    //查询该摄像头组件是否包含闪光灯
                    val flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                    val lensFacing = c.get(CameraCharacteristics.LENS_FACING)
                    if (flashAvailable != null && flashAvailable!!
                        && lensFacing != null && lensFacing === CameraCharacteristics.LENS_FACING_BACK
                    ) {
                        //打开或关闭手电筒
                        Log.e("TestFuel",id)
                        val on = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                        mCameraManager.setTorchMode(id, openOrClose)
                    }
                }
            } catch (e: CameraAccessException) {
                Log.e("TestFule",e.printStackTrace().toString())
                e.printStackTrace()
            }

        }
    }

    fun openLight(){
        try {
            val camera = Camera.open()
            camera.startPreview()
            val parameters = camera.getParameters()
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
            camera.setParameters(parameters)
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }

    fun playVoice(ctx:Context){
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val rt = RingtoneManager.getRingtone(ctx, uri)
        rt.play()
    }

}
