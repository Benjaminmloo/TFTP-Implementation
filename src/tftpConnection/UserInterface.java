package tftpConnection;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.swing.*;
import javax.swing.text.DefaultCaret;

/**
 * 
 * @author Benjamin, Andrew
 *
 */
public class UserInterface {

    private JFrame frame;
    private JTextField inputField;
    private JTabbedPane tabPane, tabPane2, tabPane3;

    public Client client;
    private ErrorSimulator errorSim;
    private Server server;

    private JTextArea clientArea, errorArea, serverArea;
    private JScrollPane clientWindow, errorWindow, serverWindow;
    private JPanel outputPanel, inputPanel, subOutputPanel;

    DefaultCaret caret;

    public UserInterface() {
	errorSim = new ErrorSimulator();
	client = new Client(errorSim);
	server = new Server(69);

	frame = new JFrame("File Transfer System");
	ImageIcon frameImage = new ImageIcon("Misc//icon.png");
	frame.setIconImage(frameImage.getImage());
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	MenuListener menuListener = new MenuListener();
	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu = new JMenu("File");
	menuBar.add(fileMenu);
	JMenuItem clientIP = new JMenuItem("Client IP");
	clientIP.addActionListener(menuListener);
	JMenuItem editServer = new JMenuItem("Server IP");
	editServer.addActionListener(menuListener);
	JMenuItem resetServer = new JMenuItem("Reset Server "
		+ "IP");
	resetServer.addActionListener(menuListener);
	fileMenu.add(clientIP);
	fileMenu.add(editServer);
	fileMenu.add(resetServer);

	tabPane = new JTabbedPane();
	tabPane2 = new JTabbedPane();
	tabPane3 = new JTabbedPane();
	inputField = new JTextField();
	inputField.addActionListener(new InputListener(inputField));

	clientArea = client.getOutputWindow();
	clientArea.setEditable(false);
	clientWindow = new JScrollPane(clientArea);
	client.setScrollBar(clientWindow.getVerticalScrollBar());
	clientWindow.setPreferredSize(new Dimension(600, 400));
	clientWindow.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

	caret = (DefaultCaret) clientArea.getCaret();
	caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

	errorArea = errorSim.getOutputWindow();
	errorArea.setEditable(false);
	errorWindow = new JScrollPane(errorArea);
	errorSim.setScrollBar(errorWindow.getVerticalScrollBar());
	errorWindow.setPreferredSize(new Dimension(600, 400));
	errorWindow.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

	caret = (DefaultCaret) errorArea.getCaret();
	caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

	serverArea = server.getOutputWindow();
	serverArea.setEditable(false);
	serverWindow = new JScrollPane(serverArea);
	server.setScrollBar(serverWindow.getVerticalScrollBar());
	serverWindow.setPreferredSize(new Dimension(600, 400));
	serverWindow.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

	caret = (DefaultCaret) serverArea.getCaret();
	caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

	tabPane.add("Client", clientWindow);
	tabPane2.add("Error Simulator", errorWindow);
	tabPane3.add("Server", serverWindow);

	/*
	 * Since it is a gridLayout, it'll always be symmetrical. A SubOutputPanel is
	 * made for error and server to reduce The size of the window and give the
	 * attention onto the Client window.
	 */
	outputPanel = new JPanel();
	subOutputPanel = new JPanel();
	inputPanel = new JPanel();

	GridBagConstraints gbc = new GridBagConstraints();

	outputPanel.setLayout(new GridLayout(0, 2));
	subOutputPanel.setLayout(new GridLayout(0, 2));
	inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));

	outputPanel.add(tabPane, gbc);
	subOutputPanel.add(tabPane2, gbc);
	subOutputPanel.add(tabPane3, gbc);
	outputPanel.add(subOutputPanel, gbc);

	inputPanel.add(new JLabel("Input commands here: "));
	inputPanel.add(inputField);

	/*
	 * The old interface if we want to revert back. outputPanel.setLayout(new
	 * BoxLayout(outputPanel, BoxLayout.Y_AXIS));
	 * 
	 * outputPanel.add(tabPane, gbc); outputPanel.add(new
	 * JLabel("Input commands here: "), gbc); outputPanel.add(inputField, gbc);
	 */

	frame.setJMenuBar(menuBar);
	frame.add(outputPanel, BorderLayout.CENTER);
	frame.add(inputPanel, BorderLayout.SOUTH);

	frame.pack();
	frame.setVisible(true);
	frame.setSize(920, 480);
	inputField.requestFocusInWindow();

	Thread clientThread = new Thread(new WindowThread(client));
	clientThread.start();
	Thread errorThread = new Thread(new WindowThread(errorSim));
	errorThread.start();
	Thread serverThread = new Thread(new WindowThread(server));
	serverThread.start();
    }

    private void forwardInput(String s) {
	client.getOutputWindow().append(s + "\n");
	client.takeInput(s);
    }

    public void requestInputFocus() {
	this.inputField.requestFocus();

    }

    public static void main(String args[]) {
	UserInterface UI = new UserInterface();
	UI.requestInputFocus();
    }

    /*
     * Sub-classes
     * 
     * Used for I/O
     */

    public class InputListener implements ActionListener {
	JTextField field;

	public InputListener(JTextField f) {
	    field = f;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
	    // TODO Auto-generated method stub
	    forwardInput(field.getText());
	    field.setText("");
	}

    }

    public class WindowThread implements Runnable {
	TFTPConnection connection;

	public WindowThread(TFTPConnection c) {
	    connection = c;
	}

	@Override
	public void run() {
	    // TODO Auto-generated method stub
	    if (connection instanceof Client) {
		((Client) connection).userInterface();
	    } else if (connection instanceof ErrorSimulator) {
		((ErrorSimulator) connection).startPassthrough();
	    } else if (connection instanceof Server) {
		((Server) connection).userInterface();
	    }
	}
    }

    public class MenuListener implements ActionListener {

	@Override
	public void actionPerformed(ActionEvent e) {
	    JMenuItem item = (JMenuItem) e.getSource();

	    switch (item.getText()) {
	    case "Client IP": {
		try {
		    InetAddress clientAddress = InetAddress.getLocalHost();
		    JOptionPane.showMessageDialog(frame, clientAddress.toString());
		} catch (UnknownHostException ue) {
		    ue.printStackTrace();
		}
		break;
	    }
	    case "Server IP": {
		String in = (String) JOptionPane.showInputDialog(frame,
			"Current server " + client.getServerAddress() + "\nEnter new address\n");
		if (in != null && in.length() > 0) {
		    try {
			client.setServerAddress(InetAddress.getByName(in));
		    } catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
	    		    e1.printStackTrace();
	    		    JOptionPane.showMessageDialog(frame, "Invalid server address");
	    		    break;
	    		}
	    		JOptionPane.showMessageDialog(frame, "Server IP Changed!");
	    	    }
	    	break;
	    	}    
	    	case "Reset Server IP":
	    	{
	    	try {
	    	    client.setServerAddress(InetAddress.getLocalHost());
		} catch (UnknownHostException ue) {
		    ue.printStackTrace();
		    JOptionPane.showMessageDialog(frame, "Default Host initialization error\n");
		    break;
		}
	    	JOptionPane.showMessageDialog(frame, "Server IP Reset");
	    	break;
	    	}
	    }
	}	
    }
}