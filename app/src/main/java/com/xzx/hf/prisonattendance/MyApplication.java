package com.xzx.hf.prisonattendance;

import android.app.Application;

import org.litepal.LitePal;

public class MyApplication extends Application{


    private long netUnavailableTime = 0;
    private int detectStatus = DETECT_IDEL;
    private String taskName = "";
    public static final int DETECT_IDEL = 0;
    public static final int DETECT_RUNNING = 1;
    private boolean beginer = false;

    public boolean isBeginer() {
        return beginer;
    }

    public void setBeginer(boolean beginer) {
        this.beginer = beginer;
    }


    public long getNetUnavailableTime() {
        return netUnavailableTime;
    }

    public void setNetUnavailableTime(long netUnavailableTime) {
        this.netUnavailableTime = netUnavailableTime;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }


    public int getDetectStatus() {
        return detectStatus;
    }

    public void setDetectStatus(int detectStatus) {
        this.detectStatus = detectStatus;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        LitePal.initialize(this);
    }
}
