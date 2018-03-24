package tftpConnection;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

    private Client client;
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
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	tabPane = new JTabbedPane();
	tabPane2 = new JTabbedPane();
	tabPane3 = new JTabbedPane();
	inputField = new JTextField();
	inputField.addActionListener(new InputController(inputField));

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
	 * Since it is a gridLayout, it'll always be symetrical. A SubOutputPanel is
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

	// The old interface if we want to revert back.
	// outputPanel.setLayout(new BoxLayout(outputPanel, BoxLayout.Y_AXIS));

	// outputPanel.add(tabPane, gbc);
	// outputPanel.add(new JLabel("Input commands here: "), gbc);
	// outputPanel.add(inputField, gbc);

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
	switch (tabPane.getSelectedIndex()) {
	case 0:
	    client.getOutputWindow().append(s + "\n");
	    client.takeInput(s);
	    break;
	case 1:
	    errorSim.getOutputWindow().append(s + "\n");
	    errorSim.takeInput(s);
	    break;
	case 2:
	    server.getOutputWindow().append(s + "\n");
	    server.takeInput(s);

	    break;
	}
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

    public class InputController implements ActionListener {
	JTextField field;

	public InputController(JTextField f) {
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
}
