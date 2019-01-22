package com.xzx.hf.prisonattendance.entity;

import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

public class UserPlus extends LitePalSupport {
    private String userType = "";
    private Long updateTime = 0L;
    private String area = "";



    @Column(unique = true, defaultValue = "unknown")
    private String userId = "";
    private String userStatus = "1";



    private String userWorkStatus = "1";

    private String dirty = "0";

    private String filePath = "";


    public String getUserWorkStatus() {
        return userWorkStatus;
    }

    public void setUserWorkStatus(String userWorkStatus) {
        this.userWorkStatus = userWorkStatus;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getDirty() {
        return dirty;
    }

    public void setDirty(String dirty) {
        this.dirty = dirty;
    }

    public String getUserStatus() {
        return userStatus;
    }

    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }



    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }
}
