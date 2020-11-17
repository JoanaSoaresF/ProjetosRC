import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
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

			HTTPClient c = new HTTPClient(url, stat);
			size = c.getFileSize(out, in);
			sock.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
		return size;

	}
}
