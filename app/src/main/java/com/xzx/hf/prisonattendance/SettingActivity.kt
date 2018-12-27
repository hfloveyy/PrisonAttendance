package com.xzx.hf.prisonattendance

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import com.baidu.aip.manager.FaceSDKManager
import com.github.kittinunf.fuel.core.FuelManager
import com.xzx.hf.prisonattendance.baidu.UserListActivity
import com.xzx.hf.prisonattendance.netty.NettyService
import com.xzx.hf.prisonattendance.utils.SharedPreferencesUtils
import kotlinx.android.synthetic.main.activity_setting.*
import org.jetbrains.anko.*
import org.jetbrains.anko.design.textInputLayout

class SettingActivity : AppCompatActivity() , View.OnClickListener{
    private val preferences by lazy { SharedPreferencesUtils(this) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        val dialog = alert("请输入密码进行管理","管理页面") {
            var password: EditText? = null

            customView {
                textInputLayout {
                    password = editText()
                }
            }
            positiveButton("确认") {
                Log.e("Receive",password!!.text.toString())
                if (password!!.text.toString() == "123456"){
                    init()
                    Log.e("Receive",password!!.text.toString())
                }else{
                    toast("密码错误！")
                    startActivity<MainActivity>()
                    //Log.e("Receive",password)
                }
            }

        }.show().setCancelable(false)
        //init()
    }
    fun init(){

        set_groudid_btn.setOnClickListener(this)
        server_ip_btn.setOnClickListener(this)
        view_group_btn.setOnClickListener(this)
        port_btn.setOnClickListener(this)
        appno_btn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        when(v){
            server_ip_btn -> setConfig("设置服务器地址,当前服务器地址:${preferences.serverIP}","eg:192.168.1.1", InputType.TYPE_CLASS_TEXT ,1)
            set_groudid_btn -> setConfig("设置监区,当前监区:${preferences.area}","eg:1",InputType.TYPE_CLASS_NUMBER,2)
            view_group_btn -> viewGroup()
            port_btn -> setConfig("设置端口,当前端口:${preferences.port}","eg:10001",InputType.TYPE_CLASS_NUMBER,3)
            appno_btn ->setConfig("设置APP编号,当前设备编号:${preferences.appno}","eg:101   代表一监区1号设备",InputType.TYPE_CLASS_NUMBER,4)
        }
    }

    private fun setConfig(title:String,hint:String,type:Int,flag:Int){
        showCreateCategoryDialog(title, hint, type, flag)
    }


    private fun viewGroup(){
        if (FaceSDKManager.initStatus != FaceSDKManager.SDK_INITED){
            startActivity<MainActivity>()
        }
        startActivity<UserListActivity>(
            "group_id" to preferences.groupId
        )
    }

    fun showCreateCategoryDialog(title:String,hint:String,type: Int,flag:Int) {
        val context = this
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)

        val view = layoutInflater.inflate(R.layout.dialog_new_category, null)

        val categoryEditText = view.findViewById(R.id.categoryEditText) as EditText
        categoryEditText.inputType = type
        categoryEditText.hint = hint
        builder.setView(view)

        // set up the ok button
        builder.setPositiveButton(android.R.string.ok) { dialog, p1 ->
            val newCategory = categoryEditText.text
            var isValid = true
            if (newCategory.isBlank()) {
                categoryEditText.error = "some error"
                isValid = false
            }

            if (isValid) {
                // do something
                when(flag){
                    1->{
                        val stopIntent = Intent(this, NettyService::class.java)
                        stopService(stopIntent)
                        val startIntent = Intent(this, NettyService::class.java)
                        //stopService(startIntent)
                        preferences.serverIP = newCategory.toString()
                        FuelManager.instance.basePath = "http://"+ preferences.serverIP+":8080/api/prison"

                        Log.e("Netty",preferences.serverIP)
                        stopService(startIntent)
                        startService(startIntent)
                        //server_ip_btn.text = "当前服务器地址:"+preferences.serverIP + "\n点击设置"
                    }
                    2->{
                        preferences.area = newCategory.toString()
                        //set_groudid_btn.text = "当前监区:"+preferences.groupId+"\n点击设置"
                    }
                    3->{
                        preferences.port = newCategory.toString()
                        val startIntent = Intent(this, NettyService::class.java)
                        startService(startIntent)
                        //set_groudid_btn.text = "当前编号:"+preferences.port+"\n点击设置"
                    }
                    4->{
                        preferences.appno = newCategory.toString()
                        //set_groudid_btn.text = "当前设备编号:"+preferences.appno+"\n点击设置"
                    }
                }

            }

            if (isValid) {
                dialog.dismiss()
            }
        }

        builder.setNegativeButton(android.R.string.cancel) { dialog, p1 ->
            dialog.cancel()
        }

        builder.show()
    }
}