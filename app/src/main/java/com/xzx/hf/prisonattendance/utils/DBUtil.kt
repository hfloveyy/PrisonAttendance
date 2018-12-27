package com.xzx.hf.prisonattendance.utils

import android.util.Log
import com.baidu.aip.api.FaceApi
import com.baidu.aip.entity.Feature
import com.baidu.aip.entity.User
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
            Log.e("DBsync","DB not init!")
            return 0L
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
            //userPlus.userStatus = json.getString("userstatus")
            feature.userId = json.getString("user_id")
            feature.groupId = "1"//json.getString("group_id")
            feature.faceToken = json.getString("face_token")
            feature.setFeature(json.getString("face_token"))
            feature.imageName = json.getString("user_id")
            //feature.ctime = json.getLong("ctime")
            feature.updateTime = json.getLong("update_time")

            user.featureList.add(feature)
            if (FaceApi.getInstance().userDelete(user.userId,user.groupId)){
                val ret = LitePal.deleteAll(UserPlus::class.java,"userid = ?" , user.userId)
                Log.e("DBsync","${user.userId}删除$ret 条数据！")
                if (PaFaceApi.deleteFaceImage(user.userId)){
                    Log.e("DBsync","${user.userId}删除人脸文件！")
                }
            }
            if (FaceApi.getInstance().userAdd(user)) {
                userPlus.updateTime = user.updateTime
                userPlus.save()
                Log.e("DBsync","insert success")
                return true
            } else {
                Log.e("DBsync","insert failure")
                return false
            }
        }catch (e:Exception){
            Log.e("DBSync",e.toString())
            return false
        }

    }
}