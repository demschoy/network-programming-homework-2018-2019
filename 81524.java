package com.fmi.mpr.hw.http;

import java.net.*;

import java.io.*;

public class HttpServer 
{
	private ServerSocket server;
	private Socket client;
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
		server = new ServerSocket(8181);
		client = null;
	}
	
	public void start()
	{
		if(!isRunning)
		{
			isRunning = true;
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
		if (ps != null) 
		{
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
//		System.out.println(request);
		
		String[] lines = request.split("\n");
		String header = lines[0];
		
		String[] headerParts = header.split(" ");
		type = headerParts[0];
		
		if(type.equals("GET"))
			return parseGetRequest(ps, request);
		if(type.equals("POST"))
			return parsePostRequest(lines);

		return null;
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
	
	private String parsePostRequest(String[] lines) throws IOException 
	{
		String header = lines[0];
		String url = header.split(" ")[1];
		
		if(url.length() != 1)
			url = url.substring(1);
		
		if(url.equals("upload.php"))
		{
			StringBuilder body = new StringBuilder();
			
			boolean readBody = false;
			for (String line : lines) 
			{
				if (readBody) 
					body.append(line);
				if (line.trim().isEmpty()) 
					readBody = true;
			}
			
			return parseBody(body.toString());
		}
		return null;
	}

	private String parseBody(String body) throws IOException 
	{
		if (body != null && !body.trim().isEmpty()) 
		{
			String[] operands = body.split(";");
			filename = operands[2].split("=")[1].split("\"")[1];
			
			String type = body.split(":")[2].split(" ")[1].split("\r")[0];
			
			return sendFile(type, body);	
		}
		return null;
	}
	private String sendFile(String type, String body) throws IOException 
	{
		PrintStream ps = new PrintStream(client.getOutputStream(), true);
		ps.println("HTTP/1.1 200 OK");
		ps.println();
		
		File file = new File(filename);
		FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
		
		
		if(type.split("/")[0].equals("image") ||type.split("/")[0].equals("video"))
		{
			fos.write(body.getBytes());
			
			fos.close();
			
			File inFile = new File(filename);
			File temp  = new File(inFile.getAbsolutePath()+".tmp");
			BufferedReader br = new BufferedReader(new FileReader(filename));
			PrintWriter pw = new PrintWriter(new FileWriter(temp));
			String line = null;
			
			for(int i=0; i<4; i++)
				br.readLine();
			
			while((line=br.readLine())!=null)
			{
				if(line.contains("------"))
					break;
				pw.println(line);
				pw.flush();
			}
			pw.close();
			br.close();
			inFile.delete();
			temp.renameTo(inFile);
		}
		if(type.split("/")[0].equals("text"))
			sendTextFiles(fos, new BufferedInputStream(client.getInputStream()));
	
		ps.println("<!DOCTYPE html>\n" + 
		   "<html>\n" + 
		   "<head>\n" + 
		   "	<title></title>\n" + 
		   "</head>\n" + 
		   "<body>\n" +
		   			"File sent!" +
		   "</body>\n" + 
		   "</html>");
		
		System.out.println("File sent!");
		
		return null;
	}

	private void sendTextFiles(FileOutputStream fos, BufferedInputStream bis) throws IOException 
	{	
		int bytesRead = 0;
		byte[] buffer = new byte[8192];
	
		while((bytesRead = bis.read(buffer, 0, 8192)) > 0)
			fos.write(buffer, 0, bytesRead);
		
		fos.close();
	}

	/*
	private String sendMedia(String body) throws IOException 
	{
		int bytesRead = 0;
		byte[] buffer = new byte[4096];
		int i=0;
		while ((bytesRead = bis.read(buffer, 0, 4096)) > 0) 
		{
			bos.write(buffer, 0, bytesRead);
		}
		return bos.toString();
	}
*/
	public static void main(String[] args) throws IOException
	{
		HttpServer serv = new HttpServer();
		serv.start();

	}
}