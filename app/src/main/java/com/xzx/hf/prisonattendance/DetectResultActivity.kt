package com.xzx.hf.prisonattendance

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.baidu.aip.api.FaceApi
import com.baidu.aip.entity.User
import com.bumptech.glide.Glide
import com.github.salomonbrys.kotson.jsonObject
import com.xzx.hf.prisonattendance.baidu.UserActivity
import com.xzx.hf.prisonattendance.utils.HttpApi
import com.xzx.hf.prisonattendance.utils.PaFaceApi
import com.xzx.hf.prisonattendance.utils.SharedPreferencesUtils
import org.litepal.LitePal
import java.util.ArrayList
import kotlinx.android.synthetic.main.activity_detect_result.*
import kotlinx.android.synthetic.main.activity_user_item_layout.*
import kotlinx.android.synthetic.main.activity_user_item_layout.view.*
import org.jetbrains.anko.selector
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textColor
import org.json.JSONArray
import org.json.JSONObject
import com.github.salomonbrys.kotson.jsonArray
import com.xzx.hf.prisonattendance.entity.UserPlus

class DetectResultActivity : AppCompatActivity(),View.OnClickListener{
    private val preferences by lazy { SharedPreferencesUtils(this) }
    private var adapter: UserAdapter? = null

    private var area = ""

