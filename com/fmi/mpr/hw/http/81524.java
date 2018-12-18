package com.fmi.mpr.hw.http;

import java.net.*;

import java.io.*;

public class HttpServer 
{
	private ServerSocket server;
	private String type;
	private String filename;
	private boolean isRunning;
	private final String errorImage = "<!DOCTYPE html>\n" + 
			   "<html>\n" + 
			   "<head>\n" + 
			   "	<title></title>\n" + 
			   "</head>\n" + 
			   "<body>\n" +
			   "<form action=\"/action_page.php\">\n" + 
			   "			  <input type=\"file\" name=\"pic\" accept=\"image/*\">\n" + 
			   "			  <input type=\"submit\">\n" + 
			   "			</form> " +
			   "</body>\n" + 
			   "</html>";
	
	private final String errorVideo = "<!DOCTYPE html>\n" + 
			   "<html>\n" + 
			   "<head>\n" + 
			   "	<title></title>\n" + 
			   "</head>\n" + 
			   "<body>\n" +
			   "<form action=\"/action_page.php\">\n" + 
			   "			  <input type=\"file\" name=\"video\" accept=\"video/*\">\n" + 
			   "			  <input type=\"submit\">\n" + 
			   "			</form> " +
			   "</body>\n" + 
			   "</html>";
	
	private final String errorText = "<!DOCTYPE html>\n" + 
			   "<html>\n" + 
			   "<head>\n" + 
			   "	<title></title>\n" + 
			   "</head>\n" + 
			   "<body>\n" +
			   "<form action=\"/action_page.php\">\n" + 
			   "			  <input type=\"file\" name=\text\" accept=\"text/*\">\n" + 
			   "			  <input type=\"submit\">\n" + 
			   "			</form> " +
			   "</body>\n" + 
			   "</html>";
	public HttpServer() throws IOException
	{
		this.server = new ServerSocket(8888);
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
		if(ps != null)
		{
			/*ps.println("HTTP/1.0 200 OK");
			ps.println();
			ps.println("<!DOCTYPE html>\n" + 
					"<html>\n" + 
					"<head>\n" + 
					"	<title>???</title>\n" + 
					"</head>\n" + 
					"<body>\n" + 
					"<h1>Hello</h1>" + 
					"<form method=\"POST\" action=\"/\">" +

					"</form>" +
					"<h2>" + (response == null || response.trim().isEmpty() ? "" : response) + "</h2>" +
					"</body>\n" + 
					"</html>");
*/		}
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
		
		if(type.equals("GET"))
			parseGetRequest(ps, request);
		if(type.equals("POST"))
			parsePostRequest(ps, request);
		
		return null;
	}

	private void parseGetRequest(PrintStream ps, String request) throws Exception 
	{
		ps.println("HTTP/1.1 200 OK");
		ps.println();
		
		String[] lines = request.split("\n");
		String header = lines[0];
		String[] headerParts = header.split(" ");
		filename = headerParts[1].substring(1);
		String extension = (filename.split("\\."))[1];
		
		try
		{
			if(extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg") || extension.equals("bmp"))
			{
				File f = new File("C:\\Users\\vivacom\\Documents\\Eclipse\\homework\\src\\homework\\"+filename);
				FileInputStream fileIn = new FileInputStream(f);
				sendImage(ps, fileIn);
				System.out.println("Sent image.");
			}
		}
		catch(Exception e) 
		{
			ps.println(errorImage);
		}
		
		try
		{

			if(extension.equals("mp4") || extension.equals("avi"))
			{
				File f = new File("C:\\Users\\vivacom\\Documents\\Eclipse\\homework\\src\\homework\\"+filename);
				FileInputStream fileIn = new FileInputStream(f);
				sendVideo(ps, fileIn);
				System.out.println("Sent video.");
			}
		}
		catch(Exception e) 
		{
			ps.println(errorVideo);
		}
		
			
		try
		{	
			if(extension.equals("txt"))
			{
				File f = new File("C:\\Users\\vivacom\\Documents\\Eclipse\\homework\\src\\homework\\"+filename);
				FileInputStream fileIn = new FileInputStream(f);
				sendText(ps,fileIn);
				System.out.println("Sent text.");
			}
		}
		catch(Exception e)
		{
			ps.println(errorText);
		}
	}
	
	private void sendText(PrintStream ps, FileInputStream fileIn) throws IOException 
	{
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
		while((bytesRead = fileIn.read(buffer, 0, 8192)) > 0)
			ps.write(buffer, 0, bytesRead);
		
		ps.flush();
		fileIn.close();
	}

	private void sendVideo(PrintStream ps, FileInputStream fileIn) throws IOException 
	{
		BufferedReader reader=null;
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
	
		while((bytesRead = fileIn.read(buffer, 0, 8192)) > 0)
			ps.write(buffer, 0, bytesRead);

		ps.flush();
		fileIn.close();
	}

	private void sendImage(PrintStream ps, FileInputStream fileIn) throws IOException 
	{
		int bytesRead = 0;
		byte[] buffer = new byte[4096];
	
		while((bytesRead = fileIn.read(buffer, 0, 4096)) > 0)
			ps.write(buffer, 0, bytesRead);
		
		ps.flush();
		fileIn.close();
	}

	private String parsePostRequest(PrintStream ps, String request) throws IOException 
	{		
		ps.println(request);
		String[] lines = request.split("\n");
		String header = lines[0];
		String[] headerParts = header.split(" ");
		
		String uri = header.split(" ")[1];
		
		if(uri.length() != 1)
			uri = uri.substring(1);
		
		if(uri.equals("video"))
		{
			ps.println("HTTP/1.0 200 OK");
			ps.println("Content-type: video/mp4");
			ps.println();
			
			File f = new File("video.mp4");
			FileInputStream fileIn = new FileInputStream(f);
					
			try
			{
				sendVideo(ps,fileIn);
			}
			catch(IOException e)
			{
				
			}
		}
		else if(uri.equals("image"))
		{
			File f = new File("");
			FileInputStream fileIn = new FileInputStream(f);
			try
			{
				sendImage(ps, fileIn);
			}
			catch(IOException e)
			{
				
			}
		}
		else 
		{
			StringBuilder body = new StringBuilder();
			boolean readBody = false;
			for(String line : lines)
			{
				if(readBody)
					body.append(line);
				if(line.trim().isEmpty())
					readBody = true;
			}
			
			return parseTextBody(body.toString());
		}
			
		return null;
	}

	private String parseTextBody(String body) 
	{
		
		return null;
		
	}
	
	public static void main(String[] args) throws IOException
	{
		new HttpServer().start();
	}
}
