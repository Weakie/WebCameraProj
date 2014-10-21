package com.weakie.encoder;

import java.nio.ByteBuffer;

public interface Encoder {

	/**
	 * 
	 * @param in direct NIO
	 * @param out direct NIO
	 * @return
	 * @throws Exception
	 */
	public long encodeData(ByteBuffer in,ByteBuffer out) throws Exception;
	
	public void destroy();
	
	public int getHeight();

	public int getWidth();

	public int getFrameSize();
}
