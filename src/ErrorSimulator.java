
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
public class ErrorSimulator extends UDPConnection{
	private DatagramSocket eSimSocket;
	private DatagramSocket sendSocket;

	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;

	private int eSimPort, serverPort;
	private boolean verbose;

	/**
	 * @author bloo
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
	 * @author bloo
	 * Default constructor setting the client and servers ports to their defaults
	 */
	ErrorSimulator() {
		this(23, 69, true);
	}

	/**
	 * @auther bloo
	 * @return clientReceiveSocket
	 */
	private DatagramSocket getClientReceiveSocket() {
		return eSimSocket;
	}

	/**
	 * @author bloo
	 * @return hostSocket
	 */
	private DatagramSocket getHostSocket() {
		return sendSocket;
	}

	/**
	 * @author bloo
	 * @return serverPort
	 */
	public int getServerPort() {
		return serverPort;
	}

	/**
	 * @author bloo
	 * @return clientPort
	 */
	public int getClientPort() {
		return eSimPort;
	}	

	/**
	 * @author bloo
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
	 * @author bloo
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
	 * @author bloo
	 * Control loop that mediates connection between client and server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ErrorSimulator h = new ErrorSimulator(23, 69, true);
	}

}
