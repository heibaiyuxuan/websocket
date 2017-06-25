package com.zhang.websocket.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebsocketServerHandler extends SimpleChannelInboundHandler<Object> {
	
	static final String PARAM_UID = "uid";
	
	private static final String WEBSOCKET_PATH = "/websocket";
	
	public static Map<String, Channel> channelMap = new ConcurrentHashMap<String, Channel>();
	
	private WebSocketServerHandshaker handshaker;
	
	@Override
	protected void messageReceived(ChannelHandlerContext context, Object msg)
			throws Exception {
		if (msg instanceof FullHttpRequest) {
			handleHttpRequest(context, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			handleWebSocketFrame(context, (WebSocketFrame) msg);
		}
		
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {
		cause.printStackTrace();
		ctx.close();
	}

	private void handleHttpRequest(ChannelHandlerContext context, FullHttpRequest req) {
		// Handle a bad request.
        if (!req.decoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
            sendHttpResponse(context, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        
        // Allow only GET methods.
        if (req.method() != HttpMethod.GET) {
            sendHttpResponse(context, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
            return;
        }

        if ("/favicon.ico".equals(req.uri()) || ("/".equals(req.uri()))) {
            sendHttpResponse(context, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
            return;
        }

        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.uri());
        Map<String, List<String>> parameters = queryStringDecoder.parameters();

        if (parameters.size() == 0 || !parameters.containsKey(PARAM_UID)) {
        	System.out.println("uid参数不可缺省");
            sendHttpResponse(context, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        String uid = parameters.get(PARAM_UID).get(0);
        if (uid == null || uid.length() == 0) {
            System.out.println("uid参数不可缺省");
            sendHttpResponse(context, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }

        // Handshake
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, true);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(context.channel());
        } else {
            ChannelFuture channelFuture = handshaker.handshake(context.channel(), req);

            // 握手成功之后,业务逻辑 注册
            if (channelFuture.isSuccess()) {
            	System.out.println("open websocket connection.");
                channelMap.put(context.channel().id().asShortText(), context.channel());
            }
        }
	}
	
	private void handleWebSocketFrame(ChannelHandlerContext context, WebSocketFrame frame) {
		// 
		if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(context.channel(), (CloseWebSocketFrame) frame.retain());
            System.out.println("close websocket.");
            return;
        }
		
        if (frame instanceof PingWebSocketFrame) {
        	context.channel().write(new PongWebSocketFrame(frame.content().retain()));
        	System.out.println("ping websocket.");
            return;
        }
        // 暂时只支持文本消息
        if (!(frame instanceof TextWebSocketFrame)) {
            throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
        }
        
        String message = ((TextWebSocketFrame) frame).text();
        System.out.println("server端接收到消息[" + message + "]");
        
        context.channel().writeAndFlush(new TextWebSocketFrame("server端接收到消息[" + message + "]"));
	}
	
	private static String getWebSocketLocation(FullHttpRequest req) {
//        String location = req.headers().get(HttpHeaderNames.HOST) + WEBSOCKET_PATH;
        String location = req.headers().get(HttpHeaderNames.HOST) + "";
        return "ws://" + location;
    }
	
	private static void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        if (res.status().code() != HttpResponseStatus.OK.code()) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpHeaderUtil.setContentLength(res, res.content().readableBytes());
        }

        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpHeaderUtil.isKeepAlive(req) || res.status().code() != HttpResponseStatus.OK.code()) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
	
}
