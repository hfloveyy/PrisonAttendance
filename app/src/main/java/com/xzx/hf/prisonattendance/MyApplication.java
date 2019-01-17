package com.xzx.hf.prisonattendance;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.xzx.hf.prisonattendance.utils.CrashHandler;
import org.litepal.LitePal;

public class MyApplication extends Application{

    private static MyApplication mInstance;
    private static Context context;
    private long netUnavailableTime = 0;
    private int detectStatus = DETECT_IDEL;
    private String taskName = "";
    public static final int DETECT_IDEL = 0;
    public static final int DETECT_RUNNING = 1;
    private boolean beginer = false;
    private static Handler mHandler = new Handler(Looper.getMainLooper());
    public synchronized static MyApplication getInstance(){
        return mInstance;
    }
    public static void runUITask(Runnable run) {
        mHandler.post(run);
    }
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
        mInstance = this;
        context = getApplicationContext();
        LitePal.initialize(this);
        CrashHandler.getInstance().init(this);
    }

    public static Context getContext(){
        return context;
    }
}
