import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.*;


/**
 * A really simple HTTP Client
 * 
 * @author You
 *
 */

public class GetFile {
	private static final int BUF_SIZE = 512;
	private static final int MAX_RETRY = 3;
	
	private static final int REQUEST_SIZE = 10;


	private static Stats stat;

	public static void main(String[] args) throws Exception {
		if (args.length < 1) {
			System.out.println("Usage: java GetFile url_to_access");
			System.exit(0);
		}
		int size = headRequest(new URL(args[0]));
		int j= 0;
		while(j * REQUEST_SIZE < size){
			for (int i = 0; i < args.length; i++) {

				// TODO lancar threads para vários servidores
				String url = args[0];
				URL u = new URL(url);
				// Assuming URL of the form http://server-name:port/path ....
				int port = u.getPort() == -1 ? 80 : u.getPort();
				String path = u.getPath() == "" ? "/" : u.getPath();

				downloadFile(u.getHost(), port, path);
				j++;
				Thread thread = new Thread(new HTTPClient(u, j*size, (j+1)*size));
			}
		}

	}

	// Implement here to download the file requested in the URL
	// and write the file in the client side.
	// In the end print te requires detatistics

	private static void downloadFile(String host, int port, String path) throws UnknownHostException, IOException {

		// TODO dowload de partes do ficheiro separadamente

		String filename = path.substring(path.lastIndexOf('/') + 1);
		if (filename.equals(""))
			filename = "index.html";

		Socket sock = new Socket(host, port);

		OutputStream out = sock.getOutputStream();
		InputStream in = sock.getInputStream();
		String request = String.format(
				"GET %s HTTP/1.0\r\n" + 
				"Host: %s\r\n" + 
				"User-Agent: X-RC2020 SimpleHttpClient\r\n\r\n", path, host);

		out.write(request.getBytes());

		System.out.println("\nSent Request:\n-------------\n" + request);
		System.out.println("Got Reply:");
		System.out.println("\nReply Header:\n--------------");

		String answerLine = Http.readLine(in); // first line is always present
		System.out.println(answerLine);
		String[] reply = Http.parseHttpReply(answerLine);
		long[] range = null;

		answerLine = Http.readLine(in);
		while (!answerLine.equals("")) {
			System.out.println(answerLine);
			String[] head = Http.parseHttpHeader(answerLine);
			answerLine = Http.readLine(in);
		}

		if (reply[1].equals("200")) {

			System.out.println("\nReply Body:\n--------------");
			long time0 = System.currentTimeMillis();
			int n;
			byte[] buffer = new byte[BUF_SIZE];

			while ((n = in.read(buffer)) >= 0) {
				System.out.write(buffer, 0, n);
			}
		} else
			System.out.println("Ooops, received status:" + reply[1]);
	}

	private static int headRequest(URL url) throws UnknownHostException, IOException {
		int port = url.getPort() == -1 ? 80 : url.getPort();
		String path = url.getPath() == "" ? "/" : url.getPath();
		String filename = path.substring( path.lastIndexOf('/')+1);
		if ( filename.equals("")) filename = "index.html";

		Socket sock = new Socket( url.getHost(), port);

		OutputStream out = sock.getOutputStream();
		InputStream in = sock.getInputStream();
		String request = String.format(
			"HEAD %s HTTP/1.0\r\n"+
			"Host: %s\r\n"+
			"User-Agent: X-RC2020 HttpClient\r\n\r\n", path, url.getHost());
			
			out.write(request.getBytes());
			String answerLine = Http.readLine(in);
			String[] reply = Http.parseHttpReply(answerLine);

		while ( !answerLine.equals("") ) {
			
			if(reply[0].equals("Content-Length:")){

				return Integer.parseInt(reply[1]);
			}
			//String[] head = Http.parseHttpHeader(answerLine);
			answerLine = Http.readLine(in);
			reply = Http.parseHttpHeader(answerLine);
		}



		return 0;
	}
}



// can add other private stuff as needed
