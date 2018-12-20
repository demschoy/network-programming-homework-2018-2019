//package com.fmi.mpr.hw.http;
package last;

import java.net.*;

import java.io.*;

public class HttpServer 
{
	private ServerSocket server;
	private String type;
	private String filename;
	private boolean isRunning;
	private final String errorMessage = "<!DOCTYPE html>\n" + 
			   "<html>\n" + 
			   "<head>\n" + 
			   "	<title></title>\n" + 
			   "</head>\n" + 
			   "<body>\n" +
			   "The file you're requesting for cannot be found. Please try again." +
			   "</body>\n" + 
			   "</html>";
		
	public HttpServer() throws IOException
	{
		this.server = new ServerSocket(8801);
	}
	
	public void start()
	{
		if(!isRunning)
		{
			this.isRunning = true;
			run();
		}
	}
	
	private void run()
	{
		while(isRunning)
		{
			try
			{
		 		listen();
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void listen() throws Exception
	{
		Socket client = null;
		try
		{
			client = server.accept();
			System.out.println(client.getInetAddress() + " connected!");
			
			processClient(client);
			
			System.out.println("Connection to " + client.getInetAddress() + " closed!");
		}
		finally
		{
			if(client != null)
				client.close();
		}
	}
	
	private void processClient(Socket client) throws Exception
	{
		try(
				BufferedInputStream bis = new BufferedInputStream(client.getInputStream());
				PrintStream ps = new PrintStream(client.getOutputStream(),true))
			{
				String response = read(ps, bis);
				writePOST(ps, response);
			}
	}
	
	private void writePOST(PrintStream ps, String response) throws IOException //POST
	{
		if (ps != null) {
			
		}
	}
	
	
	private String read(PrintStream ps, BufferedInputStream bis) throws Exception
	{
		if(bis != null)
		{
			StringBuilder request = new StringBuilder();

			byte[] buffer = new byte[1024];
			int bytesRead = 0;

			while((bytesRead = bis.read(buffer, 0, 1024)) > 0)
			{
					request.append(new String(buffer, 0, bytesRead));
	
					if(bytesRead < 1024)
						break;
			}
		
			return parseRequest(ps, request.toString());
		}
		
		return "Error";
	}
	
	private String parseRequest(PrintStream ps, String request) throws Exception
	{
		String[] lines = request.split("\n");
		String header = lines[0];
		
		String[] headerParts = header.split(" ");
		this.type = headerParts[0];
		
		String fullFileName = headerParts[1];
		
		if(type.equals("GET"))
			parseGetRequest(ps, request);
		if(type.equals("POST"))
			parsePostRequest(ps, fullFileName);
		
		return null;
	}

	private void parseGetRequest(PrintStream ps, String request) throws Exception 
	{
		ps.println("HTTP/1.1 200 OK");
		
		String[] lines = request.split("\n");
		String header = lines[0];
		String[] headerParts = header.split(" ");
		filename = headerParts[1].substring(1);
		String extension = (filename.split("\\."))[1];
		
		try
		{
			if(extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg") || extension.equals("bmp"))
			{
				ps.println();
				sendImage(ps);
				System.out.println("Sent image.");
			}
		}
		catch(Exception e) 
		{
			ps.println(errorMessage);
		}
		
		try
		{

			if(extension.equals("mp4") || extension.equals("avi"))
			{
				ps.println("Content-Type: video/mp4");
				ps.println();
				sendVideo(ps);
				System.out.println("Sent video.");
				
			}
		}
		catch(Exception e) 
		{
			ps.println(errorMessage);
		}
		
			
		try
		{	
			if(extension.equals("txt"))
			{
				ps.println();
				sendText(ps);
				System.out.println("Sent text.");
			}
		}
		catch(Exception e)
		{
			ps.println(errorMessage);
		}
	}
	
	private void sendText(PrintStream ps) throws IOException 
	{
		File f = new File(filename);
		FileInputStream fileIn = new FileInputStream(f.getAbsolutePath());
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
		while((bytesRead = fileIn.read(buffer, 0, 8192)) > 0)
			ps.write(buffer, 0, bytesRead);
		
		ps.flush();
		fileIn.close();
	}

	private void sendVideo(PrintStream ps) throws IOException 
	{
		File f = new File(filename);
		FileInputStream fileIn = new FileInputStream(f.getAbsolutePath());
		
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
	
		while((bytesRead = fileIn.read(buffer, 0, 8192)) > 0)
			ps.write(buffer, 0, bytesRead);

		ps.flush();
		fileIn.close();
	}

	private void sendImage(PrintStream ps) throws IOException 
	{
		File f = new File(filename);
		FileInputStream fileIn = new FileInputStream(f.getAbsolutePath());
		
		int bytesRead = 0;
		byte[] buffer = new byte[4096];
	
		while((bytesRead = fileIn.read(buffer, 0, 4096)) > 0)
			ps.write(buffer, 0, bytesRead);
		
		ps.flush();
		fileIn.close();
	}
	
	private String parsePostRequest(PrintStream ps, String filename) throws IOException 
	{		
		
			return null;
	}

	private String parseTextBody(String body) 
	{
		return null;
	}
	
	public static void main(String[] args) throws IOException
	{
		HttpServer serv = new HttpServer();
		serv.start();
	}
}
