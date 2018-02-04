
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * @author Benjamin Loo
 *
 */
public class ErrorSimulator {
	private DatagramSocket eSimSocket;
	private DatagramSocket sendSocket;

	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;

	private int eSimPort, serverPort;
	private boolean verbose;

	/**
	 * Base constructor for Host
	 * 
	 * @param clientPort
	 * @param serverPort
	 */
	ErrorSimulator(int clientPort, int serverPort,  boolean verbose) {
		this.eSimPort = clientPort;
		this.serverPort = serverPort;
		this.verbose = verbose;
		try {
			eSimSocket = new DatagramSocket(this.eSimPort);
			sendSocket = new DatagramSocket();
			startPassthrough();

		} catch (SocketException e) {
			eSimSocket.close();
			sendSocket.close();

			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Default constructor setting the client and servers ports to their defaults
	 */
	ErrorSimulator() {
		this(23, 69, true);
	}

	/**
	 * @return clientReceiveSocket
	 */
	private DatagramSocket getClientReceiveSocket() {
		return eSimSocket;
	}

	/**
	 * @return hostSocket
	 */
	private DatagramSocket getHostSocket() {
		return sendSocket;
	}

	/**
	 * @return serverPort
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * @return clientPort
	 */
	public int getClientPort() {
		return eSimPort;
	}

	/**
	 * @param socket
	 * @return packet data received from socket, if there are any exceptions
	 *         receiving, a size zero byte array
	 */
	DatagramPacket receive(DatagramSocket socket) {
		receivePacket = new DatagramPacket(new byte[100], 100);
		try {

			socket.receive(receivePacket);

			if (verbose) {
				System.out.println("Host Received: " + receivePacket.getPort());
				System.out.println(packetToString(receivePacket));
			}

			return receivePacket;
		} catch (IOException e) {
			eSimSocket.close();
			sendSocket.close();
			e.printStackTrace();
			System.exit(1);
		}
		return new DatagramPacket(new byte[0], 0);
	}

	/**
	 * creates a packet to be sent by base send() method
	 * 
	 * @param msg
	 * @param socket
	 * @param returnAddress
	 */
	private void send(byte[] msg, DatagramSocket socket, SocketAddress returnAddress) {
		sendPacket = new DatagramPacket(msg, msg.length, returnAddress);
		send(socket, sendPacket);
	}

	/**
	 * creates a packet to be sent by base send() method
	 * 
	 * @param msg
	 * @param socket
	 * @param address
	 * @param port
	 */
	private void send(byte[] msg, DatagramSocket socket, InetAddress address, int port) {
		sendPacket = new DatagramPacket(msg, msg.length, address, port);
		send(socket, sendPacket);
	}

	/**
	 * Base send method, used to send sendPacket over given socket
	 * 
	 * @param socket
	 * @param sendPacket
	 */
	private void send(DatagramSocket socket, DatagramPacket sendPacket) {
		try {
			if (verbose) {
				System.out.println("Host Sending: " + sendPacket.getPort());
				System.out.println(packetToString(sendPacket));
			}

			socket.send(sendPacket);

		} catch (IOException e) {
			eSimSocket.close();
			sendSocket.close();
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Passes packet to server and handles the response
	 * 
	 * @param packet
	 *            - to be sent to server
	 * @return packet received from server
	 */
	private DatagramPacket passOn(DatagramPacket packet) {
		try {
			send(Arrays.copyOf(packet.getData(), packet.getLength()), getHostSocket(), InetAddress.getLocalHost(),
					getServerPort());
		} catch (UnknownHostException e) {
			eSimSocket.close();
			sendSocket.close();
			e.printStackTrace();
			System.exit(1);
		}
		// send message, excluding the trailing zeroes
		return receive(getHostSocket());
	}

	/**
	 * Waits for packets and passes them onto their recipient
	 * 
	 */
	void startPassthrough() {
		DatagramPacket initialPacket, responsePacket;
		while (true) {
			initialPacket = receive(getClientReceiveSocket()); // wait to receive packet from client
			responsePacket = passOn(initialPacket);
			send(Arrays.copyOf(responsePacket.getData(), responsePacket.getLength()), getHostSocket(),
					initialPacket.getSocketAddress()); // send reply from the server to the server to the client
		}
	}

	/**
	 * Reads bytes from a byte array at the start index into a string
	 * 
	 * @param index statring index of the data
	 * @param packet byte array of packet data
	 * @param dataLength the number of bytes of data
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
	private String packetToString(DatagramPacket packet) {
		String descriptor = "";
		String msg;
		byte[] data = packet.getData();
		int index = 2;
		int blockNum;
		if (data.length > 0) {
			if (data[0] == 0) {
				descriptor += "Type: ";
				if (data[1] == 1) {
					descriptor += "RRQ\nFile name: ";

					msg = readBytes(index, data, packet.getLength()); // read filename from the packets
					descriptor += msg + "\nMode: "; // add the name to the packet descriptor
					index += msg.length() + 1; // update the index

					msg = readBytes(index, data, packet.getLength());
					descriptor += msg + "\n";
					index += msg.length() + 1;

				} else if (data[1] == 2) {
					descriptor += "WRQ\nFile name: ";
					msg = readBytes(index, data, packet.getLength());
					descriptor += msg + "\nMode: ";
					index += msg.length() + 1;

					msg = readBytes(index, data, packet.getLength());
					descriptor += msg + "\n";
					index += msg.length() + 1;
				} else if (data[1] == 3) {
					descriptor += "DATA\nBlock #: ";
					blockNum = data[2] * 256 + data[1]; // convert 2 byte number to decimal
					descriptor += blockNum + "\nBytes of data: " + readBytes(4, data, packet.getLength()).length()
							+ "\n"; // add block number to
					// descriptor as
					// well as read the
					// number of bytes
					// in data
				} else if (data[1] == 4) {
					descriptor += "ACK\nBlock #: ";
					blockNum = data[2] * 256 + data[1];
					descriptor += blockNum + "\n";
				} else if (data[1] == 5) {
					descriptor += "ERROR\nError Code";
					blockNum = data[2] * 256 + data[1];
					descriptor += blockNum + "ErrMsg: " + readBytes(4, data, packet.getLength()) + "\n";
				}
			}

			return descriptor;
		}

		return null;
	}

	/**
	 * Control loop that mediates connection between client and server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ErrorSimulator h = new ErrorSimulator(23, 69, true);
	}

}
