
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

	//	Class Variable definition start
	private DatagramSocket eSimSocket, mediatorSocket;
	private SocketAddress clientAddress, serverAddress;
	private String input;
	//	Class Variable definition finish
	
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
			//eSimSocket.close();
			//mediatorSocket.close();

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
	 * @author BLoo
	 */
	void mediateTransfer() {
		DatagramPacket clientPacket, serverPacket = new DatagramPacket(createAck(0), 4);
		while (true) {
			clientPacket = receive(mediatorSocket); // wait to receive packet from client
			clientAddress = clientPacket.getSocketAddress();
			send(clientPacket.getData(), mediatorSocket, serverAddress);

			if (getType(serverPacket) == 3 && getDataLength(serverPacket) < 512)
				break;

			serverPacket = receive(mediatorSocket);
			serverAddress = serverPacket.getSocketAddress();
			send(serverPacket.getData(), mediatorSocket, clientAddress);

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
	/*public static void main(String[] args) {
		ErrorSimulator h = new ErrorSimulator(23, 69, true);
		h.startPassthrough();
	}*/

}
