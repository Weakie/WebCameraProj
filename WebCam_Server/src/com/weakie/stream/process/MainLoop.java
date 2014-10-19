package com.weakie.stream.process;


public class MainLoop {

	private GrabAndToBuffer getter;
	private EncoderAndDispatcher ead;
	
	public void beginThread(){
		ead = new EncoderAndDispatcher();
		getter =  new GrabAndToBuffer(0);
		getter.addListener(ead.getListener());
		Thread t1 = new Thread(ead);
		Thread t2 = new Thread(getter);
		t1.setName("Thread of ead");
		t2.setName("Thread of getter");
		t1.start();
		t2.start();
	}
	
	public void stop(){
		getter.removeListener(ead.getListener());
		getter.setIsEnd(true);
		ead.setIsEnd(true);
	}
	
}
