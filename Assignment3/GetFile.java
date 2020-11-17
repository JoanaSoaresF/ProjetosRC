import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.io.File;
import java.io.IOException;


/**
 * A really simple HTTP Client
 * 
 * @author Goncalo Lourenco 55780
 * @author Joana Faria 55754
 *
 */

public class GetFile {
	private static final int BUF_SIZE = 512;
	private static final int MAX_RETRY = 3;
	private static Stats stat;
	static File file;

	public static void main(String[] args) throws Exception {
		stat = new Stats();
		if (args.length < 1) {
			System.out.println("Usage: java GetFile url_to_access");
			System.exit(0);
		}

		String url = args[0];
		URL u = new URL(url);
		int size = fileSize(u);
		createFile(url);

		RequestSingleServer[] servers = new RequestSingleServer[args.length];

		int sizeOfRequest = (int) size / (args.length) + 1;
		for (int i = 0; i < args.length; i++) {
			url = args[i];
			u = new URL(url);
			int startRange = i * sizeOfRequest;
			int endRange = (i + 1) * (sizeOfRequest) - 1;
			servers[i] = new RequestSingleServer(u, startRange, endRange, file, size, stat);
			servers[i].start();
		}

		for (int i = 0; i < args.length; i++) {
			servers[i].join();
		}

		stat.printReport();

	}

	private static void createFile(String url) throws IOException {
		String[] aux = url.split("/");
		String filename = aux[aux.length - 1];
		file = new File("./copy-of-" + filename);
		file.delete();
		file.createNewFile();
	}

	private static int fileSize(URL url) {
		int size = -1;
		
		try {
			int port = url.getPort() == -1 ? 80 : url.getPort();
			Socket sock = new Socket(url.getHost(), port);
			OutputStream out = sock.getOutputStream();
			InputStream in = sock.getInputStream();

			HTTPClient c = new HTTPClient(url);
			size = c.getFileSize(out, in);
			sock.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return size;
		
	}
}

	/*private static int headRequest(URL url) throws UnknownHostException, IOException {
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
			answerLine = Http.readLine(in);
			reply = Http.parseHttpHeader(answerLine);
		}
		return s;
	}



	
*/



