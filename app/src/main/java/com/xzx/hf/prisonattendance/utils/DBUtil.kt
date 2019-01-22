package com.xzx.hf.prisonattendance.utils

import android.util.Log
import com.baidu.aip.api.FaceApi
import com.baidu.aip.entity.Feature
import com.baidu.aip.entity.User
import com.xzx.hf.prisonattendance.entity.CallLog
import com.xzx.hf.prisonattendance.entity.UserPlus
import org.json.JSONObject
import org.litepal.LitePal
import java.lang.Exception
import javax.security.auth.login.LoginException

object DBUtil{
    fun getUpdateTime():Long{
        try {
            val cursor = LitePal.findBySQL("select * from userplus order by updatetime desc LIMIT 1")
            cursor.move(1)
            val updateTime:Long = cursor.getLong(cursor.getColumnIndex("updatetime"))
            return updateTime
        }catch (e:Exception){
            Log.e("DBsync",e.toString())
            return 0L
        }

    }

    fun getDirtyDatas():MutableList<UserPlus>{
        try {
            val flag = "1"
            val objs = mutableListOf<UserPlus>()
            val cursor = LitePal.findBySQL("select * from userplus where dirty = $flag")
            cursor.move(1)
            for (i in 0..((cursor.count)-1)){
                val userPlus = UserPlus()
                userPlus.userId = cursor.getString(cursor.getColumnIndex("userid"))
                userPlus.area = cursor.getString(cursor.getColumnIndex("area"))
                userPlus.userType = cursor.getString(cursor.getColumnIndex("usertype"))
                userPlus.filePath = cursor.getString(cursor.getColumnIndex("filepath"))
                cursor.move(1)
                objs.add(userPlus)
            }
            return objs
        }catch (e:Exception){
            Log.e("DBsync",e.toString())
            return mutableListOf<UserPlus>()
        }
    }

    fun getCallLog(time:Long):MutableList<String>{
        try {
            //val logs = LitePal.where("updatetime > ",time.toString()).order("updatetime").find<CallLog>()
            val logs = mutableListOf<String>()
            val cursor = LitePal.findBySQL("select * from calllog where updatetime > $time order by updatetime")
            cursor.move(1)
            //val logs = cursor.getString(cursor.getColumnIndex("calllog"))

            for (i in 0..((cursor.count)-1)){
                val log = cursor.getString(cursor.getColumnIndex("calllog"))
                cursor.move(1)
                logs.add(log)
            }

            return logs
        }catch (e:Exception){

            Log.e("DBsync",e.toString())
            return mutableListOf<String>()
        }

    }
    fun retUser(json:JSONObject):User{
        try {
            val user = User()
            val feature = Feature()
            val userPlus = UserPlus()
            user.userId = json.getString("user_id")
            user.userInfo = json.getString("user_info")
            user.groupId = "1"//json.getString("group_id")
            //user.ctime = json.getLong("ctime")
            user.updateTime = json.getLong("update_time")

            userPlus.userType = json.getString("usertype")
            userPlus.userId = json.getString("user_id")
            userPlus.updateTime = json.getLong("update_time")
            userPlus.area = json.getString("area")
            userPlus.userStatus = json.getString("userstatus")
            userPlus.userWorkStatus = json.getString("userWorkStatus")
            feature.userId = json.getString("user_id")
            feature.groupId = "1"//json.getString("group_id")
            feature.faceToken = json.getString("face_token")
            feature.setFeature(json.getString("face_token"))
            feature.imageName = json.getString("user_id")
            //feature.ctime = json.getLong("ctime")
            feature.updateTime = json.getLong("update_time")
            userPlus.updateTime = user.updateTime
            userPlus.save()
            user.featureList.add(feature)


            return user

        }catch (e:Exception){
            Log.e("DBSync",e.toString())
            val user = User()
            user.userId = json.getString("user_id")
            FaceApi.getInstance().userDelete(user.userId,"1")
            return user
        }

    }
    fun updateUser(json:JSONObject):Boolean{
        try {
            val user = User()
            val feature = Feature()
            val userPlus = UserPlus()
            user.userId = json.getString("user_id")
            user.userInfo = json.getString("user_info")
            user.groupId = "1"//json.getString("group_id")
            //user.ctime = json.getLong("ctime")
            user.updateTime = json.getLong("update_time")

            userPlus.userType = json.getString("usertype")
            userPlus.userId = json.getString("user_id")
            userPlus.updateTime = json.getLong("update_time")
            userPlus.area = json.getString("area")
            userPlus.userStatus = json.getString("userstatus")
            userPlus.userWorkStatus = json.getString("userWorkStatus")
            feature.userId = json.getString("user_id")
            feature.groupId = "1"//json.getString("group_id")
            feature.faceToken = json.getString("face_token")
            feature.setFeature(json.getString("face_token"))
            feature.imageName = json.getString("user_id")
            //feature.ctime = json.getLong("ctime")
            feature.updateTime = json.getLong("update_time")

            user.featureList.add(feature)
            synchronized(this){
                if (FaceApi.getInstance().userDelete(user.userId,user.groupId)){
                    //val ret = LitePal.deleteAll(UserPlus::class.java,"userid = ?" , user.userId)
                    //Log.e("DBsync","${user.userId}删除$ret 条数据！")
                    /*
                    if (PaFaceApi.deleteFaceImage(user.userId)){
                        Log.e("DBsync","${user.userId}删除人脸文件！")
                    }*/
                }
                if (FaceApi.getInstance().getUserInfo("1",user.userId) == null ){
                    if (FaceApi.getInstance().userAdd(user)) {
                        //userPlus.updateTime = user.updateTime
                        //userPlus.save()
                        Log.e("DBsync","${user.userId}insert success")

                    } else {
                        Log.e("DBsync","${user.userId}insert failure")
                    }
                }
            }

            return true

        }catch (e:Exception){
            Log.e("DBSync",e.toString())
            val user = User()
            user.userId = json.getString("user_id")
            FaceApi.getInstance().userDelete(user.userId,"1")
            return false
        }

    }
}