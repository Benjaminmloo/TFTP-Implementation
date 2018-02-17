import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.*;

public class UserInterface {
	
	private JFrame frame;
	private JTextField inputField;
	
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
		GridLayout grid = new GridLayout(2,2);
		frame.setLayout(grid);
		
		Container inputContainer = new Container();
		inputField = new JTextField("Input text here");
		inputContainer.add(inputField);
		inputField.addActionListener(	new InputController(inputField)	);
		inputContainer.setVisible(true);
		frame.add(inputField);
		
		
		JTextArea clientArea = client.getOutputWindow();
		clientArea.append("Client output:\n");
		clientArea.setEditable(false);
		JScrollPane clientWindow = new JScrollPane(clientArea);
		clientWindow.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		clientWindow.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		frame.add(clientWindow);
		
		JTextArea errorArea = errorSim.getOutputWindow();
		errorArea.append("ErrorSim output:\n");
		errorArea.setEditable(false);
		JScrollPane errorWindow = new JScrollPane(errorArea);
		errorWindow.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		errorWindow.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		frame.add(errorWindow);

		JTextArea serverArea = server.getOutputWindow();
		serverArea.append("Server output:\n");
		serverArea.setEditable(false);
		JScrollPane serverWindow = new JScrollPane(serverArea);
		serverWindow.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		serverWindow.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		frame.add(serverWindow);
		
		
		frame.pack();
		frame.setVisible(true);
		frame.setSize(720, 480);
		
		WindowThread clientThread = new WindowThread(client);
		clientThread.run();
		WindowThread errorThread = new WindowThread(errorSim);
		errorThread.run();
		WindowThread serverThread = new WindowThread(server);
		serverThread.run();
	}
	
	private void forwardInput(String s)
	{
		client.getOutputWindow().append(s + "\n");
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