    private var crimialslist = mutableListOf<String>()
    private var policeId:String = ""
    private var policeName:String = ""
    private var taskName:String = ""
    private var total:String = ""
    private var callnameType = ""
    private var appno = ""
    private var msg = ""
    private var saveJson = JSONObject()
    private var saveArray = JSONArray()
    private val statusList = listOf<String>("正常","看病","接见","其他")
    private val userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detect_result)

        findView()
        addListener()
        init()
    }
    private fun init(){
        hideBottomUIMenu()
        val intent = intent
        if (intent != null) {
            try {
                msg = intent.getStringExtra("msg")
                Log.e("TestFuel","MSG:"+msg)
                var json = JSONObject(msg)
                crimialslist = intent.getSerializableExtra("crimialslist") as MutableList<String>
                Log.e("Test",crimialslist.toString())
                policeId = intent.getStringExtra("policeId")
                policeName = intent.getStringExtra("policeName")
                area = intent.getStringExtra("area")
                taskName = intent.getStringExtra("taskName")
                //total = intent.getStringExtra("total")
                callnameType = intent.getStringExtra("callnametype")
                appno = intent.getStringExtra("appno")
                Log.e("TestFuel","CallNameType:"+callnameType)
                saveJson.put("policeId",policeId)
                saveJson.put("policeName",policeName)
                saveJson.put("appno",appno)
                if (appno == preferences.appno)
                    back_btn.text = "保存结果"
                val total = crimialslist.size
                when(callnameType){
                    "3" -> {
                        setTitle("点名结果(外出)")
                        calltype_tv.text = "外出服刑人员列表($total 人)"
                        calltype_tv.textColor = getColor(R.color.fuchsia)
                    }
                    "4" -> {
                        crimialslist.clear()
                        setTitle("点名结果(收回)")

                        calltype_tv.textColor = getColor(R.color.crimson)
                        //Log.e("TestFuel","MSG:"+msg)
                        try {
                            val retmsg = HttpApi.postDataSync(msg,"/callReturn")
                            Log.e("TestFuel","Ret:"+retmsg)
                            val json = JSONObject(retmsg)
                            //Log.e("TestFuel",json.toString())
                            val ulist = json.get("userlist") as JSONArray
                            for (i in 0..(ulist.length() - 1)) {
                                val item = ulist.getJSONObject(i)
                                Log.e("TestFuel","User:"+item.get("userid").toString())
                                val userid = item.get("userid").toString()
                                crimialslist.add(userid)
                                // Your code here
                            }
                        }catch (e:Exception){
                            val ulist = LitePal.where("usertype = 1 and userworkstatus = 3 and area = ?",preferences.area).find(UserPlus::class.java)
                            for (u in ulist){
                                crimialslist.add(u.userId)
                            }
                        }


                        calltype_tv.text = if(crimialslist.size>0){"缺少服刑人员列表(${crimialslist.size} 人)"}else{"服刑人员全部已收回！"}
                        //


                    }
                    "1" -> {
                        setTitle("点名结果(整点)")
                        calltype_tv.text = "缺少服刑人员列表($total 人)"
                        calltype_tv.textColor = getColor(R.color.saddlebrown)
                    }
                    "2" -> {
                        setTitle("点名结果(随机)")
                        calltype_tv.text = "缺少服刑人员列表($total 人)"
                        calltype_tv.textColor = getColor(R.color.indigo)
                    }
                }
                if (callnameType == "1"){
                    //TODO 返回外出未返回人员
                }
                for (userId in crimialslist){
                    Log.e("Result","$userId")
                }
                police_tv.text = "民警警号:$policeId\n民警姓名:$policeName\n"
                adapter!!.setUserList(PaFaceApi.convertId2User("1",crimialslist.toMutableList()))
            }catch (e:Exception){
                Log.e("Test",e.toString())
            }


        }else{
            Log.e("Result", "failed")
        }
        back_btn.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        Log.e("TestFuel","SaveJson:"+saveJson.toString())
        if (appno == preferences.appno) {
            saveJson.put("userlist",saveArray)
            HttpApi.postDataSync(saveJson.toString(),"/UpdateUserStatus")
        }
        startActivity<MainActivity>()
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
    private fun findView() {


        val layoutmanager = LinearLayoutManager(this)
        //设置RecyclerView 布局
        user_list_rv!!.setLayoutManager(layoutmanager)
        user_list_rv!!.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        adapter = UserAdapter()
        user_list_rv!!.setAdapter(adapter)
    }

    private fun addListener() {
        adapter!!.setOnItemClickLitsener(object : OnItemClickListener {

            override fun onItemClick(view: View, position: Int) {
                if (appno != preferences.appno) {
                    return
                }
                val userList = adapter!!.getUserList()
                if (userList.size > position) {
                    val user = userList[position]

                    selector("请选择状态：",statusList){ds, i ->
                        view.user_status_tv.text = "当前状态："+statusList[i]
                        val status = when(statusList[i]){
                            "正常" -> "1"
                            "就医" -> "2"
                            "会见" -> "3"
                            "其他" -> "4"
                            else -> "1"
                        }
                        /*
                        var json = jsonObject(
                            "userid" to user.userId,
                            "status" to status
                        )*/
                        var json = JSONObject()
                        json.put("userid",user.userId)
                        json.put("status",status)

                        saveArray.put(json)
                    }
                    /*
                    val cursor = LitePal.findBySQL("select * from userplus where userid = ?", user.userId)
                    cursor.move(1)
                    val userType = cursor.getString(cursor.getColumnIndex("usertype"))
                    val area = cursor.getString(cursor.getColumnIndex("area"))
                    val intent = Intent(this@DetectResultActivity, UserActivity::class.java)
                    intent.putExtra("user_id", user.userId)
                    intent.putExtra("user_info", user.userInfo)
                    intent.putExtra("area", area)
                    intent.putExtra("user_type", userType)

                    startActivity(intent)
                    */
                }

            }

            override fun onItemLongClick(view: View, position: Int) {

                /*
                if (position <= adapter!!.getUserList().size) {
                    showAlertDialog(adapter!!.getUserList()[position])
                }*/
            }
        })
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

    inner class UserAdapter : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

        private var userList: MutableList<User> = mutableListOf()
        private var mOnItemClickListener: OnItemClickListener? = null

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var userId: TextView
            var userInfo: TextView
            var image: ImageView
            var userstatus:TextView
            init {
                userId = view.findViewById<View>(R.id.user_id_tv) as TextView
                userInfo = view.findViewById<View>(R.id.user_info_tv) as TextView
                image = view.findViewById(R.id.user_image) as ImageView
                userstatus = view.findViewById(R.id.user_status_tv) as TextView
            }
        }

        fun setUserList(userList: MutableList<User>) {
            this.userList = userList
            this.notifyDataSetChanged()
        }

        fun getUserList(): MutableList<User> {
            return userList
        }

        fun setOnItemClickLitsener(mOnItemClickLitsener: OnItemClickListener) {
            mOnItemClickListener = mOnItemClickLitsener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(
                R.layout.activity_user_item_layout, parent,
                false
            )
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = userList[position]
            val userPlus = PaFaceApi.retUserPlus(user)
            val status = when(userPlus.userStatus){
                "1" -> "正常"
                "2" -> "就医"
                "3" -> "会见"
                "4" -> "其他"
                else -> "1"
            }
            val color = when(userPlus.userStatus){
                "1" -> getColor(R.color.blue)
                "2" -> getColor(R.color.red)
                "3" -> getColor(R.color.green)
                "4" -> getColor(R.color.orange)
                else -> getColor(R.color.black)
            }
            holder.userId.text = "服刑人员编号: " + user.userId
            holder.userInfo.text = "服刑人员姓名: " + user.userInfo
            holder.userstatus.text = status
            holder.userstatus.textColor = color
            //Glide.with(this@DetectResultActivity).load("http://${preferences.serverIP}:8080/Uploads/HeadPicture/${user.userId}/${user.userId}.jpg").into(holder.image)

            if (mOnItemClickListener != null) {
                holder.itemView.setOnClickListener {
                    val pos = holder.layoutPosition
                    mOnItemClickListener!!.onItemClick(holder.itemView, pos)
                }

                holder.itemView.setOnLongClickListener {
                    val pos = holder.layoutPosition
                    mOnItemClickListener!!.onItemLongClick(holder.itemView, pos)
                    true
                }
            }
        }

        override fun getItemCount(): Int {
            return userList.size
        }
    }

    interface OnItemClickListener {

        fun onItemClick(view: View, position: Int)
        fun onItemLongClick(view: View, position: Int)
    }


}