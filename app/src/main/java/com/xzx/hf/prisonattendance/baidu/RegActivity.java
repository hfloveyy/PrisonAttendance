/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package com.xzx.hf.prisonattendance.baidu;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import com.bumptech.glide.Glide;
import com.xzx.hf.prisonattendance.utils.DateUtil;
import kotlin.Pair;
import android.view.View;
import android.widget.*;
import com.baidu.aip.ImageFrame;
import com.baidu.aip.api.FaceApi;
import com.baidu.aip.db.DBManager;
import com.baidu.aip.entity.ARGBImg;
import com.baidu.aip.entity.Feature;
import com.baidu.aip.entity.Group;
import com.baidu.aip.entity.User;
import com.baidu.aip.face.FaceCropper;
import com.baidu.aip.face.FaceDetectManager;
import com.baidu.aip.face.FileImageSource;
import com.baidu.aip.manager.FaceDetector;
import com.baidu.aip.manager.FaceSDKManager;
import com.baidu.aip.utils.FeatureUtils;
import com.baidu.aip.utils.FileUitls;
import com.baidu.aip.utils.ImageUtils;
import com.baidu.aip.utils.PreferencesUtil;
import com.baidu.idl.facesdk.FaceInfo;
import com.baidu.idl.facesdk.FaceTracker;
import com.xzx.hf.prisonattendance.R;
import com.xzx.hf.prisonattendance.baidu.utils.GlobalFaceTypeModel;
import com.xzx.hf.prisonattendance.entity.UserPlus;
import com.xzx.hf.prisonattendance.utils.HttpApi;
import com.xzx.hf.prisonattendance.utils.SharedPreferencesUtils;
import org.litepal.LitePal;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 该类提供人脸注册功能，注册的人脸可以通个自动检测和选自相册两种方式获取。
 */

public class RegActivity extends Activity implements View.OnClickListener {


    SharedPreferencesUtils preferences;
    public static final int SOURCE_REG = 1;
    private static final int REQUEST_CODE_PICK_IMAGE = 1000;
    private static final int REQUEST_CODE_AUTO_DETECT = 100;
    private static final int REQUEST_CODE_IDENTIFY = 200;
    private EditText usernameEt;
    private EditText usernumEt;
    // private EditText groupIdEt;
    //private Spinner groupIdSpinner;

    private RadioGroup mTypeRg;
    private Spinner spinnerType;
    private ImageView avatarIv;
    private Button autoDetectBtn;
    private Button fromAlbumButton;
    private Button submitButton;
    private Button regIdentifyBtn;

    // 注册时使用人脸图片路径。
    private String faceImagePath;

    // 从相机识别时使用。
    private FaceDetectManager detectManager;
    private String groupId = "1";
    private String typeId = "0";
    private String area = "";
    private User usertemp;
    String url = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        preferences = new SharedPreferencesUtils(this);
        groupId = preferences.getGroupId();
        area = preferences.getArea();
        super.onCreate(savedInstanceState);
        detectManager = new FaceDetectManager(getApplicationContext());
        setContentView(R.layout.activity_reg);


        regIdentifyBtn = (Button)findViewById(R.id.reg_identify_btn);
        usernameEt = (EditText) findViewById(R.id.username_et);
        usernumEt = (EditText) findViewById(R.id.usernum_et);
        //mTypeRg = (RadioGroup) findViewById(R.id.type_rg);
        avatarIv = (ImageView) findViewById(R.id.avatar_iv);
        autoDetectBtn = (Button) findViewById(R.id.auto_detect_btn);
        fromAlbumButton = (Button) findViewById(R.id.pick_from_album_btn);
        submitButton = (Button) findViewById(R.id.submit_btn);
        submitButton.setVisibility(View.GONE);

