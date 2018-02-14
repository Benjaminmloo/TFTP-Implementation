import java.awt.Container;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.*;

public class UserInterface {
	
	private JFrame frame;
	
	public UserInterface()
	{
		frame = new JFrame("File Transfer System");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		GridLayout grid = new GridLayout(2,2);
		frame.setLayout(grid);
		
		Container inputContainer = new Container();
		frame.add(inputContainer);
		
		
		JTextArea clientText =  new JTextArea();
		PrintStream clientout = new PrintStream(	new TextAreaOutputStream(clientText)	);
		JScrollPane client = new JScrollPane(clientText);
		System.setOut(clientout);
		System.setErr(clientout);
		client.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		client.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		frame.add(client);
		
		
		frame.pack();
		frame.setVisible(true);
		frame.setSize(450, 450);
		System.out.println("AHUR DUR BURG");
	}
	
	public static void main(String args[])
	{
		UserInterface ui = new UserInterface();
	}

	
	
	public class TextAreaOutputStream extends OutputStream
	{
		private JTextArea textControl;
		
		public TextAreaOutputStream(	JTextArea control	)
		{
			textControl = control;
		}
		
		public void write(int b)	throws IOException
		{
			textControl.append(	String.valueOf(b)	);
		}
	}
}
