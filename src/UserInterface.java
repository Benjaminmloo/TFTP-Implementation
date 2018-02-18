import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

/**
 * 
 * @author Benjamin
 *
 */
public class UserInterface {
	
	private JFrame frame;
	private JTextField inputField;
	private JTabbedPane tabPane;
	
	private Client client;
	private ErrorSimulator errorSim;
	private Server server;
	
	public UserInterface()
	{
		client = new Client();
		errorSim = new ErrorSimulator();
		server = new Server(69);
		
		frame = new JFrame("File Transfer System");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		tabPane = new JTabbedPane();
		inputField = new JTextField();
		inputField.addActionListener(	new InputController(inputField)	);
		
		
		JTextArea clientArea = client.getOutputWindow();
		clientArea.setEditable(false);
		JScrollPane clientWindow = new JScrollPane(clientArea);
		clientWindow.setPreferredSize(new Dimension(600, 400));
		clientWindow.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		tabPane.add("Client", clientWindow);

		
		JTextArea errorArea = errorSim.getOutputWindow();
		errorArea.setEditable(false);
		JScrollPane errorWindow = new JScrollPane(errorArea);
		errorWindow.setPreferredSize(new Dimension(600, 400));
		errorWindow.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		tabPane.add("Error Simulator", errorWindow);


		JTextArea serverArea = server.getOutputWindow();
		serverArea.setEditable(false);
		JScrollPane serverWindow = new JScrollPane(serverArea);
		serverWindow.setPreferredSize(new Dimension(600, 400));
		serverWindow.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		tabPane.add("Server", serverWindow);

		
		JPanel panel = new JPanel();
		
		GridBagConstraints gbc =  new GridBagConstraints();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(tabPane, gbc);
		panel.add(new JLabel("Input commands here: "), gbc);
		panel.add(inputField, gbc);
		frame.add(panel);
		
		
		frame.pack();
		frame.setVisible(true);
		frame.setSize(720, 480);
		
		Thread clientThread = new Thread(	new WindowThread(client));
		clientThread.start();
		Thread errorThread = new Thread(	new WindowThread(errorSim));
		errorThread.start();
		Thread serverThread = new Thread(	new WindowThread(server));
		serverThread.start();
	}
	
	private void forwardInput(String s)
	{
		
		switch (tabPane.getSelectedIndex())
		{
		case 0:
			client.takeInput(s);
			client.getOutputWindow().append(s + "\n");
			client.takeInput(s);
			break;
		case 1:
			errorSim.takeInput(s);
			errorSim.getOutputWindow().append(s + "\n");
			errorSim.takeInput(s);
			break;
		case 2:
			server.takeInput(s);
			server.getOutputWindow().append(s + "\n");
			server.takeInput(s);
			break;
		}
	}
	
	public static void main(String args[])
	{
		UserInterface ui = new UserInterface();
	}
	
	
	/*	
	 * Sub-classes 
	 * 
	 * Used for I/O 
	 * */
	
	public class InputController implements ActionListener
	{
		JTextField field;
		
		public InputController(JTextField f)
		{
			field = f;
		}

		@Override
		public void actionPerformed(ActionEvent arg0) {
			// TODO Auto-generated method stub
			forwardInput(field.getText());
			field.setText("");
		}
		
	}
	
	public class WindowThread implements Runnable
	{
		TFTPConnection connection;

		public WindowThread(TFTPConnection c)
		{
			connection = c;
		}
		
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(connection instanceof Client)
			{
				((Client) connection).userInterface();
			}
			else if(connection instanceof ErrorSimulator)
			{
				((ErrorSimulator) connection).startPassthrough();
			}
			else if(connection instanceof Server)
			{
				((Server) connection).userInterface();
			}
		}
		
	}
}