        autoDetectBtn.setOnClickListener(this);
        fromAlbumButton.setOnClickListener(this);
        submitButton.setOnClickListener(this);
        regIdentifyBtn.setOnClickListener(this);
        init();
    }

    private void init() {
        SQLiteDatabase db = LitePal.getDatabase();
        Intent intent = getIntent();
        if (intent!=null){
            String userid = intent.getStringExtra("userid");
            String userinfo = intent.getStringExtra("userinfo");
            String usertype = intent.getStringExtra("usertype");
            usernumEt.setText(userid);
            usernameEt.setText(userinfo);
            usernumEt.setEnabled(false);
            usernameEt.setEnabled(false);
            typeId = usertype;
            url = "http://"+preferences.getServerIP()+":8080/Uploads/HeadPicture/" + userid + "/" + userid +".jpg";
            /*
            Log.e("TestFuel",url);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Glide.with(RegActivity.this).load(url).into(avatarIv);
                }
            });*/
        }

    }

    @Override
    public void onClick(View v) {
        if (v == autoDetectBtn) {
            if(usernumEt.getText() == null){
                Toast.makeText(RegActivity.this, "请先填写编号", Toast.LENGTH_LONG).show();
                return;
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest
                    .permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA}, 100);
                return;
            }
            avatarIv.setImageResource(R.drawable.avatar);
            faceImagePath = null;
            int type = PreferencesUtil.getInt(LivenessSettingActivity.TYPE_LIVENSS, LivenessSettingActivity
                    .TYPE_NO_LIVENSS);
            if (type == LivenessSettingActivity.TYPE_NO_LIVENSS || type == LivenessSettingActivity.TYPE_RGB_LIVENSS) {
                Intent intent = new Intent(RegActivity.this, RgbDetectActivity.class);
                intent.putExtra("source", SOURCE_REG);
                intent.putExtra("filename",usernumEt.getText().toString().trim());
                startActivityForResult(intent, REQUEST_CODE_AUTO_DETECT);
            }
        } else if (v == fromAlbumButton) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        100);
                return;
            }
            avatarIv.setImageResource(R.drawable.avatar);
            faceImagePath = null;
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE);

        } else if (v == submitButton) {
            /*
            int type = mTypeRg.getCheckedRadioButtonId();
            if (type == R.id.police_rb){
                typeId = "0";
            }
            if (type == R.id.crimials_rb){
                typeId = "1";
            }*/
            register(faceImagePath);
        } else if (v == regIdentifyBtn) {

            Log.e("DBsync","reg identify");
            if (faceImagePath == null){
                toast("请先检测人脸！");
                return;
            }
            Log.e("DBsync","Reg Identify!" + faceImagePath);
            if(FaceApi.getInstance().userDelete("123456","2")) {
                Log.e("DBsync","success delete");
            }
            usertemp = new User();
            usertemp.setUserId("123456");
            usertemp.setGroupId("2");
            Executors.newSingleThreadExecutor().submit(new Runnable() {

                @Override
                public void run() {
                    ARGBImg argbImg = FeatureUtils.getARGBImgFromPath(faceImagePath);
                    byte[] bytes = new byte[2048];
                    int ret = 0;
                    int type = PreferencesUtil.getInt(GlobalFaceTypeModel.TYPE_MODEL, GlobalFaceTypeModel.RECOGNIZE_LIVE);
                    if (type == GlobalFaceTypeModel.RECOGNIZE_LIVE) {
                        ret = FaceSDKManager.getInstance().getFaceFeature().faceFeature(argbImg, bytes, 50);
                    } else if (type == GlobalFaceTypeModel.RECOGNIZE_ID_PHOTO) {
                        ret = FaceSDKManager.getInstance().getFaceFeature().faceFeatureForIDPhoto(argbImg, bytes, 50);
                    }
                    if (ret == FaceDetector.NO_FACE_DETECTED) {
                        toast("人脸太小（必须大于最小检测人脸minFaceSize），或者人脸角度太大，人脸不是朝上");
                    } else if (ret != -1) {
                        Log.e("DBsync","Feature");
                        Feature feature = new Feature();
                        feature.setGroupId("2");
                        feature.setUserId("123456");
                        feature.setFeature(bytes);
                        usertemp.getFeatureList().add(feature);
                        if (FaceApi.getInstance().userAdd(usertemp)){
                            Log.e("DBsync","add success");
                            Intent intent = new Intent(RegActivity.this,RegIdentifyActivity.class);
                            startActivityForResult(intent,REQUEST_CODE_IDENTIFY);
                        }else {
                            Log.e("DBsync","add failed");
                        }
                    } else {
                        toast("抽取特征失败");
                    }
                }
            });

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_AUTO_DETECT && data != null) {
            usernumEt.setFocusable(false);
            usernumEt.setFocusableInTouchMode(false);
            faceImagePath = data.getStringExtra("file_path");

            Bitmap bitmap = BitmapFactory.decodeFile(faceImagePath);
            avatarIv.setImageBitmap(bitmap);
            //submitButton.setVisibility(View.VISIBLE);
        } else if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
