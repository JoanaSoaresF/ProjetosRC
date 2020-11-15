import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


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
			System.out.println(i*sizeOfRequest);
			System.out.println((i+1)*(sizeOfRequest)-1);
			cc[i] = new HTTPClient(u, i*sizeOfRequest, (i+1)*(sizeOfRequest)-1, file, size, stat);
			cc[i].start();
			
		}
		
		for(int i = 0; i<args.length; i++){
			cc[i].join();	
		}
		
		stat.printReport();
		
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

