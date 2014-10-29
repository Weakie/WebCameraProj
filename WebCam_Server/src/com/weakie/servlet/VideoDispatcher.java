package com.weakie.servlet;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.weakie.stream.process.MainLoop;
import com.weakie.websocket.videostream.VideoEndpoint;
import com.weakie.websocket.videostream.VideoEndpointAsync;

/**
 * Servlet implementation class VideoDispatcher
 */
public class VideoDispatcher extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private static final int BUF_SIZE = 4096;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public VideoDispatcher() {
        super();
        // TODO Auto-generated constructor stub
    }

    private MainLoop loop = new MainLoop();
    private Process process;
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String cmd = request.getParameter("cmd");
		String path = System.getProperty("java.library.path");
		response.getWriter().println(path);
		if("B".endsWith(cmd)){
			loop.beginThread();
		}else if("BB".equals(cmd)){
			loop.stop();
		}else if("Q".equals(cmd)){
			//ffmpeg -s 640x480 -f dshow -i video="Integrated Webcam" -f mpeg1video -b 800k -r 30 http://localhost:8080/WebCam_Server/dispatcher
			process = Runtime.getRuntime().exec("ffmpeg -s 640x480 -f dshow -i video=\"Integrated Webcam\" -f mpeg1video -b 800k -r 30 http://localhost:8080/WebCam_Server/dispatcher?camera=001");
			BufferedReader in = new BufferedReader(new InputStreamReader((process.getErrorStream())));
			String input = null;
			while((input = in.readLine())!=null){
				System.out.println(input);
			}
		}else if("QQ".equals(cmd)){
			process.destroy();
		}		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("=====data in=====");
		String window = request.getParameter("camera");
		System.out.println(window);
		InputStream in = request.getInputStream();
		BufferedInputStream bin = new BufferedInputStream(in);
		byte[] data = new byte[BUF_SIZE];
		int length  = 0;
		ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);
		while((length = bin.read(data))!=-1){
			buf.clear();//(position=0,limit=capacity)
			buf.put(data,0,length);//(position=length)
			buf.flip();//(position=0,limit=old position)
			VideoEndpointAsync.broadcast(buf, false);
		}
	}

}
