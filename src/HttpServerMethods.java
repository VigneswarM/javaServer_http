import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.json.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class HttpServerMethods {

	static final File DATA_DIR = new File(".");
	static final String DEFAULT_HTML = "index.html";
	static final String DEFAULT_GET = "get.json";
	static final String DEFAULT_POST = "post.json";
	static final String FILE_NOT_FOUND = "404.html";
	static final String FILE_CONFLICT = "409.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	static final String DATA = DATA_DIR + "\\Data\\";
	private static final DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	static final int PORT = 80;
	static Date date = new Date();

	static final boolean verbose = true;

	public HttpServerMethods() {

	}

	private synchronized byte[] readFileData(File file, int fileLength, Boolean verbose) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];

		if (verbose)
			System.out.println("Logging -" + sdf.format(date) + " :Reading data from " + file);

		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null)
				fileIn.close();
		}
		// System.out.println(fileData.toString());
		return fileData;
	}

	private synchronized String getContentType(String fileRequested) {
		if (fileRequested.endsWith(".htm") || fileRequested.endsWith(".html"))
			return "text/html";
		else if (fileRequested.endsWith("json"))
			return "application/json";
		else
			return "text/plain";
	}

	private synchronized void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested, Boolean verbose)
			throws IOException {
		File file = new File(DATA_DIR, FILE_NOT_FOUND);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength, verbose);

		out.println("HTTP/1.1 404 File Not Found");
		out.println("Server: Java HTTP Server : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length: " + fileLength);
		out.println();
		out.flush();

		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();

		if (verbose) {
			System.out.println("Logging -" + sdf.format(date) + " :File " + fileRequested + " not found");
		}
	}

	public synchronized void processUnImplementedMethodRequest(String requestedMethod, PrintWriter out, BufferedOutputStream dataOut,
			Boolean verbose) {
		try {

			if (verbose) {
				System.out.println(
						"Logging -" + sdf.format(date) + " :501 Not Implemented : " + requestedMethod + " method.");
			}

			File file = new File(DATA_DIR, METHOD_NOT_SUPPORTED);
			int fileLength = (int) file.length();
			String contentMimeType = "text/html";
			byte[] fileData = readFileData(file, fileLength, verbose);

			out.println("HTTP/1.1 501 Not Implemented");
			out.println("Server: Java HTTP Server : 1.0");
			out.println("Date: " + new Date());
			out.println("Content-type: " + contentMimeType);
			out.println("Content-length: " + fileLength);
			out.println();
			out.flush();
			dataOut.write(fileData, 0, fileLength);
			dataOut.flush();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public synchronized void processGetMethodRequest(ArrayList<String> headers, String method, String fileRequested, PrintWriter out,
			BufferedOutputStream dataOut, Boolean isFileServer, Boolean verbose) {
		if (!isFileServer) {
			try {
				if (fileRequested.endsWith("/")) {
					files_fetch(DATA, out, verbose);
					out.println();
					out.flush();
					if (verbose) {
						System.out.println(
								"Logging -" + sdf.format(date) + " :Returned list of  files in " + DATA + " directory");
					}

				} else {
					File file = new File(DATA, fileRequested);
					int fileLength = (int) file.length();
					String content = getContentType(fileRequested);
					byte[] fileData = readFileData(file, fileLength, verbose);

					// System.out.println(fileRequested);

					out.println("HTTP/1.0 200 OK");
					out.println("Server: Assignment II HTTP Server: 1.0");
					out.println("Date: " + new Date());
					out.println("Content-type: " + content);
					out.println("Content-disposition:" + "attachment; filename=" + fileRequested);
					out.println("Content-length: " + fileLength);
					out.println();
					out.flush();
					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
					if (verbose) {
						System.out.println("Logging -" + sdf.format(date) + " :File " + fileRequested + " of type "
								+ content + " returned");
					}
				}
			} catch (FileNotFoundException fe) {
				try {
					fileNotFound(out, dataOut, fileRequested, verbose);
				} catch (IOException ioe) {
					System.err.println("Error with file not found exception : " + ioe.getMessage());
				}
			} catch (IOException ioExc) {
				System.out.println(ioExc.getMessage());
			}
		} else {
			processNormalGetMethodRequest(headers, method, fileRequested, out, dataOut, verbose);
		}
	}

	private synchronized void processNormalGetMethodRequest(ArrayList<String> headers, String method, String fileRequested,
			PrintWriter out, BufferedOutputStream dataOut, Boolean verbose) {
		// System.out.println("Headers :: " + headers);
		// headers.stream().forEach(System.out::println);

		try {
			if (fileRequested.endsWith("/")) {
				fileRequested += DEFAULT_GET;
			}
			File file = new File(DATA_DIR, fileRequested);
			int fileLength = (int) file.length();
			String content = getContentType(fileRequested);
			byte[] fileData = readFileData(file, fileLength, verbose);
			out.println("HTTP/1.0 200 OK");
			out.println("Server: Assignment II HTTP Server: 1.0");
			out.println("Date: " + new Date());
			out.println("Content-type: " + content);
			if (!headers.isEmpty()) {
				processHeaders(file, headers, out, fileLength, fileData, dataOut, verbose);
			} else {
				out.println("Content-length: " + fileLength);
				out.println();
				out.flush();
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
				if (verbose) {
					System.out.println("Logging -" + sdf.format(date) + " :File " + fileRequested + " of type "
							+ content + " returned");
				}
			}
		} catch (FileNotFoundException fe) {
			try {
				fileNotFound(out, dataOut, fileRequested, verbose);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
		} catch (IOException ioExc) {
			System.out.println(ioExc.getMessage());
		}
	}

	private synchronized void processHeaders(File file, ArrayList<String> headers, PrintWriter out, int fileLength, byte[] fileData,
			BufferedOutputStream dataOut, boolean verbose) {

		if (verbose)
			System.out.println("Logging -" + sdf.format(date) + " :Processing Header info");

		try {
			ArrayList<String> fileLines = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new FileReader(file))) {
				String line;
				while ((line = br.readLine()) != null && !line.equals("\n")) {
					if (line.contains("Content-Type")) {
						if (headers.stream().anyMatch(x -> x.contains("Content-Type"))) {
							List<String> contentTypeLines = headers.stream().filter(x -> x.contains("Content-Type"))
									.collect(Collectors.toList());
							String s[] = contentTypeLines.get(contentTypeLines.size() - 1).split(":");
							fileLines.add("    " + '"' + s[0] + '"' + ":" + '"' + s[1] + '"');
						} else {
							fileLines.add(line);
						}
						for (String header : headers) {
							if (!header.contains("Content-Type")) {
								String s[] = header.split(":");
								fileLines.add("    " + '"' + s[0] + '"' + ":" + '"' + s[1] + '"');
							}
						}
					} else {
						fileLines.add(line);
					}
				}
				fileLines.stream().forEach(System.out::println);
				FileWriter writer = new FileWriter("temp");
				for (String str : fileLines) {
					writer.write(str);
					writer.write("\n");
				}
				writer.close();
				File filetemp = new File("temp");
				fileLength = (int) filetemp.length();
				fileData = readFileData(filetemp, fileLength, verbose);
				out.println("Content-length: " + fileData.length);
				out.println();
				out.flush();
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	private synchronized byte[] insertDataField(String dataField, byte[] fileData) {
		try {
			String fileDataStr = new String(fileData, "UTF-8");
			JSONObject obj = new JSONObject(fileDataStr);
			obj.put("data", dataField);
			obj.put("json", dataField);
			try (FileWriter file = new FileWriter(DEFAULT_POST)) {
				Gson gson1 = new GsonBuilder().setPrettyPrinting().create();
				JsonParser jp1 = new JsonParser();
				JsonElement je1 = jp1.parse(obj.toString());
				String prettyJsonString1 = gson1.toJson(je1);
				file.write(prettyJsonString1.toString());
				file.flush();
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	private synchronized void processNormalPostMethodRequest(ArrayList<String> headers, String method, String fileRequested,
			PrintWriter out, BufferedOutputStream dataOut, String dataField, Boolean verbose) {
		try {
			if (fileRequested.endsWith("/post")) {
				fileRequested = DEFAULT_POST;
			}

			File file = new File(DATA_DIR, fileRequested);
			int fileLength = (int) file.length();
			String content = getContentType(fileRequested);

			out.println("HTTP/1.0 200 OK");
			out.println("Server: Assignment II HTTP Server: 1.0");
			out.println("Date: " + new Date());
			out.println("Content-type: " + content);
			byte[] fileData = readFileData(file, fileLength, verbose);
			insertDataField(dataField, fileData);
			if (!headers.isEmpty()) {
				processHeaders(file, headers, out, fileLength, fileData, dataOut, verbose);
			} else {
				out.println("Content-length: " + fileLength);
				out.println();
				out.flush();
				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
				if (verbose) {
					System.out.println("Logging -" + sdf.format(date) + " :File " + fileRequested + " of type "
							+ content + " returned");
				}
			}
		} catch (FileNotFoundException fe) {
			try {
				fileNotFound(out, dataOut, fileRequested, verbose);
			} catch (IOException ioe) {
				System.err.println("Error with file not found exception : " + ioe.getMessage());
			}
		} catch (IOException ioExc) {
			System.out.println(ioExc.getMessage());
		}

	}

	public synchronized void processPostMethodRequest(ArrayList<String> headers, String method, String fileRequested,
			PrintWriter out, BufferedOutputStream dataOut, Boolean isFileServer, String dataField, Boolean verbose,
			String isOverWrite) {
		if (!isFileServer) {
			try {
				File file = new File(DATA, fileRequested);
				String content = getContentType(fileRequested);
				int fileLength = (int) file.length();
				File f = new File(DATA + fileRequested);
				if (f.exists()) {
					if (isOverWrite.contains("y")) {
						System.out.println(isOverWrite);
						FileWriter writer = new FileWriter(DATA + fileRequested);
						writer.write(dataField);
						writer.close();
						out.println("HTTP/1.0 200 OK");
						out.println("Server: Assignment II HTTP Server: 1.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + content);
						out.println("Content-length: " + fileLength);
						out.println();
						out.flush();
						if (verbose) 
							System.out.println("Logging -"+sdf.format(date)+" :Overwriting data at: "+DATA+fileRequested);
						dataOut.write(dataField.getBytes(), 0, fileLength);
						dataOut.flush();
					}else{
						File file2 = new File(DATA_DIR, FILE_CONFLICT);
						int fileLength2 = (int) file2.length();
						String content2 = "text/html";
						byte[] fileData2 = readFileData(file2, fileLength2, verbose);

						out.println("HTTP/1.1 409 Conflict");
						out.println("Server: Java HTTP Server : 1.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + content2);
						out.println("Content-length: " + fileLength2);
						if (verbose) 
							System.out.println("Logging -"+sdf.format(date)+" :FILE CONFLICT: "+DATA+fileRequested);
						out.println();
						out.flush();

						dataOut.write(fileData2, 0, fileLength2);
						dataOut.flush();
					}
				} else {
					FileWriter writer = new FileWriter(DATA + fileRequested);
					writer.write(dataField);
					writer.close();
					out.println("HTTP/1.0 200 OK");
					out.println("Server: Assignment II HTTP Server: 1.0");
					out.println("Date: " + new Date());
					out.println("Content-type: " + content);
					out.println("Content-length: " + fileLength);
					out.println();
					out.flush();
					if (verbose) 
						System.out.println("Logging -"+sdf.format(date)+" :Writing data at: "+DATA+fileRequested);
					dataOut.write(dataField.getBytes(), 0, fileLength);
					dataOut.flush();
				}
			} catch (FileNotFoundException fe) {
				try {
					fileNotFound(out, dataOut, fileRequested, verbose);
				} catch (IOException ioe) {
					System.err.println("Error with file not found exception : " + ioe.getMessage());
				}
			} catch (IOException ioExc) {
				System.out.println(ioExc.getMessage());
			}

		} else {
			processNormalPostMethodRequest(headers, method, fileRequested, out, dataOut, dataField, verbose);
		}

	}

	private synchronized static void files_fetch(String dirName, PrintWriter out, Boolean verbose) {

		File directory = new File(dirName);
		File[] filesArray = directory.listFiles();
		Arrays.sort(filesArray);
		out.println("HTTP/1.0 200 OK");
		out.println("Server: Assignment II HTTP Server: 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + "text/plain");
		int length = fetch_length(filesArray);

		if (verbose)
			System.out.println("Logging -" + sdf.format(date) + " :Fetching files from " + dirName);

		out.println("Content-length: " + length);
		out.println("\n-- Files --");

		for (File file : filesArray) {
			if (file.isFile())
				out.println("file: " + file.getName());
			else if (file.isDirectory())
				out.println("Directory: " + file.getName());
			else
				out.println("Unknown: " + file.getName());
		}

	}

	private synchronized static int fetch_length(File[] filesArray) {
		int sum = 0;
		for (File file : filesArray) {
			if (file.isFile())
				sum += ("file: " + file.getName()).length();
			else if (file.isDirectory())
				sum += ("Directory: " + file.getName()).length();
			else
				sum += ("Unknown: " + file.getName()).length();
		}
		return sum;

	}

}
