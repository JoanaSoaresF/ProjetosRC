import java.io.*;
import java.net.*;

/** 
* @author Goncalo Lourenco 55780
* @author Joana Faria 55754
*
*/
public class RequestSingleServer extends Thread {
    
    //constants
    private static final int BUF_SIZE = 512;
	private static final int REQUEST_SIZE = 500000;

    //variables
    private URL url;
    private int startRange;
    private int endRange;
    private int size;
    private Stats stat;
    private HTTPClient client;
    private int done, myTask;
    private int port;

    private Socket sock;
    private InputStream in;
    private OutputStream out;

	private FileOutputStream fos;
	private RandomAccessFile raf;

    public RequestSingleServer(URL url, int startRange, int endRange, File file, int totalsize, Stats stat){
        this.url = url;
		this.startRange = startRange;
		this.endRange = Math.min(endRange, totalsize-1);
        this.size = totalsize;
		this.stat = stat;
        done = 0;
        myTask = endRange - startRange + 1;
        port = url.getPort() == -1 ? 80 : url.getPort();

		try {
			raf = new RandomAccessFile(file, "rw");
			raf.seek(startRange);
			this.fos = new FileOutputStream(raf.getFD());

		} catch (Exception e) {
			e.printStackTrace();
        }
        

    }
    
    @Override
	public void run() {

		try {
            
            client = new HTTPClient(url);

            int start = startRange;
            int end = startRange+REQUEST_SIZE-1;  

            while(done < myTask && startRange+done < size){

                //connect socket
                while(!connect()){};
                
                int len = 0;
                
                if (end > endRange)
                        end = endRange;


                    
                if(start <= end && start < size){
                    len = client.partialRequest(out, in, start, end);
                    
                } 
                
                if(len>=0){
                    int written = writeOnFile();
                    if(len-1==written){
                        done+=len;
                        start+=len;
                        end+=len;
                        stat.newRequest(len); 
                    } else {
                        raf.seek(start);
                    }
                    System.out.println("arnold "+len+"   written   " + written);
                } 
                /*else {
                    smallerRequest(start, end);
                }*/
                
            }

			fos.close();
			raf.close();
			sock.close();
		} catch (Exception e) {
			e.printStackTrace();
		} 

    }
    private int writeOnFile() {

        int written = -1;
        try {
            int n;
            byte[] buffer = new byte[BUF_SIZE];
            while( (n = in.read(buffer)) >= 0 ) {
                fos.write(buffer, 0, n);
                written += n;
            }
            
        } catch (Exception e) {
           e.printStackTrace();
        }

        return written;
      
    }
    
    private boolean connect() {
		try {
            sock = new Socket(url.getHost(), port);
			out = sock.getOutputStream();
            in = sock.getInputStream();
            return true;
		} catch (IOException e) {
            e.printStackTrace();
            return false;
		}
		
    }

    private void smallerRequest(int start, int end){
       try{
            int medium = (int)(start+end/2); 
            int len = client.partialRequest(out, in, start, medium);
            if(len > 0){
                done+=len;
                len = client.partialRequest(out, in, medium, end);
                if(len > 0){
                    done+=len;
                }
            }

        } catch (Exception e) {
            connect();
            smallerRequest(start, end);
        }
    }

}