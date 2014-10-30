import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class UDPServer {

	public static boolean run = true;

	public static void main(String[] args) {
		DatagramSocket udpSocket = null;

		ServerSocket tcpSocket = null;
		Socket connection = null;
		BufferedOutputStream tcpOut = null;
		BufferedInputStream tcpIn = null;

		ByteBuffer byteBuilder;

		try {
			udpSocket = new DatagramSocket(9050);
			tcpSocket = new ServerSocket(9051);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d("Server Started");

		while (run) {

			byte[] receiveData = new byte[1024];
			byte[] sendData = null;
			int receivedPacketsCount = 0;
			DatagramPacket receivePacket = null;
			int port = 9050;
			InetAddress IPAddress = null;
			try {
				udpSocket.setSoTimeout(0);
			} catch (Exception e) {
			}
			Log.d("Waiting for udp packets");
			while (run) {
				try {
					receivePacket = new DatagramPacket(receiveData,
							receiveData.length);
					udpSocket.receive(receivePacket);
					IPAddress = receivePacket.getAddress();
					udpSocket.setSoTimeout(10000);
					receivedPacketsCount++;
				} catch (Exception c) {
					break;
				}
			}

			Log.d("Received: " + receivedPacketsCount);

			// send number back
			Log.d("Waiting for client to connect");
			try {
				connection = tcpSocket.accept();
				tcpOut = new BufferedOutputStream(connection.getOutputStream());
				tcpIn = new BufferedInputStream(connection.getInputStream());
			} catch (IOException e) {
				Log.e("Error establishing tcp connection to client");
				continue;
			}

			Log.d("Sending " + receivedPacketsCount + " to client");
			try {
				byteBuilder = ByteBuffer.allocate(4);
				byteBuilder.put((byte) (receivedPacketsCount & 0xff));
				byteBuilder.put((byte) ((receivedPacketsCount >> 8) & 0xff));
				byteBuilder.put((byte) ((receivedPacketsCount >> 16) & 0xff));
				byteBuilder.put((byte) ((receivedPacketsCount >> 24) & 0xff));

				tcpOut.write(byteBuilder.array(), 0, 4);
				tcpOut.flush();
			} catch (Exception e1) {
				Log.e("Error sending count of udp packets to client");
				e1.printStackTrace();
				continue;
			}

			int downProbes = -1;
			int downPPS = -1;

			Log.d("Waiting for instructions for download test");
			try {

				byte[] downloadData = new byte[4];

				// read number_probes
				if (tcpIn.read(downloadData, 0, 4) != 4) {
					throw new Exception();
				}
				byteBuilder = ByteBuffer.wrap(downloadData);
				byteBuilder.order(ByteOrder.LITTLE_ENDIAN);
				downProbes = byteBuilder.getInt();

				// read 1000/download_pps
				if (tcpIn.read(downloadData, 0, 4) != 4) {
					throw new Exception();
				}
				byteBuilder = ByteBuffer.wrap(downloadData);
				byteBuilder.order(ByteOrder.LITTLE_ENDIAN);
				downPPS = byteBuilder.getInt();

				tcpOut.close();
				tcpIn.close();
				connection.close();

			} catch (Exception e1) {
				Log.e("Error receiving download instructions");
				continue;
			}

			Log.d("Sending " + downProbes + " udp packets to client");
			for (short i = 0; i < downProbes; i++) {
				try {
					byteBuilder = ByteBuffer.allocate(10);
					// id
					byteBuilder.put((byte) (i & 0xff));
					byteBuilder.put((byte) ((i >> 8) & 0xff));
					// timestamp
					long timestamp = System.currentTimeMillis();
					byteBuilder.put((byte) (timestamp & 0xff));
					byteBuilder.put((byte) ((timestamp >> 8) & 0xff));
					byteBuilder.put((byte) ((timestamp >> 16) & 0xff));
					byteBuilder.put((byte) ((timestamp >> 24) & 0xff));
					byteBuilder.put((byte) ((timestamp >> 32) & 0xff));
					byteBuilder.put((byte) ((timestamp >> 40) & 0xff));
					byteBuilder.put((byte) ((timestamp >> 48) & 0xff));
					byteBuilder.put((byte) ((timestamp >> 56) & 0xff));

					sendData = byteBuilder.array();
					udpSocket.send(new DatagramPacket(sendData,
							sendData.length, IPAddress, port));
					Thread.sleep(downPPS);
				} catch (Exception e) {
					Log.e("Error sending udp packet to client");
					continue;
				}
			}
			Log.d("Done");

		}

		// end
		try {
			tcpSocket.close();
		} catch (IOException e) {
		}
		udpSocket.close();
	}
}