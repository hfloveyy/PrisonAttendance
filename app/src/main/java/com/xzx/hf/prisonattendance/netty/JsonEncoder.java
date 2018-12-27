package com.xzx.hf.prisonattendance.netty;


import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import io.netty.util.CharsetUtil;
import org.json.JSONObject;

public class JsonEncoder extends MessageToByteEncoder<String>{

	@Override
	protected void encode(ChannelHandlerContext arg0, String json,
			ByteBuf buf) throws Exception {
		
        String string = json.toString();
        
        byte[] sb = string.getBytes(CharsetUtil.UTF_8);
//        buf.writeInt(sb.length);
        buf.writeBytes(sb);
		
	}





}
