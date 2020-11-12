import java.io.*;
import java.net.*;

public class HTTPClient extends Thread {
    private static final int BUF_SIZE = 512;
    private static final int MAX_RETRY = 3;

    private static Stats stat;
    URL url;
    int startRange;
    int endRange;

    public HTTPClient(URL url, int startRange, int endRange) {
        this.url = url;
        this.startRange = startRange;
        this.endRange = endRange;

    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        int port = url.getPort() == -1 ? 80 : url.getPort();
        String path = url.getPath() == "" ? "/" : url.getPath();

        try {
            downloadFile(url.getHost(), port, path);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
	private void downloadFile(String host, int port, String path) throws UnknownHostException, IOException {
		String filename = path.substring( path.lastIndexOf('/')+1);
		if ( filename.equals("")) filename = "index.html";

		Socket sock = new Socket( host, port );

		OutputStream out = sock.getOutputStream();
		InputStream in = sock.getInputStream();
		String request = String.format(
			"GET %s HTTP/1.0\r\n"+
			"Host: %s\r\n"+
            "User-Agent: X-RC2020 HttpClient\r\n\r\n"+
            "Range: bytes=%d-%d", path, host, startRange, endRange);

		out.write(request.getBytes());

		System.out.println("\nSent Request:\n-------------\n"+request);		
		System.out.println("Got Reply:");
		System.out.println("\nReply Header:\n--------------");
		
		String answerLine = Http.readLine(in);  // first line is always present
		System.out.println(answerLine);
		String[] reply = Http.parseHttpReply(answerLine);
		long[] range = null;

		answerLine = Http.readLine(in);
		while ( !answerLine.equals("") ) {
			System.out.println(answerLine);
			String[] head = Http.parseHttpHeader(answerLine);
			answerLine = Http.readLine(in);
		}

		if ( reply[1].equals("200")) {

		System.out.println("\nReply Body:\n--------------");
                        long time0 = System.currentTimeMillis();
                        int n;
                        byte[] buffer = new byte[BUF_SIZE];
			
			while( (n = in.read(buffer)) >= 0 ) {
			     System.out.write(buffer, 0, n);
			}
		}
		else
		    System.out.println("Ooops, received status:" + reply[1]);
	}
   
}
