package com.xzx.hf.prisonattendance

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.support.v7.util.DiffUtil
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
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.textColor
import org.json.JSONObject

class UpdateListActivity : AppCompatActivity(){
    private val preferences by lazy { SharedPreferencesUtils(this) }
    private var userList = mutableListOf<User>()
    private var adapter: UserAdapter? = null
    private val handler = Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_list)
        findView()
        addListener()
        init()
    }
    fun init(){
        try {
            var userids = mutableListOf<String>()
            val mConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.getActiveNetworkInfo()
            if (mNetworkInfo != null) {
                userids = HttpApi.getUserIds(preferences.area?:"1")
            }else{
                userids = PaFaceApi.getUserIds(preferences.area)
            }
            Log.e("TestFuel","userids:"+userids.toString())
            if (userids.isEmpty()){
                userids = PaFaceApi.getUserIds(preferences.area)
            }
            Log.e("TestFuel","getuserSize："+userids.size)
            userList = PaFaceApi.convertId2User("1",userids)
            adapter!!.setUserList(PaFaceApi.getCriminals(userList))

            police_btn.isEnabled = true
            criminal_btn.isEnabled = false
            police_btn.backgroundColor = getResources().getColor(R.color.slateblue)
            criminal_btn.backgroundColor = getResources().getColor(R.color.hotpink)
            criminal_btn.textColor = getResources().getColor(R.color.white)

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
                Log.e("TestFuel","PoliceSize："+PaFaceApi.getPolices(userList).size)
                criminal_btn.backgroundColor = getResources().getColor(R.color.slateblue)
                setListViewData(PaFaceApi.getPolices(userList))
                police_btn.backgroundColor = getResources().getColor(R.color.hotpink)
                police_btn.isEnabled = false
                criminal_btn.isEnabled = true
                police_btn.text = "警察(√)"
                police_btn.textColor = getResources().getColor(R.color.white)
                criminal_btn.text = "服刑人员"

            }
            criminal_btn.setOnClickListener{
                Log.e("TestFuel","CriminalsSize："+PaFaceApi.getCriminals(userList).size)
                police_btn.backgroundColor = getResources().getColor(R.color.slateblue)
                setListViewData(PaFaceApi.getCriminals(userList))
                criminal_btn.backgroundColor = getResources().getColor(R.color.hotpink)
                police_btn.isEnabled = true
                criminal_btn.isEnabled = false
                criminal_btn.text = "服刑人员(√)"
                criminal_btn.textColor = getResources().getColor(R.color.white)
                police_btn.text = "警察"

            }
        }catch (e:Exception){
            Log.e("TestFuel",e.toString())
        }


    }

    fun setListViewData(list:MutableList<User>){

        //adapter = UserAdapter()
        adapter!!.setUserList(list)
        //user_list_rv!!.setAdapter(adapter)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            var userids = mutableListOf<String>()
            val mConnectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val mNetworkInfo = mConnectivityManager.getActiveNetworkInfo()
            if (mNetworkInfo != null) {
                handler.post(Runnable { syncDB() })
                userids = HttpApi.getUserIds(preferences.area?:"1")
            }else{
                userids = PaFaceApi.getUserIds(preferences.area)
            }
            Log.e("TestFuel","userids:"+userids.toString())
            if (userids.isEmpty()){
                userids = PaFaceApi.getUserIds(preferences.area)
            }
            //val userids = HttpApi.getUserIds(preferences.area?:"1")
            userList = PaFaceApi.convertId2User("1",userids)
            if (police_btn.isEnabled)
                setListViewData(PaFaceApi.getCriminals(userList))
            else if (criminal_btn.isEnabled)
                setListViewData(PaFaceApi.getPolices(userList))
            else
                setListViewData(userList)
        }catch (e:Exception){
            Log.e("TestFuel",e.toString())
        }


    }
    private fun addListener() {
        adapter!!.setOnItemClickLitsener(object : OnItemClickListener {

            override fun onItemClick(view: View, position: Int) {
                Log.e("TestFuel","OnItemClick!")
                val userList = adapter!!.getUserList()
                if (userList.size > position) {
                    val user = userList[position]
                    val userPlus = PaFaceApi.retUserPlus(user)
                    startActivityForResult<RegActivity>(1,
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
        private var newUsers: MutableList<User> = mutableListOf()


        private var mOnItemClickListener: OnItemClickListener? = null

        inner class UserAdapterCallBack(old:MutableList<User>,new:MutableList<User>) : DiffUtil.Callback(){
            private var oldDatas: MutableList<User> = mutableListOf()
            private var newDatas: MutableList<User> = mutableListOf()
            init {
                this.oldDatas = old
                this.newDatas = new
            }
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldDatas.get(oldItemPosition) == newDatas.get(newItemPosition)
                //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getOldListSize(): Int {
                return oldDatas.size
                //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getNewListSize(): Int {
                return newDatas.size
                //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
                return super.getChangePayload(oldItemPosition, newItemPosition)
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return oldDatas.get(oldItemPosition).equals(newDatas.get(newItemPosition))
                //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }



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
            //val diffResult = DiffUtil.calculateDiff(UserAdapterCallBack(userList, newUsers), true)
            //diffResult.dispatchUpdatesTo(this)


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
                holder.userId.text = "警员警号: " + user.userId
                holder.userInfo.text = "警员姓名：" + user.userInfo
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

    interface OnItemClickListener {

        fun onItemClick(view: View, position: Int)
        fun onItemLongClick(view: View, position: Int)
    }
}