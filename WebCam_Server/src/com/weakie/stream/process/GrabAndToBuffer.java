package com.weakie.stream.process;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.OpenCVFrameGrabber;

class GrabAndToBuffer implements Runnable{

	private int deviceNumber = 0;
	private volatile boolean isEnd = false;
	private List<ImageUpdateListener> listsners = new ArrayList<ImageUpdateListener>();
	
	public GrabAndToBuffer(int deviceNumber){
		this.deviceNumber = deviceNumber;
	}
	
	public void setIsEnd(boolean isEnd){
		this.isEnd = isEnd;
	}
	
	public synchronized void removeListener(ImageUpdateListener listener){
		this.listsners.remove(listener);
	}
	
	public synchronized void addListener(ImageUpdateListener listener){
		this.listsners.add(listener);
	}
	
	private void grabImage() throws Exception {
		OpenCVFrameGrabber grabber = null;
		
		try {
			grabber = OpenCVFrameGrabber.createDefault(this.deviceNumber);
			grabber.start();

			int count = 0;
			IplImage pFrame = grabber.grab();
			while (pFrame != null) {
				long time1=System.currentTimeMillis();
				this.updateEvents(pFrame);
				long time2=System.currentTimeMillis();
				pFrame = grabber.grab();
				long time3=System.currentTimeMillis();
				System.out.println("Frame: "+ count++ +" time grab: "+(time3-time2)+" time update: "+(time2-time1));
	  			if(isEnd){
					break;
				}
			}
			/**
			 * never release the retrieved frame,this will crash the jvm!!!
			 */
			//cvReleaseImage(pFrame);
		} finally {
			if (grabber != null) {
				grabber.release();
			}
		}
	}
	
	private synchronized void updateEvents(IplImage pFrame){
		ByteBuffer buf = pFrame.getByteBuffer();
		buf.flip();
		for(ImageUpdateListener listener:this.listsners){
			listener.updateVideoImage(buf);
		}
		buf.clear();
	}

	@Override
	public void run() {
		try {
			this.grabImage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
