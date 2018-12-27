package com.xzx.hf.prisonattendance.netty


import android.os.SystemClock
import android.util.Log

import java.util.concurrent.TimeUnit

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.LineBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.CharsetUtil
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.json.JsonObjectDecoder


class NettyClient
//    private ScheduledExecutorService mScheduledExecutorService;

    (var host: String, var tcp_port: Int) {

    private var group: EventLoopGroup? = null

    private var listener: NettyListener? = null

    private var channel: Channel? = null
    private var reconnectNum = Integer.MAX_VALUE

    var connectStatus = false

    private var isNeedReconnect = true
    private var isConnecting = false
    private var reconnectIntervalTime: Long = 5000

    fun connect() {

        if (isConnecting) {
            return
        }
        val clientThread = object : Thread("client-Netty") {
            override fun run() {
                super.run()
                isNeedReconnect = true
                reconnectNum = Integer.MAX_VALUE
                connectServer()
            }
        }
        clientThread.start()
    }


    private fun connectServer() {
        synchronized(this@NettyClient) {
            var channelFuture: ChannelFuture? = null
            if (!connectStatus) {
                isConnecting = true
                group = NioEventLoopGroup()
                val bootstrap = Bootstrap().group(group!!)
                    .option(ChannelOption.TCP_NODELAY, true)//屏蔽Nagle算法试图
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .channel(NioSocketChannel::class.java)
                    .handler(object : ChannelInitializer<SocketChannel>() { // 5
                        @Throws(Exception::class)
                        public override fun initChannel(ch: SocketChannel) {
                            ch.pipeline().addLast(
                                "ping",
                                IdleStateHandler(0, 5, 0, TimeUnit.SECONDS)
                            )//5s未发送数据，回调userEventTriggered
                            //ch.pipeline().addLast(JsonEncoder())
                            ch.pipeline().addLast(StringEncoder(CharsetUtil.UTF_8))
                            //ch.pipeline().addLast(LineBasedFrameDecoder(2048))//黏包处理
                            //ch.pipeline().addLast(StringDecoder(CharsetUtil.UTF_8))
                            //ch.pipeline().addLast(JsonDecoder())
                            ch.pipeline().addLast(JsonObjectDecoder())
                            ch.pipeline().addLast(NettyClientHandler(listener))
                        }
                    })

                try {
                    channelFuture = bootstrap.connect(host, tcp_port).addListener ( object :ChannelFutureListener{
                        override fun operationComplete(channelFuture:ChannelFuture ){
                            if (channelFuture.isSuccess) {
                                Log.e(TAG, "连接成功")
                                connectStatus = true
                                channel = channelFuture.channel()
                            } else {
                                Log.e(TAG, "连接失败")
                                connectStatus = false
                            }
                            isConnecting = false
                    }}).sync()

                    // Wait until the connection is closed.
                    channelFuture!!.channel().closeFuture().sync()
                    Log.e(TAG, " 断开连接")
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    connectStatus = false
                    listener!!.onServiceStatusConnectChanged(NettyListener.STATUS_CONNECT_CLOSED)
                    if (null != channelFuture) {
                        if (channelFuture.channel() != null && channelFuture.channel().isOpen) {
                            channelFuture.channel().close()
                        }
                    }
                    group!!.shutdownGracefully()
                    reconnect()
                }
            }
        }
    }


    fun disconnect() {
        Log.e(TAG, "disconnect")
        isNeedReconnect = false
        group!!.shutdownGracefully()
    }

    fun reconnect() {
        Log.e(TAG, "reconnect")
        if (isNeedReconnect && reconnectNum > 0 && !connectStatus) {
            reconnectNum--
            SystemClock.sleep(reconnectIntervalTime)
            if (isNeedReconnect && reconnectNum > 0 && !connectStatus) {
                Log.e(TAG, "重新连接")
                connectServer()
            }
        }
    }

    fun sendMsgToServer(data: String, listener: ChannelFutureListener): Boolean {
        Log.e("NettySendTo",data)
        val flag = channel != null && connectStatus
        if (flag) {
            //			ByteBuf buf = Unpooled.copiedBuffer(data);
            //            ByteBuf byteBuf = Unpooled.copiedBuffer(data + System.getProperty("line.separator"), //2
            //                    CharsetUtil.UTF_8);
            //channel!!.writeAndFlush(data + System.getProperty("line.separator")).addListener(listener)
            channel!!.writeAndFlush(data).addListener(listener)
        }
        return flag
    }

    fun sendMsgToServer(data: ByteArray, listener: ChannelFutureListener): Boolean {
        val flag = channel != null && connectStatus
        if (flag) {
            val buf = Unpooled.copiedBuffer(data)
            channel!!.writeAndFlush(buf).addListener(listener)
        }
        return flag
    }

    fun setReconnectNum(reconnectNum: Int) {
        this.reconnectNum = reconnectNum
    }

    fun setReconnectIntervalTime(reconnectIntervalTime: Long) {
        this.reconnectIntervalTime = reconnectIntervalTime
    }

    fun setListener(listener: NettyListener) {
        this.listener = listener
    }

    companion object {
        private val TAG = "NettyClient"


        private val CONNECT_TIMEOUT_MILLIS = 5000
    }

}
