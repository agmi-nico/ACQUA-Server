package fr.inria.acqua;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SHIFTServer {

	protected static boolean run = true;
	final static int udpPort = 9052;
	static int udpPacketSize = 16;

	public static void main(String[] args) {
		DatagramSocket udpSocket = null;

		ByteBuffer byteBuilder;

		byte[] udpPacket;
		DatagramPacket udpReceivePacket = null;

		try {
			udpSocket = new DatagramSocket(udpPort);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Log.d("Server Started");

		udpPacket = new byte[udpPacketSize];

		try {
			udpSocket.setSoTimeout(0);
		} catch (Exception e) {
		}

		InetAddress IPAddress = null;
		udpReceivePacket = new DatagramPacket(udpPacket, udpPacket.length);

		long timestamp;
		int packetDelay;
		long receiveTime;
		int id;
		while (run) {
			try {
				udpSocket.receive(udpReceivePacket);
			} catch (Exception e) {
				// e.printStackTrace();
				Log.e("Error receiving udp packet");
			}
			receiveTime = System.currentTimeMillis();
			Log.d("Packet received");
			byteBuilder = ByteBuffer.wrap(udpReceivePacket.getData());
			byteBuilder.order(ByteOrder.LITTLE_ENDIAN);
			id = byteBuilder.getInt(0);
			timestamp = byteBuilder.getLong(4);
			packetDelay = (int) (receiveTime - timestamp);
			IPAddress = udpReceivePacket.getAddress();

			byteBuilder = ByteBuffer.wrap(udpPacket);
			// id
			byteBuilder.put(0, (byte) (id & 0xff));
			byteBuilder.put(1, (byte) ((id >> 8) & 0xff));
			byteBuilder.put(2, (byte) ((id >> 16) & 0xff));
			byteBuilder.put(3, (byte) ((id >> 24) & 0xff));
			// delay
			byteBuilder.put(12, (byte) (packetDelay & 0xff));
			byteBuilder.put(13, (byte) ((packetDelay >> 8) & 0xff));
			byteBuilder.put(14, (byte) ((packetDelay >> 16) & 0xff));
			byteBuilder.put(15, (byte) ((packetDelay >> 24) & 0xff));
			// timestamp
			timestamp = System.currentTimeMillis();
			byteBuilder.put(4, (byte) (timestamp & 0xff));
			byteBuilder.put(5, (byte) ((timestamp >> 8) & 0xff));
			byteBuilder.put(6, (byte) ((timestamp >> 16) & 0xff));
			byteBuilder.put(7, (byte) ((timestamp >> 24) & 0xff));
			byteBuilder.put(8, (byte) ((timestamp >> 32) & 0xff));
			byteBuilder.put(9, (byte) ((timestamp >> 40) & 0xff));
			byteBuilder.put(10, (byte) ((timestamp >> 48) & 0xff));
			byteBuilder.put(11, (byte) ((timestamp >> 56) & 0xff));

			try {
				udpSocket.send(new DatagramPacket(byteBuilder.array(),
						udpPacketSize, IPAddress, udpPort));
				Log.d("Packet send");
			} catch (Exception e) {
				Log.e("Error sending udp packet");
				continue;
			}

		}

		udpSocket.close();
	}
}