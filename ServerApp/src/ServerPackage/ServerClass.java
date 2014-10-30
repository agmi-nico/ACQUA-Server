package ServerPackage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.Locale;

public class ServerClass {

	static ServerSocket serverSocket;
	Socket connection = null;
	// ObjectOutputStream
	BufferedOutputStream socketOut;
	BufferedInputStream socketIn;
	BufferedOutputStream fileWrite = null;
	BufferedInputStream fileRead = null;
	String message;

	String ok = "0";

	ServerClass() {
	}

	void setup() {
		// 1. Creating the server socket
		try {
			serverSocket = new ServerSocket(51254, 10);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Server started...");
	}

	protected Thread speedTestThread(final Socket connection) {
		return new Thread(new Runnable() {
			@Override
			public void run() {
				speedTest(connection);
			}
		});
	}

	private void speedTest(final Socket connection) {
		Calendar c = Calendar.getInstance(Locale.getDefault());
		String time;

		c.setTimeInMillis(System.currentTimeMillis());
		time = c.getTime().toString();
		System.out.println(time + " Connection received from "
				+ connection.getInetAddress().getHostAddress() + ":"
				+ connection.getInetAddress().getHostName());

		// 3. getting Socket Buffered Input and Output Streams
		try {
			socketOut = new BufferedOutputStream(connection.getOutputStream());
			socketIn = new BufferedInputStream(connection.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		c.setTimeInMillis(System.currentTimeMillis());
		time = c.getTime().toString();
		// Receiving the File
		byte[] fileSize = new byte[8];
		int bytesRead = 0, offset = 0;
		try {
			// confirmation to start sending file
			socketOut.write(ok.getBytes("UTF-8"), 0, 1);
			socketOut.flush();

			fileWrite = new BufferedOutputStream(new FileOutputStream(
					"/tmp/EchoFile.txt")); // Check path for Linux

			bytesRead = socketIn.read(fileSize); // Receive file size
		} catch (IOException e) {
			e.printStackTrace();
		}
		long fileLength = 0;
		for (int j = 0; j < fileSize.length; j++) {
			fileLength += ((long) fileSize[j] & 0xffL) << (8 * j);
		}

		System.out.println(time + " Receiving file...");
		byte[] fileBytes = new byte[50 * 1024 * 1024];
		try {
			while (offset < fileLength) {
				bytesRead = socketIn.read(fileBytes);
				fileWrite.write(fileBytes, 0, bytesRead);
				offset += bytesRead;
			}
			fileWrite.flush();

			c.setTimeInMillis(System.currentTimeMillis());
			time = c.getTime().toString();
			System.out.println(time + " File Received " + offset + "/"
					+ fileLength);
			fileWrite.close();

			// receive confirmation
			socketOut.write(ok.getBytes("UTF-8"), 0, 1);
			socketOut.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Sending Back the File
		try {
			fileRead = new BufferedInputStream(new FileInputStream(
					"/tmp/EchoFile.txt")); // Check path for Linux
			while ((bytesRead = fileRead.read(fileBytes)) > 0) {
				socketOut.write(fileBytes, 0, bytesRead);
				socketOut.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		c.setTimeInMillis(System.currentTimeMillis());
		time = c.getTime().toString();
		System.out.println(time + " File Sent to client " + offset + "/"
				+ fileLength);

		try {
			socketIn.close();
			socketOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void run() {
		Calendar c = Calendar.getInstance(Locale.getDefault());
		String time;

		// 2. Waiting for connection
		c.setTimeInMillis(System.currentTimeMillis());
		time = c.getTime().toString();
		System.out.println(time + " Waiting for a connection");

		try {
			connection = serverSocket.accept();
		} catch (IOException e) {
			System.err.println("Error accepting connection from client");
		}

		speedTest(connection);
		// Thread thread = speedTestThread(connection);
		// thread.start();
	}

	void stop() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ServerClass server = new ServerClass();
		server.setup();
		boolean running = true;
		while (running) {
			server.run();
		}
		server.stop();
	}
}