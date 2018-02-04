import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;


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

	private boolean verbose;

	

	/**
	 * Constructor for a Server
	 * 
	 * @param serverPort
	 */
	Server(int serverPort, boolean v) {
		this.verbose = v;
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
				new Thread(new ThreadedConnection(receivedPacket)).start(); // start new client connection for the recently acquired
																// request
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
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