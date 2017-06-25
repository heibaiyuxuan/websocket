package com.zhang.websocket.bootstrap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;

import com.zhang.websocket.handler.WebsocketServerHandler;

public class WebsocketServer {

	public void startServer(int port) {
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		
		try {
			ServerBootstrap serverBoot = new ServerBootstrap();
			serverBoot.group(bossGroup, workerGroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {

					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline pipeline = ch.pipeline();
						// 将请求和响应消息编解码为HTTP消息
						pipeline.addLast("http-codec", new HttpServerCodec());
						// HTTP消息粘包拆包处理
						pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
						// 支持WebSocket通信
						pipeline.addLast("http-chunked", new ChunkedWriteHandler());
						// WebSocket处理Handler
						pipeline.addLast("websocket-handler", new WebsocketServerHandler());
					}
				});
			
			Channel channel = serverBoot.bind(port).sync().channel();
			System.out.println("Web socket server started at port [" + port + "]");
			
			channel.closeFuture().sync();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
}
