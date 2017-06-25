package com.zhang.websocket.tmp;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.zhang.websocket.handler.WebsocketServerHandler;

public class SendMessageTest implements Runnable {
	
	private int count = 0;

	@Override
	public void run() {
		Channel channel = null;
		
		while (true) {
			String message = ++count + "";
			Map<String, Channel> channelMap = WebsocketServerHandler.channelMap;
			System.out.println("------ 开始推送消息 ------ " + channelMap.size());
			if (channelMap.size() > 0) {
				Iterator<Entry<String, Channel>> it = channelMap.entrySet().iterator();
				while (it.hasNext()) {
					channel = it.next().getValue();
					try {
						if (channel.isOpen()) {
							channel.writeAndFlush(new TextWebSocketFrame("推送消息到web[" + message + "]"));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
			try {
				Thread.sleep(30 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

}
