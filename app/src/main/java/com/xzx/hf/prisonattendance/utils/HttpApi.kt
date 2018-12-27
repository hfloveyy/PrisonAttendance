package com.xzx.hf.prisonattendance.utils

import android.content.Context

import android.opengl.Visibility
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.baidu.aip.entity.Feature
import com.baidu.aip.entity.User
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.DataPart
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.coroutines.awaitStringResponse
import com.github.kittinunf.fuel.coroutines.awaitStringResponseResult
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import com.github.salomonbrys.kotson.*
import com.google.gson.JsonObject
import com.xzx.hf.prisonattendance.R
import com.xzx.hf.prisonattendance.entity.UserPlus
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream




object HttpApi{
    init{
        Log.e("TestFuel","Initializing with object:$this")
    }
    fun getData(){
        "/get"
            .httpGet()
            .responseString { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                    }
                    is Result.Success -> {
                        val data = result.get()
                        Log.e("TestFuel",data.toString())
                    }
                }
            }
    }
    fun postData(url:String,list: List<Pair<String, Any?>>,filesDir: String){
        url
            .httpPost(list)
            .responseString { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                        Log.e("TestFuel", ex.toString())
                    }
                    is Result.Success -> {
                        val data = result.get()
                        Log.e("TestFuel", data.toString())
                        httpUpload(filesDir)
                    }
                }
            }
    }
    fun regUser(user: User,feature: Feature,userPlus: UserPlus,filesDir: String):String{
        val dataJson: JsonObject = jsonObject(
            "user_id" to user.userId,
            "user_info" to user.userInfo,
            //"group_id" to user.groupId,
            "ctime" to user.ctime,
            "update_time" to user.updateTime,
            "face_token" to feature.faceToken,
            "group_id" to userPlus.area,
            //"image_name" to feature.imageName,
            "usertype" to userPlus.userType
            //"userstatus" to userPlus.userStatus
        )
        val data = dataJson.toString()
        val ret =  postDataSync("/add_user", data,filesDir,user.userId)
        Log.e("TestFuel",ret)
        return ret
    }
    fun uploadDB(context:Context){
        val packageName = context.packageName
        val DB_PATH = "/data/data/$packageName/databases/"
        Log.e("TestFuel",DB_PATH)
        Fuel.upload("/post")
            .source { _, _ ->
                // create random file with some non-sense string
                val file = File(DB_PATH, "face.db")
                file
            }
            .progress { writtenBytes, totalBytes ->
                Log.v("TestFuel", "Upload: ${writtenBytes.toFloat() / totalBytes.toFloat()}")
            }
            .also { Log.d("TestFuel", it.toString()) }
            .responseString { _, _, result ->
                val (data, error) = result
                if (error == null) {
                    Log.e("TestFuel",data.toString())
                    //do something when success
                    Toast.makeText(
                        context, "上传face.db成功!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.e("TestFuel",error.toString())
                    //error handling
                } }
        Fuel.upload("/post")
            .source { _, _ ->
                // create random file with some non-sense string
                val file = File(DB_PATH, "faceplus.db")
                file
            }
            .progress { writtenBytes, totalBytes ->
                Log.v("TestFuel", "Upload: ${writtenBytes.toFloat() / totalBytes.toFloat()}")
            }
            .also { Log.d("TestFuel", it.toString()) }
            .responseString { _, _, result ->
                val (data, error) = result
                if (error == null) {
                    Log.e("TestFuel",data.toString())
                    //do something when success
                    Toast.makeText(
                        context, "上传faceplus.db成功!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Log.e("TestFuel",error.toString())
                    //error handling
                } }
    }

    private fun httpUpload(filesDir:String){

        Fuel.upload("/post")
            .source { _, _ ->
                // create random file with some non-sense string
                val file = File(filesDir)
                file
            }
            .progress { writtenBytes, totalBytes ->
                Log.e("TestFuel", "Upload: ${writtenBytes.toFloat() / totalBytes.toFloat()}")
            }
            //.also { Log.d("TestFuel", it.toString()) }
            .responseString { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        val ex = result.getException()
                    }
                    is Result.Success -> {
                        val data = result.get()
                        //Log.e("TestFuel", data.toString())
                    }
                }
            }
    }
    private fun httpUploadFile(filesDir: String,userId:String):String{
        var ret = ""
        var json = JSONObject()
        val img = "data:image/jpeg;base64," + fileToBase64(filesDir)
        Log.e("TestFuel",img)
        json.put("userid",userId)
        json.put("imageurl",img)
        runBlocking {
            val (request, response, result) = Fuel.post("/uploadphoto")
                .header("Content-Type" to "application/json")
                .body(json.toString())
                .requestProgress { readBytes, totalBytes ->
                    val progress = readBytes.toFloat() / totalBytes.toFloat() * 100
                    Log.e("TestFuel","Bytes uploaded $readBytes / $totalBytes ($progress %)")
                }
                //.also { Log.d("TestFuel", it.toString()) }
                .awaitStringResponseResult()
            ret = result.component1().toString()
        }
        return ret

    }
    private fun httpUploadSync(filesDir: String):String{
        var ret = ""
        runBlocking {
            val (request, response, result) = Fuel.upload("/uploadphoto")

                .source { _, _ ->
                    // create random file with some non-sense string
                    val file = File(filesDir)
                    file
                }

                .progress { writtenBytes, totalBytes ->
                    Log.e("TestFuel", "Upload: ${writtenBytes.toFloat() / totalBytes.toFloat()}")


                }
                //.also { Log.d("TestFuel", it.toString()) }
                .awaitStringResponseResult()
            ret = result.component1().toString()
        }
        return ret

    }
    fun postDataSync(url:String,data:String,filesDir: String,userId: String):String{
        var ret:String = ""
        runBlocking {

            val (request, response, result) = url.httpPost().body(data)
                .header("Content-Type" to "application/json").awaitStringResponseResult()
            //Log.e("TestFuel",result.toString())
            var postRet = result.component1()
            Log.e("TestFuel",postRet)
            val json = JSONObject(postRet)
            val flag = json.get("ret_code")
            ret = json.getString("msg")
            Log.e("TestFuel",ret)

            if (flag == "1"){
                var jsonUploadfile = JSONObject(httpUploadFile(filesDir,userId))
                Log.e("TestFuel",jsonUploadfile.toString())
                //val flagUploadfile = jsonUploadfile.get("ret_code")
                //ret = jsonUploadfile.get("msg").toString()
            }

        }
        return ret


    }
    fun postDataSync(data:String):String{
        var ret:String = ""

        runBlocking {
            try {

                val (request, response, result) = "/callname".httpPost().body(data).header("Content-Type" to "application/json").awaitStringResponseResult()
                var postRet = result.component1()
                Log.e("TestFuel",postRet)
                //val json = JSONObject(postRet)
                //Log.e("TestFuel",json.toString())
                //val flag = json.get("ret_code")
                //ret = json.get("msg").toString()
            }catch (e:Exception){
                Log.e("TestFuel","Exception:$e")
            }

        }
        return ret


    }
    fun postDataSync(data:String,url: String):String?{
        var ret:String? = ""
        Log.e("TestFuel",data)
        runBlocking {
            try {

                val (request, response, result) = url.httpPost().body(data).header("Content-Type" to "application/json").awaitStringResponseResult()
                ret = result.component1()
                Log.e("TestFuel",ret)

            }catch (e:Exception){
                Log.e("TestFuel","Exception:$e")
            }

        }
        return ret
    }

    fun syncDB(){

        val url = "/syncdb"
        runBlocking {


            val (request, response, result) = url.httpGet(listOf("updatetime" to DBUtil.getUpdateTime()))
                                                //.header("Content-Type" to "application/json")
                                                .awaitStringResponseResult()
            val ret = response.statusCode
            Log.e("TestFuel",ret.toString())
            if (ret == 200){
                val data = result.component1()
                Log.e("TestFuel",data)
                val jsonObject = JSONObject(data)
                val ret_code = jsonObject.getString("ret_code")
                val jsonArray = jsonObject.getJSONArray("userlist")
                for (i in 0..(jsonArray.length()-1)){
                    var obj:JSONObject = jsonArray.get(i) as JSONObject
                    Log.e("TestFuel",obj.toString())
                    val ret = DBUtil.updateUser(obj)
                    if (ret){

                        Log.e("DBsync","第$i 条记录插入成功！")
                    }
                }
            }else{
                Log.e("DBSync","网络连接失败,未更新数据库。")
            }

        }
    }

    fun getResult(taskName:String):Boolean{
        val url = "/result"
        var flag = false
        runBlocking {
            val (request, response, result) = url.httpGet(listOf("taskname" to taskName))
                .header("Content-Type" to "application/json")
                .awaitStringResponseResult()
            val ret = response.statusCode
            if (ret == 200){
                val data = result.component1()
                Log.e("Result",data)
                val jsonObject = JSONObject(data)
                val ret_code = jsonObject.getString("ret_code")
                val jsonArray = jsonObject.getJSONArray("userlist")
                for (i in 0..(jsonArray.length()-1)){
                    var obj:JSONObject = jsonArray.get(i) as JSONObject

                }
                flag = true
            }else{
                Log.e("Result","网络连接失败,未获取到点名结果。")
                flag = false
            }
        }
        return flag
    }



    @Throws(Exception::class)
    fun fileToBase64(path: String): String {
        val file = File(path)
        val inputFile = FileInputStream(file)
        val buffer = ByteArray(file.length().toInt())
        inputFile.read(buffer)
        inputFile.close()
        val str = Base64.encodeToString(buffer, Base64.NO_WRAP)
        //Log.e("TestFuel",str)
        return str
    }

}