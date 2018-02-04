import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author BenjaminP, BenB
 *
 */
public class Server extends UDPConnection{
	/*
	 * File transferred in 512-byte blocks, 1 block per packet transfer. Block < 512
	 * bytes terminates transfer Packet types: RRQ, WRQ, DATA, ACK, ERROR
	 * TID(transfer ID) Client gets random TID when a request is prepared, when a
	 * server grants a request it also gets a random TID Source and destination TID
	 * associated with every packet but not stored in the packet, used asDel
	 * source/destination ports for UDP Write: Client WRQ -> Server ACK Block 0 ->
	 * Client Data Block 1 -> Server ACK Block 1 -> Client Data Block 2 -> etc...
	 * Server ACK Block n Read: Client RRQ -> Server Data Block 1 -> Client ACK
	 * Block 1 -> Server Data Block 2 -> etc... Client ACK Block n RRQ acknowledged
	 * with DATA, WRQ by ACK
	 */
	private DatagramSocket requestSocket;

	private DatagramPacket sendPacket, receivePacket;

	private boolean verbose;

	private static Map<Byte, String> RequestTypes;
	static {
		RequestTypes = new HashMap<>();
		// RequestTypes.put( (byte) 0, "null"); Perhaps use as a shutdown request?
		RequestTypes.put((byte) 1, "RRQ");
		RequestTypes.put((byte) 2, "WRQ");
		RequestTypes.put((byte) 3, "DATA");
		RequestTypes.put((byte) 4, "ACK");
		RequestTypes.put((byte) 5, "ERROR");
	}

