import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class Client {

	/**
	 * Type of mode the user has entered ("Read" or "Write")
	 */
	private String mode;
	
	/**
	 * Name of the file the user has entered
	 */
	private String file;

	/**
	 * The DatagramSocket that the client will use to send and receive
	 */
	private DatagramSocket sendReceive;	
	
	private ErrorSimulator simulator;
	
	public Client() {
		try {
			sendReceive = new DatagramSocket();
			simulator = new ErrorSimulator();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method will print the byte array and also print it as a string
	 * 
	 * @param array
	 *            the array that needs to be printed
	 * @param length
	 */
	public static void printByteArray(byte[] array, int length) {
		for (int j = 0; j < length; j++) {
			System.out.print(array[j]);
		}
		System.out.println();
		System.out.println(new String(array, 0, length));
	}

	/**
	 * This method will create the request that the server will send to the
	 * intermediate host
	 * 
	 * @param requestType
	 *            The type of request being set (Write, Read, Invalid). Default
	 *            is Read
	 * @param fileName
	 *            the name of the file
	 * @param mode
	 *            the mode being used
	 * @return the byte array that of the request
	 */
	public byte[] createRequest(String requestType, String fileName, String mode) {
		byte[] b = new byte[fileName.length() + mode.length() + 4];
		if (requestType.equals("Invalid")) {
			String invalid = "Invalid Request";
			return invalid.getBytes();
		}
		b[0] = (byte) 0;
		b[1] = (byte) 1;
		if (requestType.equals("Write"))
			b[1] = 2;
		System.arraycopy(fileName.getBytes(), 0, b, 2, fileName.length());
		b[fileName.length() + 2] = 0;
		System.arraycopy(mode.getBytes(), 0, b, fileName.length() + 3, mode.length());
		b[fileName.length() + mode.length() + 3] = 0;
		return b;
	}

	public void launchUserInterface() {
		JLabel modeLabel = new JLabel("Enter Command (\"Read\" or \"Write\" or \"Close\")");
		mode = JOptionPane.showInputDialog(null, modeLabel, "Enter Mode", JOptionPane.INFORMATION_MESSAGE).toLowerCase();
		JLabel fileLabel = new JLabel("Enter File Name");
		file = JOptionPane.showInputDialog(null, fileLabel, "Enter File Name", JOptionPane.INFORMATION_MESSAGE).toLowerCase();
	}

	/**
	 * This method is used to run the Client
	 */
	public void runClient() {
		System.out.println("--------------------------------------");
		while(true) {
			launchUserInterface();
			byte[] request;
			if (mode.equals("read")) {
				System.out.println("Read Request");
				request = createRequest("Read", file, "mode");
			} else if (mode.equals("write")) {
				System.out.println("Write Request");
				request = createRequest("Write", file, "mode");
			} else if (mode.equals("close")){
				request = createRequest("Invalid", file, "mode");
			} else {
				continue;
			}
			
			System.out.println("Sending following data to port Error Simulator: ");
			DatagramPacket p;
			try {
				// send to host
				p = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), simulator.getClientPort());
				printByteArray(request, request.length);
				sendReceive.send(p);

				// Receive from host
				byte[] receive = new byte[4];
				DatagramPacket received = new DatagramPacket(receive, receive.length);
				sendReceive.receive(received);
				if (receive[0] == 0 && receive[1] == 4) { //ACK BLOCK
					BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
					byte[] sendingData = new byte[512];
					int x;
					while ((x = input.read(sendingData)) != -1) {
						System.out.println(x);
						DatagramPacket fileTransfer = new DatagramPacket(sendingData, x, InetAddress.getLocalHost(), received.getPort());
						sendReceive.send(fileTransfer);
						sendReceive.receive(received);
					}
					byte[] empty = new byte[0];
					DatagramPacket fileTransfer = new DatagramPacket(empty, 0, InetAddress.getLocalHost(), received.getPort());
					sendReceive.send(fileTransfer);
				}

				System.out.println("Received the following from host:");
				printByteArray(receive, received.getLength());
				System.out.println("--------------------------------------");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public static void main(String args[]) {
		Client c = new Client();
		c.runClient();
	}

}
