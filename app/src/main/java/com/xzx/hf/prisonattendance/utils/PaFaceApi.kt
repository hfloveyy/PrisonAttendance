package com.xzx.hf.prisonattendance.utils

import android.graphics.BitmapFactory
import com.baidu.aip.api.FaceApi
import com.baidu.aip.entity.User
import com.baidu.aip.utils.FileUitls
import com.xzx.hf.prisonattendance.entity.UserPlus
import org.litepal.LitePal
import java.io.File

object PaFaceApi{

    fun getCriminals(area:String?):MutableList<String>{
        //var list = FaceApi.getInstance().getUserList(groupId)
        val list = LitePal.where("area = ?",area).find(UserPlus::class.java)
        var criminals = mutableListOf<String>()
        for(userPlus in list!!){
            var user = retUser(userPlus)
            if (!isPolice(user)){
                criminals.add(user.userId)
            }
        }
        return criminals
    }

    fun isPolice(user:User):Boolean{
        val userPlus = LitePal.where("userid = ?",user.userId).find(UserPlus::class.java)
        if (userPlus[0].userType.toInt() == 0){
            return true
        }else{
            return false
        }
    }
    fun retUserPlus(user:User):UserPlus{
        val userPlus = LitePal.where("userid = ?",user.userId).find(UserPlus::class.java)
            return userPlus[0]
    }

    fun retUser(userPlus: UserPlus):User{
        var user = FaceApi.getInstance().getUserInfo("1",userPlus.userId)
        return user
    }

    fun convertId2User(groudId:String,list:MutableList<String>):MutableList<User>{
        val ret_list = mutableListOf<User>()
        for (userId in list){
            var user = FaceApi.getInstance().getUserInfo(groudId,userId)
            if(user!=null)
                if (!isPolice(user))
                    ret_list.add(user)
        }
        return ret_list
    }

    fun deleteFaceImage(imageName:String):Boolean{
        var flag = false
        val faceDir = FileUitls.getFaceDirectory()
        if (faceDir != null && faceDir.exists()) {
            val file = File(faceDir, imageName)
            if (file.exists()) {
                flag =  file.delete()

            }
        }
        return flag
    }
}