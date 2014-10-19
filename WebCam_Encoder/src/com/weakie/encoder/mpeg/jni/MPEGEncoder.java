package com.weakie.encoder.mpeg.jni;

import java.nio.ByteBuffer;

public class MPEGEncoder {
	
	//format value refer to FFMPEG
	public static final int AV_PIX_FMT_RGB24 = 2;
	public static final int AV_PIX_FMT_BGR24 = 3;
	
	public static MPEGEncoder createAndInitInstance(int width,int height,int frameSize,int srcFmt){
		try {
			return new MPEGEncoder(width,height,frameSize,srcFmt);
		} catch (InvalidPointerException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private boolean isAlive = false;
	private final long ptr;
	private final int height,width,frameSize;
	
	private MPEGEncoder(int width,int height,int frameSize,int srcFmt) throws InvalidPointerException{
		this.ptr = MPEGEncoder.init(width, height,frameSize,srcFmt);
		if(this.ptr != -1){
			this.isAlive = true;
		}else{
			throw new InvalidPointerException("Initialized new MPEGEncoder failed.");
		}
		this.width = width;
		this.height = height;
		this.frameSize = frameSize;
	}
	
	public synchronized long encodeData(ByteBuffer in,ByteBuffer out) throws InvalidByteBufferSizeException, InvalidPointerException{
		//check object is alive
		if(!this.isAlive){
			throw new InvalidPointerException("MPEGEncoder has already been destroyed. Point data:"+this.ptr);
		}
		//check size
		if(in.capacity()<frameSize || out.capacity()<frameSize){
			throw new InvalidByteBufferSizeException(
					"Invalid buf size: in size "+in.capacity()+" out size "+out.capacity()+" frame size "+ frameSize);
		}
		//encode
		return MPEGEncoder.encode(ptr, in, out);
	}
	
	public synchronized void destroy(){
		//destroy data
		if(this.isAlive){
			this.isAlive = false;
			MPEGEncoder.destroy(ptr);
		}
	}
	
	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public int getFrameSize() {
		return frameSize;
	}


	//native interface
	static{
		System.loadLibrary("MPEGEncoder");
		MPEGEncoder.register();
	}
	
	private static native void register();
	/**
	 * init the encoder data
	 * @param width
	 * @param height
	 * @return -1 fail, long address while success
	 */
	private static native long init(int width,int height,int frameSize,int srcFmt);
	/**
	 * encode data of image in the in buffer
	 * @param ptr
	 * @param in
	 * @param out
	 * @return return value < 0 if fail, data size if success.
	 */
	private static native int encode(long ptr,ByteBuffer in,ByteBuffer out);
	/**
	 * destroy this object
	 * @param ptr
	 */
	private static native void destroy(long ptr);
}
