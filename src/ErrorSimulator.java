
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
		DatagramPacket clientPacket, serverPacket = new DatagramPacket(createAck(0), 4);
		while (true) {
			clientPacket = receive(mediatorSocket); // wait to receive packet from client
			clientAddress = clientPacket.getSocketAddress();
			send(clientPacket.getData(), mediatorSocket, serverAddress);
			
			if(getType(serverPacket) == 3 && getDataLength(serverPacket) < 512)break;
			
			serverPacket = receive(mediatorSocket);
			serverAddress = serverPacket.getSocketAddress();
			send(serverPacket.getData(), mediatorSocket, clientAddress);

			if (getType(clientPacket) == 3 && getDataLength(clientPacket) < 512)break;
		}
	}

	/**
	 * @author bloo Control loop that mediates connection between client and server
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		ErrorSimulator h = new ErrorSimulator(23, 69, true);
		h.startPassthrough();
	}

}
