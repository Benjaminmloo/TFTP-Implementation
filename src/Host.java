
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
public class Host {
	private DatagramSocket clientReceiveSocket;
	private DatagramSocket hostSocket;

	private DatagramPacket sendPacket;
	private DatagramPacket receivePacket;

	private int clientPort, serverPort;

	byte read[], write[];

	/**
	 * Base constructor for Host
	 * 
	 * @param clientPort
	 * @param serverPort
	 */
	Host(int clientPort, int serverPort) {
		this.clientPort = clientPort;
		this.serverPort = serverPort;

		try {
			clientReceiveSocket = new DatagramSocket(this.clientPort);
			hostSocket = new DatagramSocket();

		} catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Default constructor setting the client and servers ports to their defaults
	 */
	Host() {
		this(23, 69);
	}

	/**
	 * @return clientReceiveSocket
	 */
	private DatagramSocket getClientReceiveSocket() {
		return clientReceiveSocket;
	}

	/**
	 * @return hostSocket
	 */
	private DatagramSocket getHostSocket() {
		return hostSocket;
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
		return clientPort;
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
	void send(byte[] msg, DatagramSocket socket, SocketAddress returnAddress) {
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
	void send(byte[] msg, DatagramSocket socket, InetAddress address, int port) {
		sendPacket = new DatagramPacket(msg, msg.length, address, port);
		send(socket, sendPacket);
	}

	/**
	 * Base send method, used to send sendPacket over given socket
	 * 
	 * @param socket
	 * @param sendPacket
	 */
	void send(DatagramSocket socket, DatagramPacket sendPacket) {
		try {
			System.out.println("Host Sending: " + sendPacket.getPort());
			System.out.println(new String(sendPacket.getData()));
			System.out.println(Arrays.toString(sendPacket.getData()) + "\n");

			socket.send(sendPacket);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Control loop that mediates connection between client and server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Host h = new Host(23, 69);
		DatagramPacket receivedPacket;
		SocketAddress returnAddress;
		while (true) {
			try {
				receivedPacket = h.receive(h.getClientReceiveSocket());
				returnAddress = receivedPacket.getSocketAddress(); // save return address
																	//the address the client just sent from

				h.send(Arrays.copyOf(receivedPacket.getData(), receivedPacket.getLength()), h.getHostSocket(),  //send message, excluding the trailing zeroes
						InetAddress.getLocalHost(), h.getServerPort());
				receivedPacket = h.receive(h.getHostSocket());
				h.send(Arrays.copyOf(receivedPacket.getData(), receivedPacket.getLength()), h.getHostSocket(), //send reply from the server to the server to the client
						returnAddress);
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
	}
}
