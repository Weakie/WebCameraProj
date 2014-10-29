package com.weakie.websocket.videostream;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BroadCastQueue {
	private final int capacity;
	private final int peerSize;
	private final Bucket[] data;
	private long writeIndex = 0;
	
	public BroadCastQueue(int capacity,int peerSize){
		this.capacity = capacity;
		this.peerSize = peerSize;
		this.data = new Bucket[capacity];
		for(int i = 0; i < this.capacity ;i++){
			this.data[i] = new Bucket(peerSize);
		}
	}
	
	public void write(ByteBuffer data){
		if(this.peerSize < data.remaining()){
			throw new BufferOverflowException();
		}
		int index = (int) (this.increaseWriteIndex() % this.capacity);
		this.data[index].put(data);
	}
	
	/**
	 * 
	 * @param dst
	 * @param readIndex
	 * @return
	 */
	public int read(ByteBuffer dst,final long readIndex){
		this.checkReadable(readIndex);
		return this.data[(int) (readIndex % this.capacity)].get(dst);
	}
	
	private synchronized long increaseWriteIndex() {
		try {
			this.writeIndex++;
			return this.writeIndex;
		} finally {
			// Notify all the read thread
			this.notifyAll();
		}
	}
	
	private synchronized void checkReadable(long readIndex){
		try{
			// Wait until write
			// Do not read data that not prepared
			while(readIndex >= this.writeIndex){
				this.wait();
			}
		}catch(InterruptedException e){
			e.printStackTrace();
		}
	}
	
	public synchronized long getWriteIndex(){
		return this.writeIndex;
	}
	
	public int getSize(int readIndex){
		return this.data[readIndex].getSize();
	}
	
	public int getCapacity(){
		return this.capacity;
	}
	
	public int getPeerSize(){
		return this.peerSize;
	}
	
	private static class Bucket{
		private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(true);
		private final Lock r = rwl.readLock();
		private final Lock w = rwl.writeLock();
		
		private final byte[] buf;
		private int size;
		
		public Bucket(int capacity){
			this.buf = new byte[capacity];
			this.size = 0;
		}
		
		public void put(ByteBuffer data){
			w.lock();
			try{
				this.size = data.remaining();
				System.arraycopy(data.array(), data.position(), buf, 0, data.limit());
			}finally{
				w.unlock();
			}
		}
		
		public int get(ByteBuffer dst){
			r.lock();
			try{
				if (this.size > dst.remaining()){
					throw new BufferOverflowException();
				}
				System.arraycopy(this.buf, 0, dst.array(), dst.position(), this.size);
				dst.position(dst.position()+this.size);
				return this.size;
			}finally{
				r.unlock();
			}
		}
		
		public int getSize(){
			r.lock();
			try{
				return this.size;
			}finally{
				r.unlock();
			}
		}
	}
}
