package tftpConnection;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * @author Benjamin Loo
 *
 */
public class ErrorSimulator extends TFTPConnection {

	// Class Variable definition start
	private DatagramSocket eSimSocket, mediatorSocket;
	private SocketAddress clientAddress, serverAddress;
	private String input;
	private int errorSimMode, errorSimBlock, errorSimDelay, errorSimType;

	// Class Variable definition finish

	private int eSimPort, serverPort = 69;

	/**
	 * Base constructor for Host
	 * 
	 * @param eSimPort
	 *            - the port that the error simulator listen for requests on
	 * @param serverPort
	 *            - the port that the server listens for requests on
	 * @param verbose
	 *            - verbosity of the error simulator
	 * 
	 * @author bloo
	 */
	ErrorSimulator(int eSimPort, int serverPort, boolean verbose) {
		this.eSimPort = eSimPort;
		this.serverPort = serverPort;
		this.verbose = verbose;
		try {
			eSimSocket = new DatagramSocket(this.eSimPort);
			mediatorSocket = new DatagramSocket();

		} catch (SocketException e) {
			// eSimSocket.close();
			// mediatorSocket.close();

			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Default constructor setting the client and servers ports to their defaults
	 * 
	 * @author bloo
	 */
	ErrorSimulator() {
		this(ESIM_PORT, SERVER_PORT, true);
	}

	/**
	 * Mediates connection between client and server once connection has been
	 * initiated. exits when file transfer is complete
	 * 
	 * @author BLoo, Eric
	 */
	void mediateTransfer() {
		DatagramPacket clientPacket, serverPacket = new DatagramPacket(createAck(0), 4);
		while (true) {
			clientPacket = receive(mediatorSocket); // wait to receive packet from client
			clientAddress = clientPacket.getSocketAddress();

			if (errorSimMode == 1) {
				simulateLosePacket(clientPacket, serverAddress);
			} else if (errorSimMode == 2) {
				simulateDelayPacket(clientPacket, serverAddress);
			} else if (errorSimMode == 3) {
				simulateDuplicatePacket(clientPacket, serverAddress);
			} else {
				send(clientPacket.getData(), mediatorSocket, serverAddress);
			}

			if (getType(serverPacket) == 3 && getDataLength(serverPacket) < 512)
				break;

			serverPacket = receive(mediatorSocket);
			serverAddress = serverPacket.getSocketAddress();

			if (errorSimMode == 1) {
				simulateLosePacket(serverPacket, clientAddress);
			} else if (errorSimMode == 2) {
				simulateDelayPacket(serverPacket, clientAddress);
			} else if (errorSimMode == 3) {
				simulateDuplicatePacket(serverPacket, clientAddress);
			} else {
				send(serverPacket.getData(), mediatorSocket, clientAddress);
			}

			if (getType(clientPacket) == 3 && getDataLength(clientPacket) < 512)
				break;
		}
	}

	/**
	 * Waits for packets and passes them onto their recipient
	 * 
	 * @author bloo
	 */
	void startPassthrough() {
		DatagramPacket initialPacket, responsePacket;
		while (true) {
			initialPacket = receive(eSimSocket); // wait to receive packet from client
			clientAddress = initialPacket.getSocketAddress();

			try {
				send(initialPacket.getData(), mediatorSocket, InetAddress.getLocalHost(), serverPort);
			} catch (UnknownHostException e) {
				eSimSocket.close();
				mediatorSocket.close();
				e.printStackTrace();
				System.exit(1);
			}
			responsePacket = receive(mediatorSocket);
			serverAddress = responsePacket.getSocketAddress();
			send(responsePacket.getData(), mediatorSocket, clientAddress);
			mediateTransfer();
		}
	}

	/**
	 * Simulates the loss of a packet
	 * 
	 * @param packet
	 *            - Datagram packet to be lost
	 * 
	 * @param address
	 *            - data packet address
	 * 
	 * @author Eric
	 */
	public void simulateLosePacket(DatagramPacket packet, SocketAddress address) {

		if (getBlockNum(packet) == errorSimBlock && getType(packet) == errorSimType) {

			print("THIS PACKET WILL BE LOST\n");

			// packet is not sent
		}

		else {
			send(packet.getData(), mediatorSocket, address);
		}

	}

	/**
	 * Simulates the delay of a packet
	 * 
	 * @param packet
	 *            - Datagram packet to be lost
	 * 
	 * @param address
	 *            - data packet address
	 * 
	 * @author Eric
	 */
	public void simulateDelayPacket(DatagramPacket packet, SocketAddress address) {

		if (getBlockNum(packet) == errorSimBlock && getType(packet) == errorSimType) {

			print("THIS PACKET WILL BE DELAYED\n");

			// delay the packet
			try {
				Thread.sleep(errorSimDelay);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		send(packet.getData(), mediatorSocket, address);
	}

	/**
	 * Simulates the duplication of a packet
	 * 
	 * @param packet
	 *            - Datagram packet to be lost
	 * 
	 * @param address
	 *            - data packet address
	 * 
	 * @author Eric
	 */
	public void simulateDuplicatePacket(DatagramPacket packet, SocketAddress address) {

		if (getBlockNum(packet) == errorSimBlock && getType(packet) == errorSimType) {

			print("THIS PACKET WILL BE DUPLICATED\n");

			send(packet.getData(), mediatorSocket, address);

			// delay the packet
			try {
				Thread.sleep(errorSimDelay);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			send(packet.getData(), mediatorSocket, address);

		} else {
			send(packet.getData(), mediatorSocket, address);
		}

	}

	/**
	 * Sets simulation parameters required to simulate errors.
	 * 
	 * @param mode
	 *            - which simulation mode (lose, delay, duplicate)
	 * 
	 * @param block
	 *            - which block number to (lose, delay, duplicate)
	 * 
	 * @param delay
	 *            - how long to delay packet transfer, or delay between duplicates
	 *            sent
	 * 
	 * @param type
	 *            - which type of packet to error simulate, ie. 4th ACK or 4th DATA?
	 * 
	 * @author Eric
	 */
	public void setParameters(int mode, int block, int delay, int type) {
		errorSimMode = mode;
		errorSimBlock = block;
		errorSimDelay = delay;
		errorSimType = type;
	}

	@Override
	public void takeInput(String s) {
		input += s;
	}

	/**
	 * Control loop that mediates connection between client and server
	 * 
	 * @param args
	 * @author Bloo
	 */
	/*
	 * public static void main(String[] args) { ErrorSimulator h = new
	 * ErrorSimulator(23, 69, true); h.startPassthrough(); }
	 */

}
