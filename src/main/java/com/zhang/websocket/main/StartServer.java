package com.zhang.websocket.main;

import java.io.IOException;
import java.util.Properties;

import com.zhang.websocket.bootstrap.WebsocketServer;
import com.zhang.websocket.tmp.SendMessageTest;

public class StartServer {

	private static Properties properties;
	
	public static void main(String[] args) {
		properties = new Properties();
		try {
			properties.load(StartServer.class.getClassLoader().getResourceAsStream("config.properties"));
			
			int port = Integer.valueOf(properties.getProperty("server.port", "9090"));
			
			new Thread(new SendMessageTest()).start();
			
			WebsocketServer websocketServer = new WebsocketServer();
			websocketServer.startServer(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
