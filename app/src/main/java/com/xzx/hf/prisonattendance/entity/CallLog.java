package com.xzx.hf.prisonattendance.entity;
import org.litepal.crud.LitePalSupport;
public class CallLog extends LitePalSupport{
    private String calllog = "";
    private Long updatetime = 0L;
    private String exceptionlog = "";

    public String getExceptionlog() {
        return exceptionlog;
    }

    public void setExceptionlog(String exceptionlog) {
        this.exceptionlog = exceptionlog;
    }

    public Long getUpdatetime() {
        return updatetime;
    }

    public void setUpdatetime(Long updatetime) {
        this.updatetime = updatetime;
    }


    public String getCalllog() {
        return calllog;
    }

    public void setCalllog(String calllog) {
        this.calllog = calllog;
    }
}
