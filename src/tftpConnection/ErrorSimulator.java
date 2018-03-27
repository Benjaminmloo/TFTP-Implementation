package tftpConnection;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @author Benjamin Loo
 *
 */
public class ErrorSimulator extends TFTPConnection {

    // Class Variable definition start
    private DatagramSocket eSimSocket, mediatorSocket, errorSocket;
    private SocketAddress clientAddress, serverAddress;
    // private String input;
    private int errorSimMode, errorSimBlock, errorSimDelay, errorSimType;
    private int newOpCode, newNum;
    private String newField;
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
	clearErrorSim();

	try {
	    eSimSocket = new DatagramSocket(this.eSimPort);
	    mediatorSocket = new DatagramSocket();
	    errorSocket = new DatagramSocket();
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
     * @throws UnknownHostException
     */
    void mediateTransfer() throws UnknownHostException {
	DatagramPacket receivePacket = null, lastPacket = null;
	SocketAddress receiveAddress;
	while (true) {
	    try {
		lastPacket = receivePacket;
		receivePacket = receive(mediatorSocket);
		if (serverAddress.equals(receivePacket.getSocketAddress()))
		    receiveAddress = clientAddress;
		else if (clientAddress.equals(receivePacket.getSocketAddress()))
		    receiveAddress = serverAddress;
		else {
		    print(receivePacket.getSocketAddress() + "");
		    continue;
		}

		if (errorSimMode > 0 && (TFTPPacket.getBlockNum(receivePacket) == errorSimBlock
			&& TFTPPacket.getType(receivePacket) == errorSimType)) {
		    if (errorSimMode == 1) {
			simulateLosePacket(receivePacket, receiveAddress, false);
		    } else if (errorSimMode == 2) {
			simulateDelayPacket(receivePacket, receiveAddress, false);
		    } else if (errorSimMode == 3) {
			simulateDuplicatePacket(receivePacket, receiveAddress, false);
		    } else if (errorSimMode == 4) {
			simulateInvalidFormat(receivePacket, receiveAddress, false);
		    } else if (errorSimMode == 5) {
			simulateUnknownTID(receivePacket, receiveAddress, false);
		    }
		} else {
		    send(Arrays.copyOf(receivePacket.getData(), receivePacket.getLength()), mediatorSocket,
			    receiveAddress);
		}

		if (TFTPPacket.getType(receivePacket) == TFTPPacket.OP_ERROR
			|| lastPacket != null && TFTPPacket.getType(lastPacket) == TFTPPacket.OP_DATA
				&& TFTPPacket.getDataLength(lastPacket) < MAX_DATA_SIZE)
		    break;

	    } catch (IllegalArgumentException e) {
		e.printStackTrace();
	    } catch (SocketTimeoutException e) {
		e.printStackTrace();
		System.exit(1);
	    } // wait to receive packet from client
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
	    try {
		initialPacket = receive(eSimSocket);

		clientAddress = initialPacket.getSocketAddress();
		try {
		    if (errorSimMode > 0 && TFTPPacket.getType(initialPacket) == errorSimType) {

			if (errorSimMode == 1) {
			    simulateLosePacket(initialPacket, serverAddress, true);
			} else if (errorSimMode == 2) {
			    simulateDelayPacket(initialPacket, serverAddress, true);
			} else if (errorSimMode == 3) {
			    simulateDuplicatePacket(initialPacket, serverAddress, true);
			} else if (errorSimMode == 4) {
			    simulateInvalidFormat(initialPacket, serverAddress, true);
			} else if (errorSimMode == 5) {
			    simulateUnknownTID(initialPacket, serverAddress, true);
			}
		    } else {
			send(Arrays.copyOf(initialPacket.getData(), initialPacket.getLength()), mediatorSocket,
				InetAddress.getLocalHost(), serverPort);
		    }

		} catch (UnknownHostException e) {
		    e.printStackTrace();
		    System.exit(1);
		}
		responsePacket = receive(mediatorSocket);
		serverAddress = responsePacket.getSocketAddress();

		if (errorSimMode > 0 && TFTPPacket.getType(responsePacket) == errorSimType
			&& TFTPPacket.getBlockNum(responsePacket) == errorSimBlock) {
		    if (errorSimMode == 1) {
			simulateLosePacket(responsePacket, clientAddress, false);
		    } else if (errorSimMode == 2) {
			simulateDelayPacket(responsePacket, clientAddress, false);
		    } else if (errorSimMode == 3) {
			simulateDuplicatePacket(responsePacket, clientAddress, false);
		    } else if (errorSimMode == 4) {
			simulateInvalidFormat(responsePacket, clientAddress, false);
		    } else if (errorSimMode == 5) {
			simulateUnknownTID(responsePacket, clientAddress, false);
		    }
		} else {
		    send(Arrays.copyOf(responsePacket.getData(), responsePacket.getLength()), mediatorSocket,
			    clientAddress);
		}

		if (TFTPPacket.getType(responsePacket) == TFTPPacket.OP_ERROR)
		    continue;

		mediateTransfer();

	    } catch (SocketTimeoutException | UnknownHostException e1) {
		e1.printStackTrace();
		System.exit(1);
	    }
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
     * @throws UnknownHostException
     */
    public void simulateLosePacket(DatagramPacket packet, SocketAddress address, boolean firstPass)
	    throws UnknownHostException {

	print("THIS PACKET WILL BE LOST\n");
	clearErrorSim();

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
     * @throws UnknownHostException
     */
    public void simulateDelayPacket(DatagramPacket packet, SocketAddress address, boolean firstPass)
	    throws UnknownHostException {

	print("THIS PACKET WILL BE DELAYED\n");

	// delay the packet
	try {
	    Thread.sleep(errorSimDelay);
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	if (firstPass)
	    send(Arrays.copyOf(packet.getData(), packet.getLength()), mediatorSocket, InetAddress.getLocalHost(),
		    serverPort);
	else
	    send(Arrays.copyOf(packet.getData(), packet.getLength()), mediatorSocket, address);
	clearErrorSim();
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
     * @throws UnknownHostException
     */
    public void simulateDuplicatePacket(DatagramPacket packet, SocketAddress address, boolean firstPass)
	    throws UnknownHostException {

	print("THIS PACKET WILL BE DUPLICATED\n");

	if (firstPass)
	    send(Arrays.copyOf(packet.getData(), packet.getLength()), mediatorSocket, InetAddress.getLocalHost(),
		    serverPort);
	else
	    send(Arrays.copyOf(packet.getData(), packet.getLength()), mediatorSocket, address);

	// delay the packet
	try {
	    Thread.sleep(errorSimDelay);
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	if (firstPass)
	    send(Arrays.copyOf(packet.getData(), packet.getLength()), mediatorSocket, InetAddress.getLocalHost(),
		    serverPort);
	else
	    send(Arrays.copyOf(packet.getData(), packet.getLength()), mediatorSocket, address);
	clearErrorSim();

    }

    public void simulateInvalidFormat(DatagramPacket packet, SocketAddress address, boolean firstPass)
	    throws UnknownHostException {

	print("THIS PACKET WILL BE \n INCORRECTLY FORMATED\n");
	byte[] data = new byte[1000];
	ByteBuffer buf = ByteBuffer.wrap(data);

	if (newOpCode >= 0) {
	    buf.put(new byte[] { (byte) (newOpCode / 256), (byte) (newOpCode % 256) });
	}

	if (newNum >= 0) {
	    buf.put(new byte[] { (byte) (newNum / 256), (byte) (newNum % 256) });
	}

	if (newField != null) {
	    buf.put(newField.getBytes());
	}

	data = Arrays.copyOf(data, buf.position() - 1);

	if (firstPass)
	    send(Arrays.copyOf(packet.getData(), packet.getLength()), mediatorSocket, InetAddress.getLocalHost(),
		    serverPort);
	else
	    send(Arrays.copyOf(packet.getData(), packet.getLength()), mediatorSocket, address);
	clearErrorSim();
    }

    /**
     * Simulates unkown TID on a specific packet. ( sends the packet to the wrong
     * address )
     * 
     * @param packet
     *            - Datagram packet
     * 
     * @param address
     *            - data packet address
     * 
     * @author Eric
     * @throws UnknownHostException
     */
    public void simulateUnknownTID(DatagramPacket packet, SocketAddress address, boolean firstPass)
	    throws UnknownHostException {

	print("SIMULATING UNKNOWN TID\n");

	if (firstPass)
	    send(Arrays.copyOf(packet.getData(), packet.getLength()), errorSocket, InetAddress.getLocalHost(),
		    serverPort);
	else
	    send(Arrays.copyOf(packet.getData(), packet.getLength()), errorSocket, address);

	try {
	    receive(errorSocket);
	} catch (IllegalArgumentException e) {
	    e.printStackTrace();
	} catch (SocketTimeoutException e) {
	    e.printStackTrace();
	}

	clearErrorSim();

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

    public void setParameters(int mode, int block, int delay, int type, int opCode, int num, String field) {
	errorSimMode = mode;
	errorSimBlock = block;
	errorSimDelay = delay;
	errorSimType = type;
	newOpCode = opCode;
	newNum = num;
	newField = field;
    }

    @Override
    public void takeInput(String s) {
	// input += s;
    }

    void clearErrorSim() {
	setParameters(-1, -1, -1, -1, -1, -1, null);
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
