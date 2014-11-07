package fr.inria.acqua;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ACQUAServer {

	protected static boolean run = true;
	final static int numberProbes = 100;
	final static int udpPort = 9050;
	final static int tcpPort = 9051;
	final static int measurementSize = 16;
	final static int udpPacketSize = measurementSize + 60;

	final static int fullPacketSize = 28 * 8 + udpPacketSize * 8;

	public static void main(String[] args) {
		DatagramSocket udpSocket = null;

		ServerSocket tcpServer = null;
		Socket tcpSocket = null;
		BufferedOutputStream tcpOut = null;
		BufferedInputStream tcpIn = null;

		ByteBuffer byteBuilder;

		byte[] udpPacket;
		DatagramPacket udpReceivePacket = null;

		int experimentID = -1;

		try {
			udpSocket = new DatagramSocket(udpPort);
			tcpServer = new ServerSocket(tcpPort);
		} catch (Exception e) {
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

			// long delaySum = 0;
			long packetDelay = Long.MAX_VALUE;
			long lowestDelay = Long.MAX_VALUE;
			long startTime = 0;
			long endTime = 0;
			int receivedPackets = 0;
			udpReceivePacket = new DatagramPacket(udpPacket, udpPacket.length);
			int socketTimeout = 5000;

			Log.d("Waiting for udp packets");
			boolean downloading = true;
			long timestamp;
			int expID = -1;
			// ArrayList<Long> clientTimes = new ArrayList<Long>();
			// ArrayList<Long> serverTimes = new ArrayList<Long>();
			// ArrayList<Long> delayTimes = new ArrayList<Long>();

			// long totalTime;
			while (downloading) {
				try {
					udpSocket.receive(udpReceivePacket);
					endTime = System.currentTimeMillis();
					byteBuilder = ByteBuffer.wrap(udpReceivePacket.getData());
					byteBuilder.order(ByteOrder.LITTLE_ENDIAN);
					timestamp = byteBuilder.getLong(4);
					packetDelay = (endTime - timestamp);
					expID = byteBuilder.getInt(12);
					if (startTime == 0) {
						startTime = endTime;
						udpSocket.setSoTimeout(socketTimeout);
						// totalTime = startTime;
						// Log.d("start time " + startTime);
						IPAddress = udpReceivePacket.getAddress();
						experimentID = expID;
					} else {
						if (experimentID != expID) {
							Log.e("Experiements IDs do not match "
									+ experimentID + "!=" + expID);
						}
					}
					if (Math.abs(packetDelay) < Math.abs(lowestDelay)) {
						lowestDelay = packetDelay;
					}
					// delaySum += packetDelay;

					// clientTimes.add(timestamp);
					// serverTimes.add(endTime);
					// delayTimes.add(packetDelay);
					// System.err.println("Server:" + endTime + " Client:"
					// + timestamp + " Result:" + packetDelay);

					receivedPackets++;
				} catch (Exception c) {
					downloading = false;
				}
			}

			// for (int i = 0; i < delayTimes.size(); i++) {
			// Log.e("Server:" + serverTimes.get(i) + " Client:"
			// + clientTimes.get(i) + " Result:" + delayTimes.get(i));
			// }

			// System.out.print("\n");
			// Log.d("end time   " + endTime);
			Log.d("Experiment " + experimentID + " Received: "
					+ receivedPackets + " in " + (endTime - startTime) + "ms");

			// send number back
			Log.d("Waiting for tcp connection from client");
			try {
				tcpSocket = tcpServer.accept();
				tcpOut = new BufferedOutputStream(tcpSocket.getOutputStream());
				tcpIn = new BufferedInputStream(tcpSocket.getInputStream());
				Log.d("Connected");
			} catch (IOException e) {
				Log.e("Error establishing tcp connection to client");
				continue;
			}

			// int delay = (int) (delaySum / receivedPackets); // milliseconds
			int delay = (int) lowestDelay;
			int time = (int) (endTime - startTime); // miliseconds
			time = time == 0 ? 1 : time;
			Log.d("bandwith receivedPackets:" + receivedPackets
					+ " * packetSize:" + fullPacketSize + " / time:" + time);
			int bandwith = (receivedPackets * fullPacketSize / time); // Kbits/seconds
			double lossRate = 100 * (numberProbes - receivedPackets)
					/ numberProbes;
			int intLossRate = (int) lossRate;

			Log.d("Sending [delay:" + delay + "ms, bandwith:" + bandwith
					+ "Kbits/s, lossRate:" + lossRate + "%] to client");
			// Log.d("Expected bandwith without loss, same time: "
			// + (numberProbes * fullPacketSize / time) + "Kbit/s");
			byteBuilder = ByteBuffer.allocate(measurementSize);

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
			byteBuilder.put((byte) (intLossRate & 0xff));
			byteBuilder.put((byte) ((intLossRate >> 8) & 0xff));
			byteBuilder.put((byte) ((intLossRate >> 16) & 0xff));
			byteBuilder.put((byte) ((intLossRate >> 24) & 0xff));
			// experiment ID
			byteBuilder.put((byte) (experimentID & 0xff));
			byteBuilder.put((byte) ((experimentID >> 8) & 0xff));
			byteBuilder.put((byte) ((experimentID >> 16) & 0xff));
			byteBuilder.put((byte) ((experimentID >> 24) & 0xff));

			try {
				tcpOut.write(byteBuilder.array(), 0, measurementSize);
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

			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}

			// numberProbes = 20;
			// udpPacketSize = 12 + 500;

			Log.d("Sending " + numberProbes + " udp packets to client");
			// long asdf = System.currentTimeMillis();
			byteBuilder = ByteBuffer.allocate(udpPacketSize);
			for (int i = 0; i < numberProbes; i++) {
				try {
					// id
					byteBuilder.put(0, (byte) (i & 0xff));
					byteBuilder.put(1, (byte) ((i >> 8) & 0xff));
					byteBuilder.put(2, (byte) ((i >> 16) & 0xff));
					byteBuilder.put(3, (byte) ((i >> 24) & 0xff));
					// experiment ID
					byteBuilder.put(12, (byte) (experimentID & 0xff));
					byteBuilder.put(13, (byte) ((experimentID >> 8) & 0xff));
					byteBuilder.put(14, (byte) ((experimentID >> 16) & 0xff));
					byteBuilder.put(15, (byte) ((experimentID >> 24) & 0xff));
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

					udpSocket.send(new DatagramPacket(byteBuilder.array(),
							udpPacketSize, IPAddress, udpPort));
				} catch (Exception e) {
					Log.e("Error sending udp packet to client");
					continue;
				}
			}
			// System.err.println("Time: " + (System.currentTimeMillis() -
			// asdf));
			Log.d("Done");
			Log.d("Total time " + (System.currentTimeMillis() - startTime)
					+ "ms.");
		}

		try {
			tcpServer.close();
		} catch (IOException e) {
		}
		udpSocket.close();
	}
}