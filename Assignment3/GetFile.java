import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.*;
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
	
	//private static final int REQUEST_SIZE = 10;


	private static Stats stat;
	private static FileOutputStream fos;
	static File file;

	public static void main(String[] args) throws Exception {
		stat = new Stats();
		if (args.length < 1) {
			System.out.println("Usage: java GetFile url_to_access");
			System.exit(0);
		}
		int size = headRequest(new URL(args[0]));
		System.out.println(size);

		String url = args[0];
		String[] aux = url.split("/");
		String filename = aux[aux.length - 1];
		file = new File("./copy-of-" + filename);
		HTTPClient [] cc = new HTTPClient[args.length];

		int sizeOfRequest = (int) size / (args.length) + 1;
		for(int i = 0; i<args.length; i++){
			url = args[i];
			URL u = new URL(url);
			cc[i] = new HTTPClient(u, i*sizeOfRequest, (i+1)*(sizeOfRequest)-1, file, size, stat);
			cc[i].start();
			
		}
		
		for(int i = 0; i<args.length; i++){
			cc[i].join();	
		}
		
		stat.printReport();
		
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
		int s = -1;

		int port = url.getPort() == -1 ? 80 : url.getPort();
		String path = url.getPath() == "" ? "/" : url.getPath();
		String filename = path.substring( path.lastIndexOf('/')+1);
		if ( filename.equals("")) filename = "index.html";

		Socket sock = new Socket( url.getHost(), port);

		OutputStream out = sock.getOutputStream();
		InputStream in = sock.getInputStream();
		String request = String.format(
			"GET %s HTTP/1.0\r\n"+
			"Host: %s\r\n"+
			"User-Agent: X-RC2020 HttpClient\r\n"+
            "Range: bytes=0-1\r\n\r\n", path, url.getHost());
			
			out.write(request.getBytes());
			String answerLine = Http.readLine(in);
			String[] reply = Http.parseHttpReply(answerLine);

		while ( !answerLine.equals("") ) {
			
			if((reply[0].toLowerCase()).equals("Content-Length".toLowerCase())){

				s = Integer.parseInt(reply[1]);
			
			} else if((reply[0].toLowerCase()).equals("Content-Range".toLowerCase())){
					String [] r = reply[1].split(" ");
					String [] r1 = r[1].split("-");
					r1[0]=r1[1].split("/")[1];
					if(!r1[0].equals("*")){
						int range = Integer.parseInt(r1[0]);
						if(range > s)
							s = range;
					}
			}
			//String[] head = Http.parseHttpHeader(answerLine);
			answerLine = Http.readLine(in);
			reply = Http.parseHttpHeader(answerLine);
		}
		return s;
	}
}



// can add other private stuff as needed
