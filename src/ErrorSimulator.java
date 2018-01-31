
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * @author BLoo
 *
 */
public class ErrorSimulator {
	private DatagramSocket eSimSocket;
	private DatagramSocket sendSocket;

	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;

	private int eSimPort, serverPort;

	/**
	 * Base constructor for Host
	 * 
	 * @param clientPort
	 * @param serverPort
	 */
	ErrorSimulator(int clientPort, int serverPort) {
		this.eSimPort = clientPort;
		this.serverPort = serverPort;

		try {
			eSimSocket = new DatagramSocket(this.eSimPort);
			sendSocket = new DatagramSocket();

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
		this(23, 69);
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
		byte[] msg;

		receivePacket = new DatagramPacket(new byte[100], 100);

		try {

			socket.receive(receivePacket);

			msg = receivePacket.getData();

			System.out.println("Host Received: " + receivePacket.getPort());
			System.out.println(new String(msg));
			System.out.println(Arrays.toString(msg) + "\n");

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
			System.out.println("Host Sending: " + sendPacket.getPort());
			System.out.println(new String(sendPacket.getData()));
			System.out.println(Arrays.toString(sendPacket.getData()) + "\n");

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
	 * @param packet - to be sent to server
	 * @return packet received from server  
	 */
	private DatagramPacket passOn(DatagramPacket packet)
	{
		try {
			send(Arrays.copyOf(packet.getData(), packet.getLength()), getHostSocket(), InetAddress.getLocalHost(), getServerPort());
		} catch (UnknownHostException e) {
			eSimSocket.close();
			sendSocket.close();
			e.printStackTrace();
			System.exit(1);
		}
		//send message, excluding the trailing zeroes
		return receive(getHostSocket());
	}
	
	/**
	 * Waits for packets and passes them onto their recipient 
	 * 
	 */
	void startPassthrough()
	{
		DatagramPacket initialPacket, responsePacket;
		while (true) {
			initialPacket = receive(getClientReceiveSocket()); //wait to receive packet from client
			responsePacket = passOn(initialPacket);
			send(Arrays.copyOf(responsePacket.getData(), responsePacket.getLength()), getHostSocket(), initialPacket.getSocketAddress()); //send reply from the server to the server to the client
		}
	}

	/**
	 * Control loop that mediates connection between client and server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ErrorSimulator h = new ErrorSimulator(23, 69);
		h.startPassthrough();
	}
}
