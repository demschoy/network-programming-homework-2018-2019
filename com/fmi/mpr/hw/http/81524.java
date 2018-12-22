package com.fmi.mpr.hw.http;

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
			   			"ERROR! The file you're requesting for cannot be found. Please try again." +
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
				String response = read(ps, client, bis);
				writePOST(ps, response);
			}
	}
	
	private void writePOST(PrintStream ps, String response) throws IOException //POST
	{
		if (ps != null) 
		{
			//ps.println("HTTP/1.1 200 OK");
			ps.println();
			ps.println( "<!DOCTYPE html>\n" + 
						"<form action=\"upload.php\" method=\"POST\" enctype=\"multipart/form-data\">\n" + 
						"Select file to upload:\n" +
						"<input type=\"file\" name=\"fileToUpload\" id=\"fileToUpload\">\n" +
						"<input type=\"submit\" value=\"Upload File\" name=\"submit\">\n" +
						"</form>" +
					"<h2>" + (response == null || response.trim().isEmpty() ? "" : response) + "</h2>" +
					"</body>\n" + 
					"</html>");
		}
	}
	
	
	private String read(PrintStream ps, Socket client, BufferedInputStream bis) throws Exception
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
		
			return parseRequest(ps, client, request.toString());
		}
		
		return "Error";
	}
	
	private String parseRequest(PrintStream ps, Socket client, String request) throws Exception
	{
		String[] lines = request.split("\n");
		String header = lines[0];
		
		String[] headerParts = header.split(" ");
		this.type = headerParts[0];
		
		String fullFileName = headerParts[1];
		
		if(type.equals("GET"))
			return parseGetRequest(ps, request);
		if(type.equals("POST"))
			return parsePostRequest(ps, lines, fullFileName, client);
		
		return parsePostRequest(ps, lines, fullFileName, client);
		
		//return null;
	}

	private String parseGetRequest(PrintStream ps, String request) throws Exception 
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
			
			if(extension.equals("mp4") || extension.equals("avi"))
			{
				ps.println("Content-Type: video/mp4");
				ps.println();
				sendVideo(ps);
				System.out.println("Sent video.");
			}
			
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
		
		return null;
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
	
	private String parsePostRequest(PrintStream ps, String[] lines, String filename, Socket client) throws IOException 
	{		
		StringBuilder body = new StringBuilder();
		boolean readBody = false;
		for (String line : lines) {
			if (readBody) {
				body.append(line);
			}
			if (line.trim().isEmpty()) {
				readBody = true;
			}
		}
		
		return parseBody(client, body.toString());
	}

	private String parseBody(Socket client, String body) throws IOException 
	{
		if (body != null && !body.trim().isEmpty()) 
		{
			String[] operands = body.split("&");
			String fileName = operands[0].split("=")[1];
			
			return sendFile(fileName, body, client);
		}
		return null;
	}
	
	private String sendFile(String fileName, String body, Socket client) throws IOException 
	{
		BufferedInputStream bis = new BufferedInputStream(client.getInputStream());
		String extension = fileName.split("\\.")[1];
		String data = null;
		PrintStream ps = new PrintStream(client.getOutputStream(), true);
		
		if(extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg") || extension.equals("bmp")
				|| extension.equals("mp4") || extension.equals("avi"))
		{
			data = sendMedia(bis, ps);
		}
		
		if(extension.equals("txt"))
		{
			data = sendTextFiles(bis, ps);
		}
        
		File statText = new File(fileName);
        FileOutputStream is = new FileOutputStream(statText);
        OutputStreamWriter osw = new OutputStreamWriter(is);    
        Writer w = new BufferedWriter(osw);
        w.write(data);
        w.close();
        
		return null;
	}

	private String sendTextFiles(BufferedInputStream bis, PrintStream ps) throws IOException 
	{
		ps.flush();
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
	
		while((bytesRead = bis.read(buffer, 0, 8192)) > 0)
			ps.write(buffer, 0, bytesRead);
		
		return ps.toString();
		
	}

	private String sendMedia(BufferedInputStream bis, PrintStream ps) throws IOException 
	{
		ps.flush();
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
	
		while((bytesRead = bis.read(buffer, 0, 8192)) > 0)
			ps.write(buffer, 0, bytesRead);
		
		return ps.toString();
		
	}

	public static void main(String[] args) throws IOException
	{
		HttpServer serv = new HttpServer();
		serv.start();
	}
}
