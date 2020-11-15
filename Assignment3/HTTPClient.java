import java.io.*;
import java.net.*;

public class HTTPClient extends Thread {
	private static final int BUF_SIZE = 512;
	private static final int REQUEST_SIZE = 2000000;

	private Stats stat;
	private URL url;
	private int startRange;
	private int endRange;
	private int size;
	private FileOutputStream fos;
	private RandomAccessFile raf;
	private OutputStream out;
	private InputStream in;
	private int cicle;
	private boolean done;
	private int seek;

	int aux = 0;

	private Socket sock;

	public HTTPClient(URL url, int startRange, int endRange, File file, int totalsize, Stats stat) {
		this.url = url;
		this.startRange = startRange;
		this.endRange = endRange;
		this.size = totalsize;
		this.cicle = 0;
		this.stat = stat;
		done = false;
	
		try {
			raf = new RandomAccessFile(file, "rw");
			raf.setLength(startRange);
			raf.seek(startRange);
			this.fos = new FileOutputStream(raf.getFD());

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void run() {

		int port = url.getPort() == -1 ? 80 : url.getPort();
		String path = url.getPath() == "" ? "/" : url.getPath();
		try {
			sock = new Socket(url.getHost(), port);
			out = sock.getOutputStream();
			in = sock.getInputStream();
		
			int i = cicle;
			while (startRange + i * REQUEST_SIZE <= endRange) {
				cicle = i;

				int start = i * REQUEST_SIZE;
				int end = (i + 1) * REQUEST_SIZE - 1;

				if (end > endRange - startRange)
						end = endRange - startRange;

				start += startRange;
				end+= startRange;
				downloadFile(url.getHost(), path, start, end);
				i++;
			}

			if (!done)
				run();

			fos.close();
			raf.close();
			sock.close();
		} catch (SocketException e) {
			cicle--;
			run();

		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	public void downloadFile(String host, String path, int start, int end) throws UnknownHostException, IOException {
	
		if (start <= end && start <= size) {
			String request = String.format("GET %s HTTP/1.0\r\n" + "Host: %s\r\n"
					+ "User-Agent: X-RC2020 HttpClient\r\n" + "Range: bytes=%d-%d\r\n\r\n", path, host,
					start, end);

		
			out.write(request.getBytes());
	
			System.out.println("\nSent Request:\n-------------\n"+request);		
			System.out.println("Got Reply:");
			System.out.println("\nReply Header:\n--------------");
			
			String answerLine = Http.readLine(in);  // first line is always present
			System.out.println(answerLine);
			String[] reply = Http.parseHttpReply(answerLine);
			int[] range  = new int[2];
			int sended = 0;

			answerLine = Http.readLine(in);

			while (!answerLine.equals("") ) {
				System.out.println(answerLine);
				
				
				String[] head = Http.parseHttpHeader(answerLine);
				if((head[0].toLowerCase()).equals("Content-Length".toLowerCase())){
					sended = Integer.parseInt(head[1]);
					
					/*if(a<end-start+1 && startRange + end < this.size-1){
						System.out.printf("Arnold dentro : %d   %d   %d   %d\n", a, (end-start), (startRange + end), this.size);
						downloadFile(host, port, path, i);	
					}*/


				} else if((head[0].toLowerCase()).equals("Content-Range".toLowerCase())){
					String [] r = head[1].split(" ");
					String [] r1 = r[1].split("-");
					r1[1]=r1[1].split("/")[0];

					

					range[0] = Integer.parseInt(r1[0]);
					range[1] = Integer.parseInt(r1[1]);
					//raf.seek(range[0]);

					if(range[1]>aux)
						aux = range[1];
					if(range[1] >= endRange || range[1] >= this.size-1)
						done = true;
						
				}
				answerLine = Http.readLine(in);
			}
			

			if ( reply[1].equals("200") || reply[1].equals("206")) {

				stat.newRequest(end-start+1);

				System.out.println("\nReply Body:\n--------------");
				int n;
				byte[] buffer = new byte[BUF_SIZE];
	
			
				
				if(range[1]<end && range[1] < this.size-1){
					System.out.println("Entra2");
					in.readAllBytes();
					downloadFile(host, path,  start, end);
				} else {
					while( (n = in.read(buffer)) >= 0 ) {
				
						//raf.seek(range[0]);
						fos.write(buffer, 0, n);
	
					}

				}

				/*if(range[0]>start) {
					System.out.println("Entra1");
					//raf.seek(start);
					downloadFile(host, path, start, range[0]-1);
				
				}

				if(range[1]<end && end < this.size-1) {
					System.out.println("Entra2");
					raf.seek(range[1]+1);
					downloadFile(host, path, range[1]+1, end);

				}*/
			}	
		} else { 
			System.out.println(start);
			System.out.println(this.size);
			System.out.println(aux);
			done = true;
		}
	}	   
}
