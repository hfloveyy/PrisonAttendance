package com.xzx.hf.prisonattendance

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.baidu.aip.api.FaceApi
import com.baidu.aip.entity.User
import com.xzx.hf.prisonattendance.baidu.RegActivity
import com.xzx.hf.prisonattendance.utils.HttpApi
import com.xzx.hf.prisonattendance.utils.PaFaceApi
import kotlinx.android.synthetic.main.activity_update_list.*
import com.xzx.hf.prisonattendance.utils.SearchView
import com.xzx.hf.prisonattendance.utils.SharedPreferencesUtils
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.textColor
import org.json.JSONObject

class UpdateListActivity : AppCompatActivity(){
    private val preferences by lazy { SharedPreferencesUtils(this) }
    private var userList = mutableListOf<User>()
    private var adapter: UserAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_list)

        findView()
        addListener()
        init()
    }
    fun init(){

        val userids = HttpApi.getUserIds(preferences.area?:"1")
        userList = PaFaceApi.convertId2User("1",userids)
        adapter!!.setUserList(userList)
        searchView.searchWay = object:SearchView.SearchWay<User>() {

            override fun getData(): MutableList<User> {
                return userList
            }


            override fun matchItem(item: User, s: String): Boolean {
                //如果串item中包含串s，则匹配
                return item.userId.contains(s)||(item.userInfo.contains(s))
            }

            override fun update(resultList: MutableList<User>) {
                //更新ListView的数据
                setListViewData(resultList)
            }
        }
        police_btn.setOnClickListener{
            setListViewData(PaFaceApi.getPolices(userList))
        }
        criminal_btn.setOnClickListener{
            setListViewData(PaFaceApi.getCriminals(userList))
        }

    }

    fun setListViewData(list:MutableList<User>){

        adapter = UserAdapter()
        adapter!!.setUserList(list)
        user_list_rv!!.setAdapter(adapter)
        addListener()
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
                Log.e("TestFuel","OnItemClick!")
                val userList = adapter!!.getUserList()
                if (userList.size > position) {
                    val user = userList[position]
                    val userPlus = PaFaceApi.retUserPlus(user)
                    startActivity<RegActivity>(
                        "userid" to user.userId,
                        "userinfo" to user.userInfo,
                        "usertype" to userPlus.userType
                    )


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

    inner class UserAdapter : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

        private var userList: MutableList<User> = mutableListOf()
        private var mOnItemClickListener: OnItemClickListener? = null

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            var userId: TextView
            var userInfo: TextView
            var image: ImageView
            var imageStatus:TextView
            init {
                userId = view.findViewById<View>(R.id.user_id_tv) as TextView
                userInfo = view.findViewById<View>(R.id.user_info_tv) as TextView
                image = view.findViewById(R.id.user_image) as ImageView
                imageStatus = view.findViewById(R.id.image_status_tv) as TextView

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
                R.layout.activity_user_item_layout_face, parent,
                false
            )
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = userList[position]
            val userPlus = PaFaceApi.retUserPlus(user)
            if (userPlus.userType == "1"){
                holder.userId.text = "服刑人员编号: " + user.userId
                holder.userInfo.text = "服刑人员姓名：" + user.userInfo
            }else {
                holder.userId.text = "民警编号: " + user.userId
                holder.userInfo.text = "民警姓名：" + user.userInfo
            }
            if (user.featureList[0].faceToken == ""){
                holder.imageStatus.text = "未采集"
                holder.imageStatus.textColor = getResources().getColor(R.color.red)
            }else{
                holder.imageStatus.text = "已采集"
                holder.imageStatus.textColor = getResources().getColor(R.color.green)
            }
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