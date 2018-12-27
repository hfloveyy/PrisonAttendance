package com.xzx.hf.prisonattendance.netty

interface NettyListener {


    /**
     * 当接收到系统消息
     */
    fun onMessageResponse(msg: Any)

    /**
     * 当服务状态发生变化时触发
     */
    fun onServiceStatusConnectChanged(statusCode: Int)

    companion object {

        val STATUS_CONNECT_SUCCESS: Int = 1

        val STATUS_CONNECT_CLOSED: Int = 0

        val STATUS_CONNECT_ERROR: Int = 2
    }
}