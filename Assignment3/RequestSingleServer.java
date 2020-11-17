import java.io.*;
import java.net.*;

/**
 * @author Goncalo Lourenco 55780
 * @author Joana Faria 55754
 *
 */
public class RequestSingleServer extends Thread {

    // constants
    public static final int REQUEST_SIZE = 750000;

    // variables
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

    public RequestSingleServer(URL url, int startRange, int endRange, File file, int totalSize, Stats stat) {
        this.url = url;
        this.startRange = startRange;
        this.endRange = Math.min(endRange, totalSize - 1);
        this.size = totalSize;
        this.stat = stat;
        done = 0;
        myTask = endRange - startRange + 1;
        port = url.getPort() == -1 ? 80 : url.getPort();
        sock = null;

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
            int end = startRange + REQUEST_SIZE - 1;

            while (done < myTask && startRange + done < size) {

                // connect socket
                if (sock == null || !sock.isClosed())
                    while (!connect()) {
                    }
                ;

                byte[] reply = null;

                if (end > endRange)
                    end = endRange;

                if (start <= end && start < size) {
                    reply = client.partialRequest(out, in, start, end);
                }

                if (reply != null) {
                    writeOnFile(reply);
                    int len = reply.length;
                    done += len;
                    start += len;
                    end += len;
                    stat.newRequest(len);
                }
            }

            fos.close();
            raf.close();
            sock.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void writeOnFile(byte[] reply) {

        try {
            fos.write(reply, 0, reply.length);

        } catch (Exception e) {
            e.printStackTrace();
        }

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
}