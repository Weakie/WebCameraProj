package com.weakie.stream.process;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.weakie.encoder.mpeg.jni.InvalidByteBufferSizeException;
import com.weakie.encoder.mpeg.jni.InvalidPointerException;
import com.weakie.encoder.mpeg.jni.MPEGEncoder;
import com.weakie.websocket.videostream.VideoEndpoint;

class EncoderAndDispatcher implements Runnable {
	
	private volatile boolean isEnd = false;
	private ByteBuffer bufout = ByteBuffer.allocateDirect(640*480*4);
	private ByteBuffer bufwrite = ByteBuffer.allocate(640*480*4);
	private ArrayBlockingQueue<ByteBuffer> bufQueue = new ArrayBlockingQueue<ByteBuffer>(10);
	
	public void setIsEnd(boolean isEnd){
		this.isEnd = isEnd;
	}
	
	@Override
	public void run() {
		MPEGEncoder encoder = MPEGEncoder.createAndInitInstance(640, 480, 640*480*3, MPEGEncoder.AV_PIX_FMT_BGR24);
		try{
			ByteBuffer bufin = null;
			while(!isEnd){
				long time0 = System.currentTimeMillis();
				bufin = this.bufQueue.take();
				long time1 = System.currentTimeMillis();
				long size = encoder.encodeData(bufin, bufout);
				if(size <= -1){
					continue;
				}
				long time2 = System.currentTimeMillis();
				this.broadcast(size);
				long time3 = System.currentTimeMillis();
				System.out.println("times in Encoder: "+(time3-time2)+" "+(time2-time1)+" "+(time1-time0));
			}
		} catch (InvalidByteBufferSizeException e) {
			e.printStackTrace();
		} catch (InvalidPointerException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}finally{
			encoder.destroy();
		}
	}
	
	private void broadcast(long size){
		bufout.position((int)size);
		bufout.flip();
		bufwrite.clear();
		bufwrite.put(bufout);
		bufwrite.flip();
		bufout.clear();
		VideoEndpoint.broadcast(bufwrite, false);
	}
	
	private  ImageUpdateListener listener = new ImageUpdateListener(){

		@Override
		public void updateVideoImage(ByteBuffer image) {
			try {
				boolean success = bufQueue.offer(image, 10, TimeUnit.MILLISECONDS);
				if(!success){
					bufQueue.clear();
					bufQueue.add(image);
				}
				System.out.println(bufQueue.size());
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	};
	
	public ImageUpdateListener getListener(){
		return this.listener;
	}
}
