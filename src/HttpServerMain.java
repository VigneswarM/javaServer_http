import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.StringTokenizer;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class HttpServerMain implements Runnable {

	private static Socket connect;
	private static Boolean isFileServerMode;
	private static Boolean isverbose = false;
	static Date date = new Date();
	static String isOverWrite;
	private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	public HttpServerMain(Socket c) {
		connect = c;
	}

	public static void main(String[] args) {

		OptionParser parser = new OptionParser();

		// Getting the port number
		final String[] portOptions = { "port", "p" };
		parser.acceptsAll(Arrays.asList(portOptions), "EchoServer listening port").withOptionalArg().defaultsTo("80");

		// getting the directory from the user
		String cwd = System.getProperty("user.dir");
		final String[] directoryOptions = { "directory", "d" };
		parser.acceptsAll(Arrays.asList(directoryOptions), "opening directory").withOptionalArg().defaultsTo(cwd);

		// checking if Isverbose
		final String[] verboseOptions = { "verbose", "v" };
		parser.acceptsAll(Arrays.asList(verboseOptions), "Verbose logging - Print debugging messages.");

		// checking if Isverbose
		final String[] FileServerOptions = { "isFileServer", "n" };
		parser.acceptsAll(Arrays.asList(FileServerOptions), "Is Normal server - true/false?");

		OptionSet opts = parser.parse(args);
		int port = Integer.parseInt((String) opts.valueOf("port"));
		String dir = (String) opts.valueOf("directory");
		isverbose = opts.has("verbose");
		Boolean isFileServer = opts.has("isFileServer");

		/*
		 * System.out.println(isverbose); System.out.println(port);
		 * System.out.println(dir); System.out.println(isFileServer);
		 */
		isFileServerMode = isFileServer;

		startServer(port);
	}

	public static void startServer(int PORT) {
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			synchronized (serverConnect) {
				System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

				if (isverbose)
					System.out.println(
							"Logging -" + sdf.format(date) + " :Server started  on port : " + PORT + " ...\\n");

				while (true) {
					HttpServerMain myServer = new HttpServerMain(serverConnect.accept());
					System.out.println("Connecton opened. (" + new Date() + ")");
					Thread thread = new Thread(myServer);
					thread.start();
				}
			}

		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	private static synchronized void validateRequestedMethod() {
		BufferedReader in = null;
		PrintWriter out = null;
		BufferedOutputStream dataOut = null;
		String fileRequested = null;

		try {
			synchronized (connect) {

				in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
				out = new PrintWriter(connect.getOutputStream());
				dataOut = new BufferedOutputStream(connect.getOutputStream());
				synchronized (dataOut) {
					String line;
					int length = 0;
					ArrayList<String> inputLines = new ArrayList<>();
					ArrayList<String> headers = new ArrayList<>();
					String dataField = null;
					while ((line = in.readLine()) != null) {
						// System.out.println(line);
						inputLines.add(line);
						if (line.contains("Overwrite"))
							isOverWrite = line.split(":")[1];
						if (line.contains("Content-Type") || line.contains("Accept"))
							headers.add(line);
						if (inputLines.get(0).contains("POST") && line.contains("{")) {
							dataField = line;
							break;
						} else if (inputLines.get(0).contains("GET") && line.contains("User-Agent")) {
							break;
						}
					}
					String input = inputLines.get(0);

					StringTokenizer parse = new StringTokenizer(input);

					String method = parse.nextToken().toUpperCase();
					fileRequested = parse.nextToken().toLowerCase();
					HttpServerMethods httpServerMethods = new HttpServerMethods();
					if (isverbose)
						System.out.println("Logging -" + sdf.format(date) + " :Validating request : " + method);

					if (!method.equals("GET") && !method.equals("POST")) {
						httpServerMethods.processUnImplementedMethodRequest(method, out, dataOut, isverbose);
					} else if (method.equals("GET")) {
						httpServerMethods.processGetMethodRequest(headers, method, fileRequested, out, dataOut,
								isFileServerMode, isverbose);
					} else {
						httpServerMethods.processPostMethodRequest(headers, method, fileRequested, out, dataOut,
								isFileServerMode, dataField, isverbose, isOverWrite);
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		} finally {
			try {
				in.close();
				out.close();
				dataOut.close();
				connect.close();
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			}

			//System.out.println("Connection closed.\n");

		}

	}

	@Override
	public void run() {
		validateRequestedMethod();
	}

}
