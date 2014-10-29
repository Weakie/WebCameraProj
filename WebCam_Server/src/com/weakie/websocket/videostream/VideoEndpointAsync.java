package com.weakie.websocket.videostream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import com.sun.prism.paint.Stop;

public class VideoEndpointAsync extends Endpoint{

	private static final String GUEST_PREFIX = "GUEST";
	private static final AtomicInteger connectionIds = new AtomicInteger(0);

	private static final short width = 640;
	private static final short height = 480;
	private static final byte[] STREAM_MAGIC_BYTES = new byte[]{'j','s','m','p'};
	
	private final String nickname;
	private EndPointWorker worker;
	
	public VideoEndpointAsync() {
		nickname = GUEST_PREFIX + connectionIds.getAndIncrement();
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		// Set maximum messages size to 500,000 bytes.
		session.setMaxBinaryMessageBufferSize(500000);
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
		this.worker = new EndPointWorker(session);
		threadPool.execute(worker);
		
		String message = String.format("* %s %s", nickname, "has joined.");
		System.out.println(message);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		String message = String.format("* %s %s", nickname, "has disconnected.");
		System.out.println(message);
		this.worker.stop();
	}

	@Override
	public void onError(Session session, Throwable t) {
		System.out.println("Chat Error: " + t.toString());
		this.worker.stop();
	}
	
	
	private static BroadCastQueue queue = new BroadCastQueue(100,width*height*3);
	private static ExecutorService threadPool = Executors.newCachedThreadPool();
	
	public static void broadcast(ByteBuffer data,boolean isLast){
		data.mark();
		queue.write(data);
		data.reset();
	}
	
	private static class EndPointWorker implements Runnable{

		private Session session;
		private long readIndex = 0;
		private volatile boolean isEnd = false;
		private final ByteBuffer buf = ByteBuffer.allocate(640*480*3);
		
		public EndPointWorker(Session session){
			this.session = session;
			this.readIndex = queue.getWriteIndex();
		}
		
		public void stop(){
			this.isEnd = true;
		}
		
		@Override
		public void run() {
			try {
				while (!this.isEnd) {
					this.readIndex++;
					if(this.readIndex+10<queue.getWriteIndex()){
						this.readIndex = queue.getWriteIndex()-1;
					}
					buf.clear();
					queue.read(buf, readIndex);
					buf.flip();
					synchronized (this.session) {
						if (this.session.isOpen()) {
							this.session.getBasicRemote().sendBinary(buf);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
}
