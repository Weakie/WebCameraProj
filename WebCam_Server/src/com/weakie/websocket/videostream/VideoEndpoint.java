package com.weakie.websocket.videostream;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

public class VideoEndpoint extends Endpoint {

	private static final String GUEST_PREFIX = "Guest";
	private static final AtomicInteger connectionIds = new AtomicInteger(0);
	private static final Set<VideoEndpoint> connections = new CopyOnWriteArraySet<VideoEndpoint>();

	private static final short width = 640;
	private static final short height = 480;
	private static final byte[] STREAM_MAGIC_BYTES = new byte[]{'j','s','m','p'};
	
	private final String nickname;
	private Session session;

	public VideoEndpoint() {
		nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		this.session = session;
		// Set maximum messages size to 10.000 bytes.
		//session.setMaxTextMessageBufferSize(10000);
		session.setMaxBinaryMessageBufferSize(500000);
		connections.add(this);
		
		// Send head data while open
		try {
			ByteBuffer headData = ByteBuffer.allocate(8);
			headData.clear();
			headData.put(STREAM_MAGIC_BYTES);
			headData.putShort(width);
			headData.putShort(height);
			headData.flip();
			session.getBasicRemote().sendBinary(headData);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		String message = String.format("* %s %s", nickname, "has joined.");
		System.out.println(message);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		connections.remove(this);
		String message = String.format("* %s %s", nickname, "has disconnected.");
		System.out.println(message);
	}

	@Override
	public void onError(Session session, Throwable t) {
		System.out.println("Chat Error: " + t.toString());
	}
	
	public static void broadcast(ByteBuffer data,boolean isLast){
		data.mark();
		for(VideoEndpoint client:connections){
			try{
				synchronized(client){
					data.reset();
					client.session.getBasicRemote().sendBinary(data);
				}
			}catch(Exception e){
				e.printStackTrace();
				connections.remove(e);
				try{
					client.session.close();
				}catch(Exception e2){
					
				}
			}
		}
	}

}
