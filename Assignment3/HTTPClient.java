import java.io.*;
import java.net.*;

public class HTTPClient {
	private static final int BUF_SIZE = 512;
	private static final int MAX_RETRY = 3;
	private static final int REQUEST_SIZE = 10;

	private static Stats stat;
	private URL url;
	private int startRange;
	private int endRange;
	private int size;
	private boolean next;
	private FileOutputStream fos;
	private RandomAccessFile raf;

	public HTTPClient(URL url, int startRange, int endRange, File file, int totalsize) {
		this.url = url;
		this.startRange = startRange;
		this.endRange = endRange;
		this.size = totalsize;
		try {
			raf = new RandomAccessFile(file, "rw");
			raf.setLength(startRange);
    		raf.seek(startRange);
			this.fos = new FileOutputStream(raf.getFD());
			
		} catch (Exception e) {
			System.out.println("Erro aqui");
			e.printStackTrace();
		}

	}


	public void run() {
	
		int port = url.getPort() == -1 ? 80 : url.getPort();
		String path = url.getPath() == "" ? "/" : url.getPath();

		try {
			System.out.println("Chego aqui"+ startRange);
			
			
			for (int i = 0; startRange + i * REQUEST_SIZE <= endRange; i++)
				downloadFile(url.getHost(), port, path, i);
			
			//fos.flush();
			System.out.println(endRange);
			fos.close();
			raf.close();
			
			
			


		} catch (Exception e1) {
			System.out.println("Erro blabla");
			
			e1.printStackTrace();
		}

        
    }
    
	public void downloadFile(String host, int port, String path, int i) throws IOException {
		
		Socket sock = new Socket(url.getHost(), port);
		

		OutputStream out = sock.getOutputStream();
		InputStream	in = sock.getInputStream();

		int start = i*REQUEST_SIZE;
		
		int end = (i+1)*REQUEST_SIZE-1;


		System.out.println("end: "+end);
		if(end > endRange - startRange)
			end = endRange - startRange;
		/*else if (startRange + end + REQUEST_SIZE-1 >size)
			end = size-startRange;
		/*
			System.out.println("start: "+start);
			System.out.println("end: "+end);
			System.out.println("startRange: "+startRange);
			System.out.println("start+startRange: "+start+startRange);
			System.out.println("size: "+size);
			System.out.println(start <= end && start+startRange < size);
		*/
		if(start <= end && start+startRange < size){
			String request = String.format(
				"GET %s HTTP/1.0\r\n"+
				"Host: %s\r\n"+
				"User-Agent: X-RC2020 HttpClient\r\n"+
				"Range: bytes=%d-%d\r\n\r\n", path, host, startRange + start, startRange + end);

			out.write(request.getBytes());

			System.out.println("\nSent Request:\n-------------\n"+request);		
			System.out.println("Got Reply:");
			System.out.println("\nReply Header:\n--------------");
			
			String answerLine = Http.readLine(in);  // first line is always present
			System.out.println(answerLine);
			String[] reply = Http.parseHttpReply(answerLine);
			int[] range  = new int[2];

			answerLine = Http.readLine(in);

			while (!answerLine.equals("") ) {
				System.out.println(answerLine);
				
				String[] head = Http.parseHttpHeader(answerLine);
				if((head[0].toLowerCase()).equals("Content-Range".toLowerCase())){
					String [] r = head[1].split(" ");
					String [] r1 = r[1].split("-");
					r1[1]=r1[1].split("/")[0];

					range[0] = Integer.parseInt(r1[0]);
					range[1] = Integer.parseInt(r1[1]);
				}
				answerLine = Http.readLine(in);
			}
			

			if ( reply[1].equals("200") || reply[1].equals("206")) {

				System.out.println("\nReply Body:\n--------------");
				long time0 = System.currentTimeMillis();
				int n;
				byte[] buffer = new byte[BUF_SIZE];
				
				int m = range[0];
				
				while( (n = in.read(buffer)) >= 0 ) {
					fos.write(buffer, 0, n);
					m+=n;
				}
				
			}
			else
				System.out.println("Ooops, received status:" + reply[1]);
			
			sock.close();
		}
	}
	   
}
