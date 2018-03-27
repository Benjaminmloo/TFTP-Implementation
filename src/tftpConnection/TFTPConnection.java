package tftpConnection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.net.InetSocketAddress;

import javax.swing.JScrollBar;
import javax.swing.JTextArea;

/**
 * @author BenjaminP EricM, BenjaminL, AndrewN
 *
 */
public abstract class TFTPConnection {

    // Class Variable definition start
    protected boolean verbose;
    protected JTextArea outputWindow = new JTextArea();
    protected JScrollBar scrollBar = new JScrollBar();

    protected static final byte[] MODE_OCTET = "octet".getBytes();
    protected static final byte[] MODE_NETASCII = "netascii".getBytes();

    protected static final int SERVER_PORT = 69;
    protected static final int ESIM_PORT = 23;

    private static final int MAX_PACKET_SIZE = 516;
    protected static final int MAX_DATA_SIZE = MAX_PACKET_SIZE - 4;
    protected static final byte ZERO_BYTE = 0;

    private static final int TRANSMIT_LIMIT = 5;

    private DatagramPacket lastSentPkt;
    // Class Variable definition end

    /**
     * Requests a usable socket until successful
     * 
     * @return a usable socket
     */
    protected DatagramSocket waitForSocket() {
	while (true) {
	    try {
		return new DatagramSocket();
	    } catch (SocketException e) {
		e.printStackTrace();
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e1) {
		    e1.printStackTrace();
		}
	    }
	}
    }

    /**
     * waits a datagram socket on a specied port
     * 
     * @param port
     *            - the port requested for the socket
     * @return the socket requested
     */
    protected DatagramSocket waitForSocket(int port) {
	while (true) {
	    try {
		InetSocketAddress sockAd = new InetSocketAddress(InetAddress.getLocalHost(), port);
		return new DatagramSocket(sockAd);
	    } catch (SocketException e) {
		e.printStackTrace();
		try {
		    Thread.sleep(1000);
		} catch (InterruptedException e1) {
		    e1.printStackTrace();
		}
	    } catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
    }

    /**
     * Sends a file over network over tftp
     * 
     * @param packet
     *            - the initial packet, containing return address and file name
     * @param socket
     *            - the socket that will be used to send data and receive acks over
     * @throws IOException
     */
    protected void sendFile(DatagramPacket packet, DatagramSocket socket) throws IOException {
	ArrayList<byte[]> data = new ArrayList<byte[]>();
	data = readFile(TFTPPacket.getFileName(packet));
	sendFile(data, packet.getSocketAddress(), socket);
    }

    /**
     * sends a file over tftp
     * 
     * @param data
     *            - array list holding data blocks to be sent
     * @param recipientAddress
     *            - address data is being sent too
     * @param socket
     *            - The socket the data will be sent over
     * @throws IllegalArgumentException
     */
    protected void sendFile(ArrayList<byte[]> data, SocketAddress recipientAddress, DatagramSocket socket)
	    throws IllegalArgumentException {
	DatagramPacket ackPacket;
	for (int i = 1; i <= data.size(); i++) {
	    byte sendData[] = data.get(i - 1);
	    /**
	     * if the packet is sent but not received which is indicated by a time out the
	     * packet is transmitted again the thread waits again for the ack
	     */
	    for (int j = 0; j < TRANSMIT_LIMIT; j++) {
		try {
		    this.send(TFTPPacket.createData(i, sendData), socket, recipientAddress);

		    do {
			ackPacket = receive(socket);
			if (isLast(ackPacket)) {
			    send(lastSentPkt, socket);
			    continue;
			}
		    } while (!isFrom(ackPacket, socket, recipientAddress) || !isNext(ackPacket));

		    if (TFTPPacket.getType(ackPacket) == TFTPPacket.OP_ERROR) {
			System.err.println("\nError Occured\n" + TFTPPacket.packetToString(ackPacket));
			return;
		    }

		    break; // if packet was sent and the apropriate ack was received break out of
			   // retransmit loop
		} catch (SocketTimeoutException e) { // default timeout is 2 seconds
		    if (verbose)
			println("Time out");
		    if (j >= TRANSMIT_LIMIT) {
			print("Connection timed out \n Stopping transfer");
			return;
		    }
		}
	    }
	}
    }

    /**
     * Sends msg to return address over the given socket
     * 
     * @param msg
     *            - byte array to be sent
     * @param socket
     *            - socket that will be used to send packet
     * @param returnAddress
     *            - address the packet will be send too
     * 
     * @author bloo
     */
    protected void send(byte[] msg, DatagramSocket socket, SocketAddress returnAddress) {
	send(new DatagramPacket(msg, msg.length, returnAddress), socket);
    }

    /**
     * creates a packet to be sent by base send() method
     * 
     * @param msg
     *            - byte array to be sent
     * @param socket
     *            - socket that will be used to send packet
     * @param address
     *            - address the packet will be sent too
     * @param port
     *            - port the packet will be send too
     * 
     * @author bloo
     */
    protected void send(byte[] msg, DatagramSocket socket, InetAddress address, int port) {
	DatagramPacket sendPacket = new DatagramPacket(msg, msg.length, address, port);
	send(sendPacket, socket);
    }

    /**
     * Base send method, used to send sendPacket over given socket
     * 
     * @param sendPacket
     *            - packet to be sent, includes destination
     * @param socket
     *            - socket the packet will sent over
     * 
     * @author bloo
     */
    protected void send(DatagramPacket sendPacket, DatagramSocket socket) {
	try {
	    if (verbose) {
		println("Sending: ");
		println(TFTPPacket.packetToString(sendPacket));
	    }

	    socket.send(sendPacket);
	    lastSentPkt = sendPacket;
	} catch (IOException e) {
	    socket.close();
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    /**
     * receives a file over a datagram socket and saves at specified location this
     * method receives the read request acknowledge an initial data packet over the
     * socket
     * 
     * @param socket
     *            - Socket where communication will take place
     * @param file
     *            - the file path where the file will be saced
     */
    protected void receiveFile(DatagramSocket socket, String file) throws IOException {
	receiveFile(receive(socket), socket, file); // Calls the receiveFile below
    }

    /**
     * receives a file in tftp packets. when all packets have been received the data
     * is sent to be saved
     * 
     * @param packet
     *            - the initial acknoledge indicating the start of data transfer
     * @param socket
     *            - the socket used to communicate
     * @param file
     *            - where the received file will be stored
     */
    protected void receiveFile(DatagramPacket packet, DatagramSocket socket, String file) throws IOException {
	ArrayList<byte[]> data = new ArrayList<byte[]>();
	SocketAddress returnAddress = packet.getSocketAddress();
	DatagramPacket receivePacket = null;

	if (TFTPPacket.getType(packet) == TFTPPacket.OP_DATA) { // if the initial packet is a data packet
	    if (TFTPPacket.getBlockNum(packet) == 1) {
		data.add(TFTPPacket.getByteData(packet));

		if (TFTPPacket.getDataLength(packet) != MAX_DATA_SIZE) {
		    send(TFTPPacket.createAck(data.size()), socket, returnAddress);
		    saveFile(data, file);
		    return;
		}
	    } else {
		return;
	    }

	} else if (TFTPPacket.getType(packet) == TFTPPacket.OP_ERROR) {
	    System.err.println("\\nError Occured\\n" + TFTPPacket.getErrorMsg(packet));
	    return;
	}

	do {
	    for (int i = 0; i < TRANSMIT_LIMIT; i++) {
		try {
		    send(TFTPPacket.createAck(data.size()), socket, returnAddress);

		    do {
			receivePacket = receive(socket);
			if (isLast(receivePacket)) {
			    send(lastSentPkt, socket);
			    continue;
			}
		    } while (!isFrom(receivePacket, socket, returnAddress) || !isNext(receivePacket));

		    if (TFTPPacket.getType(receivePacket) == TFTPPacket.OP_DATA) {
			data.add(TFTPPacket.getByteData(receivePacket));
			break;
		    } else if (TFTPPacket.getType(receivePacket) == TFTPPacket.OP_ERROR) {
			System.err.println("ERROR: " + TFTPPacket.getErrorMsg(receivePacket));
			return;
		    } else {
			return;
		    }
		} catch (SocketTimeoutException e) {
		    if (verbose) {
			if (i % 50 == 0)
			    print("\n");
			print(".");
		    }
		    if (i >= TRANSMIT_LIMIT) {
			print("Connection timed out \n Stopping transfer");
			return;
		    }
		}
	    }
	} while (receivePacket == null || (TFTPPacket.getType(receivePacket) != TFTPPacket.OP_DATA
		^ TFTPPacket.getDataLength(receivePacket) == MAX_DATA_SIZE));

	send(TFTPPacket.createAck(data.size()), socket, returnAddress);
	// continue if the last received packet hold data and are full
	// if the last received packet isn't data
	// or if nothing was received last
	saveFile(data, file);
    }

    /**
     * receives the next in order packet allows for the rejection of incorrect
     * packets
     * 
     * @param socket
     *            - socket the packet will be received on
     * @param length
     *            - the length of the expected pocket
     * @return the next packet received in order, will return null if the max number
     *         of time outs is reached
     * @throws SocketTimeoutException
     * @author bloo
     */
    protected DatagramPacket receiveNext(DatagramSocket socket) throws SocketTimeoutException {
	return receiveNext(socket, MAX_PACKET_SIZE);
    }

    /**
     * receives the next in order packet allows for the rejection of incorrect
     * packets
     * 
     * @param socket
     *            - socket the packet will be received on
     * @param length
     *            - the length of the expected pocket
     * @return the next packet received in order, will return null if the max number
     *         of time outs is reached
     * @throws SocketTimeoutException
     * @author bloo
     */
    protected DatagramPacket receiveNext(DatagramSocket socket, int length) throws SocketTimeoutException {
	DatagramPacket receivedPacket;
	while (true) {
	    receivedPacket = receive(socket, MAX_PACKET_SIZE);
	    /*
	     * println((lastSentPkt == null) + ", " + (getType(receivedPacket) == OP_ACK &&
	     * getType(lastSentPkt) == OP_DATA && getBlockNum(receivedPacket) ==
	     * getBlockNum(lastSentPkt)) + ", " + (getType(receivedPacket) == OP_DATA &&
	     * getType(lastSentPkt) == OP_ACK && getBlockNum(receivedPacket) ==
	     * getBlockNum(lastSentPkt) + 1) + ", " + (getType(receivedPacket) ==
	     * OP_ERROR));
	     */

	    if (lastSentPkt == null
		    || (TFTPPacket.getType(receivedPacket) == TFTPPacket.OP_ACK
			    && TFTPPacket.getType(lastSentPkt) == TFTPPacket.OP_DATA
			    && TFTPPacket.getBlockNum(receivedPacket) == TFTPPacket.getBlockNum(lastSentPkt))
		    || (TFTPPacket.getType(receivedPacket) == TFTPPacket.OP_DATA
			    && TFTPPacket.getType(lastSentPkt) == TFTPPacket.OP_ACK
			    && TFTPPacket.getBlockNum(receivedPacket) == TFTPPacket.getBlockNum(lastSentPkt) + 1)
		    || TFTPPacket.getType(receivedPacket) == TFTPPacket.OP_ERROR) {
		return receivedPacket;
	    }
	}
    }

    /**
     * Base receive method
     * 
     * Receives a DatagramPacket over the given socket
     * 
     * @param socket
     *            - socket packet will be received at
     * @return receivedPacket unless there is an exception trying to receive
     * 
     * @author bloo
     * @throws SocketTimeoutException
     */
    protected DatagramPacket receive(DatagramSocket socket) throws SocketTimeoutException {
	return receive(socket, MAX_PACKET_SIZE);
    }

    /**
     * Base receive method Receives a DatagramPacket over the given socket
     * 
     * @param socket
     *            - socket to receive from
     * @param length
     *            - size of potential packet
     * @return receivePacket unless there is an exception trying to receive
     * 
     * @author bloo
     * @throws SocketTimeoutException
     */
    protected DatagramPacket receive(DatagramSocket socket, int length) throws SocketTimeoutException {
	DatagramPacket receivedPacket;

	while (true) {
	    receivedPacket = new DatagramPacket(new byte[length], length);

	    try {
		socket.receive(receivedPacket);
		if (!isValid(receivedPacket, socket))
		    continue;

		if (verbose) {
		    println("received: ");
		    println(TFTPPacket.packetToString(receivedPacket));
		}

		return receivedPacket;

	    } catch (IOException e) {
		if (e instanceof SocketTimeoutException)
		    throw (SocketTimeoutException) e;
		e.printStackTrace();
		System.exit(1);
	    }
	    return new DatagramPacket(new byte[0], 0);
	}

    }

    protected boolean isLast(DatagramPacket packet) {
	if (lastSentPkt == null
		|| (TFTPPacket.getType(packet) == TFTPPacket.OP_ACK
			&& TFTPPacket.getType(lastSentPkt) == TFTPPacket.OP_DATA
			&& TFTPPacket.getBlockNum(packet) == TFTPPacket.getBlockNum(lastSentPkt) - 1)
		|| (TFTPPacket.getType(packet) == TFTPPacket.OP_DATA
			&& TFTPPacket.getType(lastSentPkt) == TFTPPacket.OP_ACK
			&& TFTPPacket.getBlockNum(packet) == TFTPPacket.getBlockNum(lastSentPkt))
		|| TFTPPacket.getType(packet) == TFTPPacket.OP_ERROR)
	    return true;
	return false;
    }

    protected boolean isNext(DatagramPacket packet) {
	if (lastSentPkt == null
		|| (TFTPPacket.getType(packet) == TFTPPacket.OP_ACK
			&& TFTPPacket.getType(lastSentPkt) == TFTPPacket.OP_DATA
			&& TFTPPacket.getBlockNum(packet) == TFTPPacket.getBlockNum(lastSentPkt))
		|| (TFTPPacket.getType(packet) == TFTPPacket.OP_DATA
			&& TFTPPacket.getType(lastSentPkt) == TFTPPacket.OP_ACK
			&& TFTPPacket.getBlockNum(packet) == TFTPPacket.getBlockNum(lastSentPkt) + 1)
		|| TFTPPacket.getType(packet) == TFTPPacket.OP_ERROR)
	    return true;
	return false;
    }

    /**
     * Authenticates packet
     * 
     * @param packet
     * @throws IOException
     * @author Benjamin, BLoo
     */
    public boolean isValid(DatagramPacket packet, DatagramSocket socket) {
	byte[] data = packet.getData();
	byte[] hold;

	Set<Byte> validPackets = TFTPPacket.PacketTypes.keySet();
	if (data[0] == ZERO_BYTE && validPackets.contains(data[1])) // Checks packet type formatting
	{
	    switch (data[1]) {
	    case (byte) 1:
	    case (byte) 2: /* RRQ & WRQ Packet */
	    {
		hold = TFTPPacket.readToStop(2, packet.getData(), packet.getLength());
		if (hold != null) {
		    int i = hold.length + 3;
		    i += TFTPPacket.readToStop(hold.length + 3, packet.getData(), packet.getLength()).length;
		    System.out.println(i + ", " + (packet.getLength() - 1));
		    return i == packet.getLength() - 1;
		}
		/*
		 * Find offset of mode field by reading the first field and adding that fields
		 * length, accounting for the terminating zero and initial offset, to the offset
		 */
	    }
	    case (byte) 3: /* DATA Packet */
	    {
		hold = TFTPPacket.readToStop(4, data, packet.getLength());
		System.out.println((hold != null) + ", " + (hold.length) + "," + (packet.getLength() - 4));
		return (hold != null && hold.length == packet.getLength() - 4);
	    }
	    case (byte) 4: /* ACK Packet */
	    {
		System.out.println(packet.getLength() + ", " + 4);
		return packet.getLength() == 4;
	    }
	    case (byte) 5: /* ERROR Packet */
	    {
		hold = TFTPPacket.readToStop(4, packet.getData(), packet.getLength());
		System.out.println((hold != null) + ", " + (hold.length) + ", " + (packet.getLength() - 5));
		return (hold != null && hold.length == packet.getLength() - 5);
	    }
	    }
	}

	if (socket != null)
	    send(TFTPPacket.createError(4, "Illegal TFTP operation.".getBytes()), socket, packet.getSocketAddress());
	return false;
    }

    private void isValidTest() {
	byte[] file = "test.txt".getBytes();
	byte[] contents = new byte[512];
	byte[] rrq, wrq, data, ack, error;

	for (int i = 0; i < contents.length; i++)
	    contents[i] = (byte) (Math.random() * 24 + 66);

	rrq = TFTPPacket.createRQ((byte) 1, file, MODE_NETASCII);
	wrq = TFTPPacket.createRQ((byte) 2, file, MODE_NETASCII);

	data = TFTPPacket.createData(5, contents);

	DatagramPacket dp = new DatagramPacket(data, data.length);
	data = dp.getData();

	ack = TFTPPacket.createAck(5);
	error = TFTPPacket.createError((byte) 1, MODE_OCTET);

	System.out.println("rrq test: " + isValid(new DatagramPacket(rrq, rrq.length), null));

	System.out.println("wrq test: " + isValid(new DatagramPacket(wrq, wrq.length), null));

	System.out.println("data test: " + isValid(new DatagramPacket(data, data.length), null));

	System.out.println("ack test: " + isValid(new DatagramPacket(ack, ack.length), null));

	System.out.println("error test: " + isValid(new DatagramPacket(error, error.length), null));
    }

    protected boolean isFrom(DatagramPacket packet, DatagramSocket socket, SocketAddress expectedSender) {
	if (packet.getSocketAddress().equals(expectedSender))
	    return true;
	send(TFTPPacket.createError(5, "Packet received from an unrecognised TID".getBytes()), socket,
		packet.getSocketAddress());

	return false;
    }

    /**
     * Split file into 512 byte chunks
     *
     * @param fileNameb
     *            - file to be split
     * @return
     * @throws IOException
     * @author BenjaminP
     */
    protected ArrayList<byte[]> readFile(String fileName) throws IOException {
	ArrayList<byte[]> parsedData = new ArrayList<byte[]>();
	byte[] buffer = new byte[512];
	byte[] byteData;

	byteData = Files.readAllBytes(Paths.get(fileName));

	for (int i = 0; i < byteData.length; i += 512) {
	    if (i + 512 <= byteData.length) {
		buffer = Arrays.copyOfRange(byteData, i, i + 512);
	    } else {
		buffer = Arrays.copyOfRange(byteData, i, byteData.length);
	    }

	    parsedData.add(buffer);
	}

	println(parsedData.get(parsedData.size() - 1).length + "");
	if (parsedData.get(parsedData.size() - 1).length == 512)
	    parsedData.add(new byte[0]);
	return parsedData;
    }

    /**
     * Saves data blocks to a file
     * 
     * @param data
     *            - an arraylist of byte arrays storing the byte data
     * @param fileName
     *            - the name of the file where the data will be stored
     * @return the number blocks saved
     * @throws IOException
     * @author Eric
     */
    protected int saveFile(ArrayList<byte[]> data, String fileName) throws IOException {
	if (new File(new File(fileName).getParent()).getUsableSpace() < data.size() * 512)
	    throw new FullFileSystemException("File " + fileName + "cannot fit the file's " + data.size() * 512
		    + "bytes. File has space " + new File(fileName).getUsableSpace());
	OutputStream file = new FileOutputStream(fileName);

	for (int i = 0; i < data.size(); i++) {
	    file.write(data.get(i));
	}

	file.close();
	return data.size();
    }

    /**
     * @param b
     * 
     * @author Benjamin
     */
    public void setScrollBar(JScrollBar b) {
	this.scrollBar = b;
    }

    /**
     * Gets textarea attached to child
     * 
     * @return
     * @author Benjamin
     */
    public JTextArea getOutputWindow() {
	return this.outputWindow;
    }

    /**
     * Prints to UI and console
     * 
     * @param s
     * @author Benjamin
     */
    public void print(String s) {
	System.out.println(s);
	this.outputWindow.append(s);
	this.scrollBar.setValue(scrollBar.getMaximum() + 1);
    }

    /**
     * Prints to UI and console
     * 
     * @param s
     * 
     * @author Benjamin
     */
    public void println(String s) {
	System.out.println(s + "\n");
	this.outputWindow.append(s + "\n");
	this.scrollBar.setValue(scrollBar.getMaximum() + 1);
    }

    public abstract void takeInput(String s);

    protected class FullFileSystemException extends IOException {
	private static final long serialVersionUID = 7770593212561838179L;

	FullFileSystemException(String msg) {
	    super(msg);
	}
    }
}
