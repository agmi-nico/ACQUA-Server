package fr.inria.acqua;

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

	protected static boolean run = true;
	final static int numberProbes = 200;
	final static int udpPort = 9050;
	final static int tcpPort = 9051;

	public static void main(String[] args) {
		DatagramSocket udpSocket = null;

		int udpPacketSize = 12;
		int fullPacketSize = 28 + udpPacketSize;

		ServerSocket tcpServer = null;
		Socket tcpSocket = null;
		BufferedOutputStream tcpOut = null;
		BufferedInputStream tcpIn = null;

		ByteBuffer byteBuilder;

		byte[] udpPacket;
		DatagramPacket udpReceivePacket = null;

		try {
			udpSocket = new DatagramSocket(udpPort);
			tcpServer = new ServerSocket(tcpPort);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d("Server Started");

		while (run) {

			udpPacket = new byte[udpPacketSize];

			InetAddress IPAddress = null;
			try {
				udpSocket.setSoTimeout(0);
			} catch (Exception e) {
			}

			long delaySum = 0;
			long startTime = 0;
			long endTime = 0;
			int receivedPackets = 0;
			udpReceivePacket = new DatagramPacket(udpPacket, udpPacket.length);

			Log.d("Waiting for udp packets");
			boolean downloading = true;
			while (downloading) {
				try {
					udpSocket.receive(udpReceivePacket);
					if (startTime == 0) {
						udpSocket.setSoTimeout(5000);
						startTime = System.currentTimeMillis();
						IPAddress = udpReceivePacket.getAddress();
					}
					endTime = System.currentTimeMillis();
					byteBuilder = ByteBuffer.wrap(udpReceivePacket.getData());
					byteBuilder.order(ByteOrder.LITTLE_ENDIAN);
					// int id = byteBuilder.getInt(0);
					long timestamp = byteBuilder.getLong(4);

					delaySum += (endTime - timestamp);
					receivedPackets++;
				} catch (Exception c) {
					downloading = false;
				}
			}
			Log.d("Received: " + receivedPackets);

			// send number back
			Log.d("Waiting for tcp connection from client");
			try {
				tcpSocket = tcpServer.accept();
				tcpOut = new BufferedOutputStream(tcpSocket.getOutputStream());
				tcpIn = new BufferedInputStream(tcpSocket.getInputStream());
			} catch (IOException e) {
				Log.e("Error establishing tcp connection to client");
				continue;
			}

			int delay = (int) (delaySum / receivedPackets);
			int bandwith = (int) (receivedPackets * fullPacketSize / (endTime - startTime));
			int lossRate = 100 * (numberProbes - receivedPackets);

			Log.d("Sending delay:" + delay + " bandwith:" + bandwith
					+ " lossRate:" + lossRate + " to client");
			byteBuilder = ByteBuffer.allocate(12);

			// delay
			byteBuilder.put((byte) (delay & 0xff));
			byteBuilder.put((byte) ((delay >> 8) & 0xff));
			byteBuilder.put((byte) ((delay >> 16) & 0xff));
			byteBuilder.put((byte) ((delay >> 24) & 0xff));
			// badnwith
			byteBuilder.put((byte) (bandwith & 0xff));
			byteBuilder.put((byte) ((bandwith >> 8) & 0xff));
			byteBuilder.put((byte) ((bandwith >> 16) & 0xff));
			byteBuilder.put((byte) ((bandwith >> 24) & 0xff));
			// lossrate
			byteBuilder.put((byte) (lossRate & 0xff));
			byteBuilder.put((byte) ((lossRate >> 8) & 0xff));
			byteBuilder.put((byte) ((lossRate >> 16) & 0xff));
			byteBuilder.put((byte) ((lossRate >> 24) & 0xff));

			try {
				tcpOut.write(byteBuilder.array(), 0, 4);
				tcpOut.flush();
			} catch (Exception e1) {
				Log.e("Error sending measurements results to client");
				e1.printStackTrace();
				continue;
			}

			try {
				byte[] ok = new byte[1];
				if (tcpIn.read(ok, 0, 1) != 1) {
					throw new Exception();
				}
				tcpOut.close();
				tcpIn.close();
				tcpSocket.close();

				Log.d("Waiting OK");
			} catch (Exception e1) {
				Log.e("Error receiving OK from client");
				continue;
			}

			Log.d("Sending " + numberProbes + " udp packets to client");
			for (int i = 0; i < numberProbes; i++) {
				try {
					byteBuilder = ByteBuffer.allocate(12);
					// id
					byteBuilder.put((byte) (i & 0xff));
					byteBuilder.put((byte) ((i >> 8) & 0xff));
					byteBuilder.put((byte) ((i >> 16) & 0xff));
					byteBuilder.put((byte) ((i >> 24) & 0xff));
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

					udpPacket = byteBuilder.array();
					udpSocket.send(new DatagramPacket(udpPacket, udpPacketSize,
							IPAddress, tcpPort));
				} catch (Exception e) {
					Log.e("Error sending udp packet to client");
					continue;
				}
			}
			Log.d("Done");
		}

		try {
			tcpServer.close();
		} catch (IOException e) {
		}
		udpSocket.close();
	}
}