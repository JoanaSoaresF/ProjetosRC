import java.io.*;
import java.net.*;

/** 
* @author Goncalo Lourenco 55780
* @author Joana Faria 55754
*
*/
public class HTTPClient {

    public static final int BUF_SIZE = 512;

	private URL url;
	//private int port;
	private String path;
	private String host;


	public HTTPClient(URL url){
		this.url = url;
		//port = url.getPort() == -1 ? 80 : url.getPort();
		path = url.getPath() == "" ? "/" : url.getPath();
		host = url.getHost();
		
	}

	public byte[] completeRequest(OutputStream out, InputStream in) throws IOException {

			String request = String.format(
			"HEAD %s HTTP/1.1\r\n"+
			"Host: %s\r\n"+
			"User-Agent: X-RC2020 SimpleHttpClient\r\n\r\n", path, host);
			byte[] result = sendRequest(request, in, out, 0);

			return result;
	}

	public byte[] partialRequest(OutputStream out, InputStream in, int start, int end) throws IOException {
		String request= String.format("GET %s HTTP/1.1\r\n" 
			+ "Host: %s\r\n"
			+ "User-Agent: X-RC2020 HttpClient\r\n" 
			+ "Range: bytes=%d-%d\r\n\r\n", path, host, start, end);
			byte[] result = sendRequest(request, in, out, start);

		return result;
	}

	private byte[] sendRequest (String request, InputStream in, OutputStream out, int start) throws IOException {
		int sended = -1;
		byte[] bytesSended = null;	

		out.write(request.getBytes());

		System.out.println("\nSent Request:\n-------------\n"+request);		
		System.out.println("Got Reply:");
		System.out.println("\nReply Header:\n--------------");
		
		String answerLine = Http.readLine(in);  // first line is always present
		System.out.println(answerLine);
		String[] reply = Http.parseHttpReply(answerLine);
		int[] range = new int[2];
		int length = 0;

		answerLine = Http.readLine(in);
		while ( !answerLine.equals("") ) {
			System.out.println(answerLine);
			String[] head = Http.parseHttpHeader(answerLine);
			if((head[0].toLowerCase()).equals("Content-Range".toLowerCase())){
				String [] r = head[1].split(" ");
				String [] r1 = r[1].split("-");
				r1[1]=r1[1].split("/")[0];
				range[0] = Integer.parseInt(r1[0]);
				range[1] = Integer.parseInt(r1[1]);
				if(start<range[0]) {
					return null;
				}
			} if((head[0].toLowerCase()).equals("Content-Length".toLowerCase())){
				length = Integer.parseInt(head[1]);

			}
			answerLine = Http.readLine(in);
		}

		if (reply[1].equals("200") || reply[1].equals("206")) {
			sended = length;
			bytesSended = new byte[sended];
			int n;
			int written = 0;
			int left = sended;
			byte[] aux = new byte[BUF_SIZE];
			

			while( (n = in.read(aux)) >= 0 ) {
				for(int i = 0; i<n;i++)
					bytesSended[written+i]=aux[i];
				written += n;
				left -= n;
				if(left<0) {
					left = 0;
				}
			}
			if(written < sended) {
				return null;
			}
		
		}

		return bytesSended;
	}

	
	public int getFileSize(OutputStream out, InputStream in) throws IOException {
		int s = -1;
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
}
