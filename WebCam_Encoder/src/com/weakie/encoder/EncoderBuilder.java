package com.weakie.encoder;

public class EncoderBuilder {

	//format value refer to FFMPEG
	public static final int AV_PIX_FMT_RGB24 = 2;
	public static final int AV_PIX_FMT_BGR24 = 3;
	
	public static Encoder buildMPEGEncoderJavaCV(int width,int height,int frameSize,int srcFmt){
		return com.weakie.encoder.mpeg.MPEGEncoder.createAndInitInstance(width, height, frameSize, srcFmt);
	}
	
	public static Encoder buildMPEGEncoderJNI(int width,int height,int frameSize,int srcFmt){
		return com.weakie.encoder.mpeg.jni.MPEGEncoder.createAndInitInstance(width, height, frameSize, srcFmt);
	}
}
