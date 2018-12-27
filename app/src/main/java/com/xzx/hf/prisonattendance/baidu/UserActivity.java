/*
 * Copyright (C) 2018 Baidu, Inc. All Rights Reserved.
 */
package com.xzx.hf.prisonattendance.baidu;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import com.baidu.aip.api.FaceApi;
import com.baidu.aip.entity.Feature;
import com.baidu.aip.entity.User;
import com.baidu.aip.utils.FileUitls;
import com.bumptech.glide.Glide;
import com.xzx.hf.prisonattendance.R;
import com.xzx.hf.prisonattendance.utils.SharedPreferencesUtils;

import java.io.File;
import java.util.List;

public class UserActivity extends Activity {

    private TextView userIdTv;
    private TextView userInfoTv;
    private TextView userTypeTv;
    private TextView areaTv;
    private TextView featureTv;
    private ImageView faceIv;
    SharedPreferencesUtils preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        preferences = new SharedPreferencesUtils(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        findView();
        display();

    }

    private void findView() {
        userIdTv = (TextView) findViewById(R.id.user_id_tv);
        userInfoTv = (TextView) findViewById(R.id.user_info_tv);
        userTypeTv = (TextView) findViewById(R.id.user_type_tv);
        areaTv = (TextView) findViewById(R.id.area_tv);
        featureTv = (TextView) findViewById(R.id.feature_tv);
        faceIv = (ImageView) findViewById(R.id.face_iv);
    }

    private void display() {
        Intent intent = getIntent();
        if (intent != null) {
            String userId = intent.getStringExtra("user_id");
            String userInfo = intent.getStringExtra("user_info");
            String area = intent.getStringExtra("area");
            //String userType = intent.getStringExtra("user_type");

            userIdTv.setText("编号:" + userId);
            userInfoTv.setText("姓名:" + userInfo);
            areaTv.setText("监区:" + area);
            //userTypeTv.setText(userType);
            User user = FaceApi.getInstance().getUserInfo("1", userId);
            String url = "http://"+ preferences.getServerIP()+":8080/Uploads/HeadPicture/" + user.getUserId() + "/" +user.getUserId() + ".jpg";
            Glide.with(UserActivity.this).load(url).into(faceIv);
            /*
            List<Feature> featureList = user.getFeatureList();
            if (featureList != null && featureList.size() > 0) {
                // featureTv.setText(new String(featureList.get(0).getFeature()));
                File faceDir = FileUitls.getFaceDirectory();
                if (faceDir != null && faceDir.exists()) {
                    File file = new File(faceDir, featureList.get(0).getImageName());
                    if (file != null && file.exists()) {
                        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                        faceIv.setImageBitmap(bitmap);
                    }
                }
            }*/
        }
    }
}