//            String filePath = getRealPathFromURI(uri);
                String filePath = imageUriToFile(uri);
                detect(filePath);
            }
        } else if (requestCode == REQUEST_CODE_IDENTIFY){
            if (resultCode == Activity.RESULT_OK){
                toast("验证成功,可以更新人脸数据！！");
                regIdentifyBtn.setVisibility(View.GONE);
                submitButton.setVisibility(View.VISIBLE);
            }else if(resultCode == Activity.RESULT_CANCELED){
                toast("验证失败,请重新检测人脸！");
            }

        }
    }

    // 从相册检测。
    private void detect(final String filePath) {

        FileImageSource fileImageSource = new FileImageSource();
        fileImageSource.setFilePath(filePath);
        detectManager.setImageSource(fileImageSource);
        detectManager.setUseDetect(true);
        detectManager.setOnFaceDetectListener(new FaceDetectManager.OnFaceDetectListener() {
            @Override
            public void onDetectFace(int status, FaceInfo[] faces, ImageFrame frame) {
                if (faces != null && status != FaceTracker.ErrCode.NO_FACE_DETECTED.ordinal()
                        && status != FaceTracker.ErrCode.UNKNOW_TYPE.ordinal()) {
                    final Bitmap cropBitmap = FaceCropper.getFace(frame.getArgb(), faces[0], frame.getWidth());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            avatarIv.setImageBitmap(cropBitmap);
                        }
                    });

                    // File file = File.createTempFile(UUID.randomUUID().toString() + "", ".jpg");
                    File faceDir = FileUitls.getFaceDirectory();
                    if (faceDir != null) {
                        String imageName = UUID.randomUUID().toString();
                        File file = new File(faceDir, imageName);
                        // 压缩人脸图片至300 * 300，减少网络传输时间
                        ImageUtils.resize(cropBitmap, file, 300, 300);
                        RegActivity.this.faceImagePath = file.getAbsolutePath();
                        submitButton.setVisibility(View.VISIBLE);
                    } else {
                        toast("注册人脸目录未找到");
                    }
                } else {
                    toast("未检测到人脸，可能原因：人脸太小（必须大于最小检测人脸minFaceSize），或者人脸角度太大，人脸不是朝上");
                }
            }
        });
        detectManager.start();
    }

    private String getRealPathFromURI(Uri contentURI) {
        String result = null;
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(contentURI, null, null, null, null);
            if (cursor == null) { // Source is Dropbox or other similar local file path
                result = contentURI.getPath();
            } else {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                result = cursor.getString(idx);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return result;
    }


    public String imageUriToFile(final Uri uri) {
        if (null == uri) {
            return null;
        }
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = getContentResolver().query(uri,
                    new String[]{MediaStore.Images.ImageColumns.DATA},
                    null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }


    private File target;

    private void register(final String filePath) {

        final String username = usernameEt.getText().toString().trim();
        final String usernum = usernumEt.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(RegActivity.this, "姓名不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(usernum)) {
            Toast.makeText(RegActivity.this, "编号不能为空", Toast.LENGTH_SHORT).show();
            return;
        }/*
        Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,4}");
        Matcher matcher = pattern.matcher(username);
        if (!matcher.matches()) {
            Toast.makeText(RegActivity.this, "姓名由汉字组合", Toast.LENGTH_SHORT).show();
            return;
        }
        */
        // final String groupId = groupIdEt.getText().toString().trim();
        if (TextUtils.isEmpty(groupId)) {
            Toast.makeText(RegActivity.this, "分组groupId为空", Toast.LENGTH_SHORT).show();
            return;
        }
        /*
        matcher = pattern.matcher(username);
        if (!matcher.matches()) {
            Toast.makeText(RegActivity.this, "groupId由数字、字母、下划线中的一个或者多个组合", Toast.LENGTH_SHORT).show();
            return;
        }
        /*
         * 用户id（由数字、字母、下划线组成），长度限制128B
         * uid为用户的id,百度对uid不做限制和处理，应该与您的帐号系统中的用户id对应。
         *
         */
        //final String uid = UUID.randomUUID().toString();
        // String uid = 修改为自己用户系统中用户的id;
        if (TextUtils.isEmpty(faceImagePath)) {
            Toast.makeText(RegActivity.this, "人脸文件不存在", Toast.LENGTH_LONG).show();
            return;
        }
        final File file = new File(filePath);
        if (!file.exists()) {
            Toast.makeText(RegActivity.this, "人脸文件不存在", Toast.LENGTH_LONG).show();
            return;
        }
        /*
        if(FaceApi.getInstance().getUserInfo(groupId,usernum)!=null){
            Toast.makeText(RegActivity.this, "用户编号已注册,请更改编号", Toast.LENGTH_LONG).show();
            return;
        }*/

        final User user = new User();
        final UserPlus userPlus = new UserPlus();
        user.setUserId(usernum);
        user.setUserInfo(username);
        user.setGroupId(groupId);
        user.setUpdateTime( System.currentTimeMillis());
        user.setCtime(0);//System.currentTimeMillis());
        userPlus.setUserId(usernum);
        userPlus.setUserType(typeId);
        userPlus.setArea(area);
        userPlus.setUserStatus("1");
        //userPlus.setUpdateTime(DateUtil.INSTANCE.getNowDateTime());
        Executors.newSingleThreadExecutor().submit(new Runnable() {

            @Override
            public void run() {
                ARGBImg argbImg = FeatureUtils.getARGBImgFromPath(filePath);
                byte[] bytes = new byte[2048];
                int ret = 0;
                int type = PreferencesUtil.getInt(GlobalFaceTypeModel.TYPE_MODEL, GlobalFaceTypeModel.RECOGNIZE_LIVE);
                if (type == GlobalFaceTypeModel.RECOGNIZE_LIVE) {
                    ret = FaceSDKManager.getInstance().getFaceFeature().faceFeature(argbImg, bytes, 50);
                } else if (type == GlobalFaceTypeModel.RECOGNIZE_ID_PHOTO) {
                    ret = FaceSDKManager.getInstance().getFaceFeature().faceFeatureForIDPhoto(argbImg, bytes, 50);
                }
                if (ret == FaceDetector.NO_FACE_DETECTED) {
                    toast("人脸太小（必须大于最小检测人脸minFaceSize），或者人脸角度太大，人脸不是朝上");
                } else if (ret != -1) {
                    Feature feature = new Feature();
                    feature.setGroupId(groupId);
                    feature.setUserId(usernum);
                    feature.setFeature(bytes);
                    feature.setImageName(file.getName());
                    feature.setCtime(user.getCtime());
                    feature.setUpdateTime(user.getUpdateTime());
                    user.getFeatureList().add(feature);
                    userPlus.setFilePath(filePath);
//                   target = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/chaixiaogangFeature2");
//                   Utils.saveToFile(target,"feature2.txt",bytes);
                    //String ret_msg = HttpApi.INSTANCE.regUser(user,feature,userPlus,filePath);
                    //String ret_msg = HttpApi.INSTANCE.updateUser(user,feature,userPlus,filePath);
                    //Log.e("TestFuel",ret_msg);
                    //if (ret_msg.equals("success")){

                    if(FaceApi.getInstance().userDelete(user.getUserId(),user.getGroupId())) {
                            if (FaceApi.getInstance().userAdd(user)) {
                                //userPlus.setUpdateTime(user.getUpdateTime());
                                userPlus.setDirty("1");
                                userPlus.save();
                                //HttpApi.INSTANCE.syncDB();
                                toast("更新成功！");
                                finish();
                            } else {

                            }
                    }
                    /*
                    }else {
                        toast(ret_msg);
                    }*/

                } else {
                    toast("抽取特征失败");
                }
            }
        });
    }


    private void toast(final String text) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(RegActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private Handler handler = new Handler(Looper.getMainLooper());
}