	/**
	 * Constructor for a Server
	 * 
	 * @param serverPort
	 */
	Server(int serverPort, boolean verbose) {
		this.verbose = verbose;
		try {
			requestSocket = new DatagramSocket(69);
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	Server(int serverPort) {
		this(serverPort, true);
	}

	
	private class ClientConnection extends Thread {
		SocketAddress returnAddress;

		ClientConnection(DatagramPacket requestPacket) {
			returnAddress = requestPacket.getSocketAddress(); // set the return address of the packet
		}

		@Override
		public void run() {// TODO parse request then transfer files.
			while (true) {

			}
		}
	}


	/**
	 * methods the manages packet being received
	 * 
	 * sends any received packets to be processed by another method
	 * 
	 * @param threaded
	 *            flag to determine whether or not to use threads to wait for
	 *            request
	 */
	
	void waitForRequest() {
		DatagramPacket receivedPacket;
		while (true) {
			try {
				receivedPacket = receive(requestSocket); // wait for new request packet
				new ClientConnection(receivedPacket).start(); // start new client connection for the recently acquired
																// request
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}

	/**
	 * 
	 * Pulls request type from packet and returns
	 * 
	 * @param packet
	 * @return Returns request type
	 * @throws IllegalArgumentException
	 *             When data is not in proper format or request is not a recognized
	 *             type
	 */
	private byte getRequest(DatagramPacket packet) throws IllegalArgumentException {
		byte data[] = packet.getData();

		if (data[0] == 0 && data[data.length - 1] == 0) {
			byte request = data[1];
			if (RequestTypes.containsKey(request)) {
				return request;
			} else
				throw new IllegalArgumentException();
		} else
			throw new IllegalArgumentException();
	}

	/**
	 * @author BenjaminP Handles packet requests
	 * 
	 * @param packet
	 */
	private void requestHandler(DatagramPacket packet) {
		byte request = this.getRequest(packet);
		switch (request) {

		/* Read Request */
		case 1:
			// Respond with Data block 1 and 0 bytes of data
			byte data[] = { 0, 3, 0, 1 };
			this.send(data, packet.getSocketAddress());
			break;

		/* Write Request */
		case 2:
			// Respond with ACK block 0
			byte data1[] = { 0, 4, 0, 0 };
			this.send(data1, packet.getSocketAddress());
			break;

		/* Data */
		case 3:
			System.out.println("Received out of time DATA packet");
			break;

		/* Acknowledge */
		case 4:
			System.out.println("Received out of time ACK packet");
			break;

		/* Error */
		case 5:
			/* Currently does nothing */
			break;
		}

	}

	/**
	 * @author BenjaminP
	 * @param packet
	 */
	private void readRequestHandler(DatagramPacket packet) throws IllegalArgumentException {
		List<byte[]> data = this.parseRRQ(this.getFileName(packet));
		DatagramSocket rrqSocket = null;
		try {
			rrqSocket = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		for (int i = 0; i < data.size(); i++) {
			byte sendData[] = data.get(i);
			this.send(sendData, rrqSocket, packet.getSocketAddress());

			DatagramPacket ackPacket = this.receive(rrqSocket);
			if (this.getRequest(ackPacket) != (byte) 4) {
				System.out.println("Not an ACK packet!");
				throw new IllegalArgumentException();
			}

			if (processACK(ackPacket.getData()) != i) {
				System.out.println("ACK for a different Block!");
				throw new IllegalArgumentException();
			}
		}
		/*
		 * List of byte arrays of max size 512 = Call parser here
		 * 
		 */

		/*
		 * LOOP this.send( 1st array, packet.getSocketAddress); Wait for ACK...
		 * this.send( 2nd array, packet.getSocketAddress); Wait for ACK etc...
		 */
	}

	private int processACK(byte[] data) {
		return ((data[2] * 10) + data[3]);
	}

	/**
	 * @author BenjaminP Parses given file to be sent through data packets
	 * 
	 * @param file
	 * @return
	 */
	private List<byte[]> parseRRQ(String file) {
		String data = "";
		try {
			FileReader fileReader = new FileReader(file);

			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				data = data.concat(" " + line);
			}

			bufferedReader.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		List<byte[]> outList = new ArrayList<byte[]>();
		byte byteData[] = data.getBytes();
		for (int i = 0; i < byteData.length; i += 512) {
			byte temp[] = null;
			if (i + 512 <= byteData.length) {
				temp = Arrays.copyOfRange(byteData, i, i + 512);
				outList.add(temp);
			} else {
				temp = Arrays.copyOfRange(byteData, i, byteData.length);
				outList.add(temp);
			}
		}

		return outList;
	}

	/**
	 * 
	 * @param packet
	 */
	private void writeRequestHandler(DatagramPacket packet) {
		int blockNum = 0;

		SocketAddress returnAddress = packet.getSocketAddress();
		DatagramSocket wrqSocket = null;
		DatagramPacket newPacket;
		OutputStream file = null;
		try {
			file = new FileOutputStream(getFileName(packet));
			wrqSocket = new DatagramSocket();
			send(createAck(blockNum), wrqSocket, returnAddress);
			do {
				newPacket = receive(wrqSocket, 516);
				file.write(newPacket.getData(), 4, newPacket.getLength()); // write the data section of the packet to
																			// the file
				send(createAck(++blockNum), wrqSocket, returnAddress); // send ack to the client
			} while (newPacket.getLength() == 516); // continue while the packets are full
			file.close(); // close the file when done
		} catch (FileNotFoundException e) { //catch any exceptions closing any necessary connections
			e.printStackTrace();
			System.exit(1);
		} catch (SocketException e) {
			if (wrqSocket != null) {
				wrqSocket.close();
			}
			try {
				file.close();
			} catch (IOException x) {
				x.printStackTrace();
			}
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			if (wrqSocket != null) {
				wrqSocket.close();
			}
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * creates acknoledge packet based on given block number
	 * 
	 * @param blockNum
	 *            - the current number the packet is acknowledging
	 * @return byte array with acknowledge data
	 */
	private byte[] createAck(int blockNum) {
		byte[] ack = new byte[] { 0, 4, (byte) (blockNum / 256), (byte) (blockNum % 256) };
		return ack;
	}

	/**
	 * Reads bytes from a byte array at the start index into a string
	 * 
	 * @param index
	 *            statring index of the data
	 * @param packet
	 *            byte array of packet data
	 * @param dataLength
	 *            the number of bytes of data
	 * @return resulting String of data
	 */
	private String readBytes(int offset, byte[] packet, int dataLength) {
		String data = "";
		int index = offset;
		while (index < dataLength - offset && packet[index] != 0) {
			data += (char) packet[index++];
		}
		return data;
	}

	/**
	 * Parses a tftp packet in byte form and returns info
	 * 
	 * @param packet
	 * @return contents of a packet
	 */
	private String getFileName(DatagramPacket packet) {
		return readBytes(2, packet.getData(), packet.getLength());
	}

	/**
	 * Start the sever waiting for request
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Server s = new Server(69);
		s.waitForRequest();
	}
}