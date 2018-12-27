package com.xzx.hf.prisonattendance.netty;

import android.util.Log;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

import io.netty.util.CharsetUtil;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonDecoder extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in,
			List<Object> out) throws Exception {
		// System.out.println("decode:"+in.capacity());
		// System.out.println("decode:"+in.readableBytes());
		if (in.readableBytes() < 4) {
			return;// (1)
		}

		// int dataLength = in.getInt(in.readerIndex());
		// if (in.readableBytes() < dataLength) {
		// return;// (2)
		// }
		//
		// in.skipBytes(4);// (3)
		int dataLength = in.readableBytes();

		byte[] code = new byte[dataLength];
		in.readBytes(code);
		String string = new String(code, CharsetUtil.UTF_8);
		//Log.e("Netty","received:" + string);

		// received:###@checktime@2015-10-22
		// 16:14:26@$$$###@recserverstate@0@$$${"op":"checktime","strtime":"20151022161426"}

		try {

			if (string.indexOf("{") > -1) {
				string = string.substring(string.indexOf("{"));
				out.add(new JSONObject(string));
			} else {
				Log.e("Netty","数据错误");
			}
		} catch (JSONException e) {
			Log.e("Netty",e.toString());
			e.printStackTrace();
		}

	}

}
