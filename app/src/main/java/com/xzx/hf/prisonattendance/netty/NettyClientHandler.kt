package com.xzx.hf.prisonattendance.netty


import android.util.Log
import io.netty.buffer.ByteBuf

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import io.netty.util.CharsetUtil
import org.json.JSONObject


class NettyClientHandler
//    private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled.unreleasableBuffer(Unpooled.copiedBuffer("Heartbeat"+System.getProperty("line.separator"),
//            CharsetUtil.UTF_8));
//    byte[] requestBody = {(byte) 0xFE, (byte) 0xED, (byte) 0xFE, 5,4, (byte) 0xFF,0x0a};


    (private val listener: NettyListener?) : SimpleChannelInboundHandler<ByteBuf>() {


    @Throws(Exception::class)
    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            if (evt.state() == IdleState.WRITER_IDLE) {
                //ctx.channel().writeAndFlush("Heartbeat" + System.getProperty("line.separator"))
            }
        }
    }

    /**
     * 连接成功
     *
     * @param ctx
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun channelActive(ctx: ChannelHandlerContext) {
        Log.e(TAG, "channelActive")
        //        NettyClient.getInstance().setConnectStatus(true);
        listener!!.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_SUCCESS)
    }

    @Throws(Exception::class)
    override fun channelInactive(ctx: ChannelHandlerContext) {
        Log.e(TAG, "channelInactive")
        //        NettyClient.getInstance().setConnectStatus(false);
        //        listener.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_CLOSED);
        // NettyClient.getInstance().reconnect();
    }

    /**
     * 客户端收到消息
     *
     * @param channelHandlerContext
     * @param byteBuf
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun channelRead0(channelHandlerContext: ChannelHandlerContext, byteBuf: ByteBuf) {
        //Log.e(TAG, "channelRead0")
        //Log.e("Receive","received:" + byteBuf.toString(CharsetUtil.UTF_8))
        listener!!.onMessageResponse(byteBuf.toString(CharsetUtil.UTF_8))
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        // Close the connection when an exception is raised.
        //        NettyClient.getInstance().setConnectStatus(false);
        Log.e(TAG, cause.toString())
        listener!!.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_ERROR)
        cause.printStackTrace()
        ctx.close()
    }

    companion object {

        private val TAG = "NettyClientHandler"
    }
}
