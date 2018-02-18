import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * @author BenjaminP, BenB
 *
 */
public class Server extends TFTPConnection {
	
	
	//	Class Variable definition start
	private WaitForRequest waitThread;
	boolean cont = true;

	/**
	 * Constructor for a Server
	 * 
	 * @param serverPort
	 *            - port for server to receive requests from
	 * @param verbose
	 *            - whether or not the server will be verbose
	 * @author bloo
	 */
	public Server(int serverPort, boolean verbose) {
		this.verbose = verbose;
		waitThread = new WaitForRequest(waitForSocket(serverPort));
		waitThread.start();
	}

	/**
	 * Constructor for a Server assumes verbosity
	 * 
	 * @param serverPort
	 *            - port for server to receive requests from
	 * @author bloo
	 */
	public Server(int serverPort) {
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
	 * 
	 * @author bloo
	 */

	void userInterface() {
		Scanner n = new Scanner(System.in);
		byte operation;

		while (cont) {
			while (true) { // get transfer type
				try {
					this.outputWindow.append("settings(1), quit(2): ");
					System.out.print("settings(1), quit(2): ");
					operation = n.nextByte();
					break;
				} catch (InputMismatchException e) {
					System.out.println("Invalid input!");
					n.next();
				}

			}

			if (operation == 1) {
				while (true) { // get transfer mode
					try {
						System.out.print("Verbose mode (true/false): ");
						verbose = n.nextBoolean();

						break;
					} catch (InputMismatchException e) {
						System.out.println("Invalid input!");
						n.next();
					}
				}

			} else if (operation == 2) {
				cont = false;
				waitThread.interrupt();
			} else {
				System.out.println("Invalid input! enter 1 or 2");
			}
	// For Testing Purposes
	public int getWaitForRequest() {
		if(waitThread.getDatagramSoc() == SERVER_PORT) {
			return waitThread.getDatagramSoc();
		}
		return 0;
	}
	
	/**
	 * Thread that waits for tftp requests and dipatches Threaded connections to
	 * handle the requests
	 * 
	 * @author BLoo
	 *
	 */
	private class WaitForRequest extends Thread {
		DatagramPacket receivedPacket;
		DatagramSocket requestSocket;

		/**
		 * constructor for WaitForRequest threads
		 * 
		 * @param socket
		 *            - the socket that will be waited on, should already have port
		 *            attached
		 */
		protected WaitForRequest(DatagramSocket socket) {
			requestSocket = socket;
		}

		// Testing Purposes
		public int getDatagramSoc() {
			System.out.println(" JUnit Test: " + requestSocket.getLocalPort());
			return requestSocket.getLocalPort();
		}
		
		@Override
		public void run() {
			System.out.println("waiting");
			while (cont) {
				try {
					receivedPacket = receive(requestSocket); // wait for new request packet
					if (receivedPacket != null)
						new Thread(new ThreadedConnection(receivedPacket, verbose)).start(); // start new client
																								// connection for the
					// recently acquired request
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}

		@Override
		public void interrupt() {
			super.interrupt();
			requestSocket.close();
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
	 * 
	 * @author bloo
	 */
	void userInterface() {
		Scanner n = new Scanner(System.in);
		byte operation;

		while (cont) {
			while (true) {
				try {
					System.out.print("settings(1), quit(2): ");
					operation = n.nextByte();
					break;
				} catch (InputMismatchException e) {
					System.out.println("Invalid input!");
					n.next();
				}

			}

			//Turns on verbose or exits server
			if (operation == 1) 
				while (true) {
					try {
						System.out.print("Verbose mode (true/false): ");
						verbose = n.nextBoolean();
						break;
					} catch (InputMismatchException e) {
						System.out.println("Invalid input!");
						n.next();
					}

			} else if (operation == 2) {
				cont = false;
				waitThread.interrupt();
			} else 
				System.out.println("Invalid input! enter 1 or 2");
		}

		n.close();
	}

	/**
	 * Start the sever waiting for request
	 * 
	 * @param args
	 * @author bloo
	 */
	/*public static void main(String[] args) {
		Server s = new Server(SERVER_PORT);
		s.userInterface();
	}*/
}