package com.weakie.encoder.mpeg;

import static org.bytedeco.javacpp.avutil.av_frame_alloc;
import static org.bytedeco.javacpp.swscale.SWS_BICUBIC;

import java.nio.ByteBuffer;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.avcodec;
import org.bytedeco.javacpp.avcodec.AVCodec;
import org.bytedeco.javacpp.avcodec.AVCodecContext;
import org.bytedeco.javacpp.avcodec.AVPacket;
import org.bytedeco.javacpp.avutil;
import org.bytedeco.javacpp.avutil.AVDictionary;
import org.bytedeco.javacpp.avutil.AVFrame;
import org.bytedeco.javacpp.avutil.AVRational;
import org.bytedeco.javacpp.swscale;
import org.bytedeco.javacpp.swscale.SwsContext;
import org.bytedeco.javacpp.swscale.SwsFilter;

import com.weakie.encoder.Encoder;

public class MPEGEncoder implements Encoder {
	static{
		try {
			/**
			 * Due to a dll dependency, swresample must be loaded prior to any reference to avcodec, 
			 * otherwise an UnsatisfiedLinkError is thrown.
			 */
			Class.forName("org.bytedeco.javacpp.swresample");
			/**
			 * register all AVCodec
			 */
			avcodec.avcodec_register_all();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	public static MPEGEncoder createAndInitInstance(int width,int height,int frameSize,int srcFmt){
		MPEGEncoder en = new MPEGEncoder(width,height,frameSize,srcFmt);
		if(en.init()!=1){
			en.destroy();
			return null;
		}
		return en;
	}
	
	private AVCodec codec;
	private AVCodecContext c;
	private AVRational rational;

	private AVFrame frame;
	private AVFrame frameSrc;
	private SwsContext swsContext;
	
	private int width,height,frameSize;
	private int framePts = 0;
	
	private int srcFmt;
	private int desFmt = avutil.AV_PIX_FMT_YUV420P;
	
	private AVPacket pkt = new AVPacket();
	private int[] got_output = new int[1];
	
	private MPEGEncoder(int width,int height,int frameSize,int srcFmt){
		rational = new AVRational();
		rational.num(1);
		rational.den(25);
		this.srcFmt = srcFmt;
		this.width = width;
		this.height= height;
		this.frameSize=frameSize;
	}
	
	private int init(){
		int ret = 0;
		
		//get codec
		codec = avcodec.avcodec_find_encoder(avcodec.AV_CODEC_ID_MPEG1VIDEO);
		if(codec.isNull()){
			System.out.println("Codec not found!");
			return -1;
		}
		
		//init codec context
		c = avcodec.avcodec_alloc_context3(codec);
		if(c.isNull()){
			System.out.println("Could not allocate video codec context!");
			return -1;
		}
		c.bit_rate(800000);
		c.width(this.width);
		c.height(this.height);
		c.time_base(rational);
		c.gop_size(10);
		c.max_b_frames(1);
		c.pix_fmt(this.desFmt);
		if(avcodec.avcodec_open2(c, codec, (AVDictionary)null)<0){
			System.out.println("Could not open codec");
			return -1;
		}
		
		//init dest frame of YUV
		frame = av_frame_alloc();
		if(frame.isNull()){
			System.out.println("Could not allocate video frame!");
			return -1;
		}
		frame.format(this.desFmt);
		frame.width(width);
		frame.height(height);
		ret = avutil.av_image_alloc(frame.data(), frame.linesize(), frame.width(), frame.height(), frame.format(), 32);
		if(ret < 0){
			System.out.println("Could not allocate raw picture buffer");
			return -1;
		}
		//init src frame of RGB
		frameSrc = av_frame_alloc();
		if(frameSrc.isNull()){
			System.out.println("Could not allocate video frame!");
			return -1;
		}
		frameSrc.format(this.srcFmt);
		frameSrc.width(width);
		frameSrc.height(height);
		ret = avutil.av_image_alloc(frameSrc.data(), frameSrc.linesize(), frameSrc.width(), frameSrc.height(), frameSrc.format(), 32);
		if(ret < 0){
			System.out.println("Could not allocate raw picture buffer");
			return -1;
		}
		//init convert context
		swsContext = swscale.sws_getCachedContext(swsContext,
				frameSrc.width(), frameSrc.height(), frameSrc.format(), 
				frame.width(), frame.height(), frame.format(), 
				SWS_BICUBIC, (SwsFilter)null, (SwsFilter)null, (DoublePointer)null);
		
		return 1;
	}
	
	@Override
	public long encodeData(ByteBuffer in, ByteBuffer out) throws Exception {
		int ret = 0;
		
		avcodec.av_init_packet(pkt);
		pkt.data(null);
		pkt.size(0);
		
		if(in.capacity()<frameSize){
			System.out.println("Error invalid encode in size " +in.limit()+ " out size " +out.limit());
			return -1;
		}
		Pointer inbuf = new Pointer(in);
		Pointer.memcpy(frameSrc.data(0), inbuf, frameSize);
		inbuf.deallocate();
		
		swscale.sws_scale(this.swsContext, 
				frameSrc.data(), frameSrc.linesize(), 0, height,
				frame.data(), frame.linesize());
		
		frame.pts(this.framePts++);
		
		ret = avcodec.avcodec_encode_video2(c, pkt, frame, got_output);
		if(ret < 0){
			System.out.println("Error encoding frame");
			return -2;
		}
		
		if(got_output[0] == 1){
			if(pkt.size()>out.limit()){
				return -3;
			}
			Pointer outbuf = new Pointer(out);
			//outbuf.put(pkt.data());
			Pointer.memcpy(outbuf, pkt.data(), pkt.size());
			outbuf.deallocate();
			int retSize = pkt.size();
			avcodec.av_free_packet(pkt);
			return retSize;
		}
		return -4;
	}

	@Override
	public void destroy() {
		avcodec.avcodec_close(c);
		avcodec.avcodec_free_context(c);
		avutil.av_freep(frame.data());
		avutil.av_frame_free(frame);
		avutil.av_freep(frameSrc.data());
		avutil.av_frame_free(frameSrc);
		swscale.sws_freeContext(swsContext);
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getFrameSize() {
		return frameSize;
	}

}
