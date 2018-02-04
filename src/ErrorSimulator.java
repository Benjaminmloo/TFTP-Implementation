
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
public class ErrorSimulator extends TFTPConnection {
	private DatagramSocket eSimSocket, mediatorSocket;
	private SocketAddress clientAddress, serverAddress;

	private int eSimPort, serverPort = 69;

	/**
	 * @author bloo Base constructor for Host
	 * 
	 * @param clientPort
	 * @param serverPort
	 */
	ErrorSimulator(int clientPort, int serverPort, boolean verbose) {
		this.eSimPort = clientPort;
		this.serverPort = serverPort;
		this.verbose = verbose;
		try {
			eSimSocket = new DatagramSocket(this.eSimPort);
			mediatorSocket = new DatagramSocket();
			startPassthrough();

		} catch (SocketException e) {
			eSimSocket.close();
			mediatorSocket.close();

			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * @author bloo Default constructor setting the client and servers ports to
	 *         their defaults
	 */
	ErrorSimulator() {
		this(23, 69, true);
	}

	/**
	 * @author bloo Waits for packets and passes them onto their recipient
	 * 
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
			meddiateConnection();
		}
	}

	void meddiateConnection() {
		DatagramPacket clientPacket, serverPacket;
		while (true) {
			clientPacket = receive(mediatorSocket); // wait to receive packet from client
			clientAddress = clientPacket.getSocketAddress();
			send(clientPacket.getData(), mediatorSocket, serverAddress);
			
			if (getType(clientPacket) == 3 && clientPacket.getLength() < 516)break;
			
			serverPacket = receive(mediatorSocket);
			serverAddress = serverPacket.getSocketAddress();
			send(serverPacket.getData(), mediatorSocket, clientAddress);
			
			if(getType(serverPacket) == 3 && serverPacket.getLength() < 516)break;
		}
	}

	/**
	 * @author bloo Control loop that mediates connection between client and server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ErrorSimulator h = new ErrorSimulator(23, 69, true);
	}

}
