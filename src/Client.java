import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import javax.swing.JOptionPane;

public class Client {

	/**
	 * Type of mode the user has entered ("Read" or "Write")
	 */
	private String mode;
	
	/**
	 *  Name of the file the user has entered
	 */
	private String file;

	/**
	 *  The DatagramSocket that the client will use to send and receive
	 */
	private DatagramSocket sendReceive;	
	
	/** 
	 * ErrorSimulator object created with client for future iterations
	 */
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
	 * This method will print the byte arrays contents as a byte and has a
	 * string
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
	
	
	/**
	 * Launches a dialog box for user input
	 * Options:
	 * 	- accepts "read" or "write" as text (not case sensitive). 
	 *  - once valid "read" or "write" is detected will prompt
	 *    for file name.
	 *  - once valid filename is given, it will allow runClient() to
	 *    continue
	 *  - to close, simply press cancel button
	 */
	public void launchUserInterface() {
		boolean validCommand = false;
		boolean validFileName = false;
		while (!validCommand) {
			try {
				mode = JOptionPane.showInputDialog(null, "Enter Command (\"Read\" or \"Write\")", "Enter Mode", JOptionPane.INFORMATION_MESSAGE).toLowerCase();
				if (mode == null) {
					System.exit(0);
				} else if (mode.equals("read") || mode.equals("write")) {
					validCommand = true; // break while loop and continue to next check
				}
			} catch (NullPointerException e) { // If null, cancel has been selected so close program
				System.out.println("Closing client");
				System.exit(0);
			}
		}
		while (!validFileName) {
			try {
				file = JOptionPane.showInputDialog(null, "Enter File Name (including file extension ie. *.txt)", "Enter File Name", JOptionPane.INFORMATION_MESSAGE).toLowerCase();
				if (new File(file).exists()) { // check if file exists with that name, if yes break and return to runClient()
					validFileName = true;
				} else {
					System.out.println("File does not exist!"); //re-prompt user for valid file name
				}
			} catch (NullPointerException e) {
				System.out.println("Closing client");
				sendReceive.close();
				System.exit(0);
			}
			
		}
	}

	/**
	 * This method is used to run the Client
	 * - Calls UI to prompt user for mode type and file name
	 * - Checks what user requested
	 * - Constructs packet and either calls read or write
	 * - Sends to ErrorSimulator, which is forwarded to server
	 * - Waits for ACK
	 * - Re-prompts user until cancel button is clicked
	 */
	public void runClient() {
		System.out.println("--------------------------------------");
		while(true) {
			launchUserInterface();
			byte[] request;
			if (mode.equals("read")) { // check request type
				System.out.println("Read Request");
				request = createRequest("Read", file, "mode");
			} else if (mode.equals("write")) {
				System.out.println("Write Request");
				request = createRequest("Write", file, "mode");
			} else {
				continue;
			}

			System.out.println("Sending following data to port Error Simulator: ");
			DatagramPacket p;
			try {
				// send to ErrorSimulator
				simulator = new ErrorSimulator();
				p = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), simulator.getClientPort());
				printByteArray(request, request.length);
				sendReceive.send(p);
				
				// Receive from ErrorSimulator
				byte[] receive = new byte[516];
				DatagramPacket received = new DatagramPacket(receive, receive.length);
				sendReceive.receive(received);
				if (receive[0] == 0 && receive[1] == 4) { // Form write packet and send
					BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
					byte[] sendingData = new byte[512];
					int x;
					byte block = 1;
					while ((x = input.read(sendingData)) != -1) {
						byte[] sendingMessage = new byte[516];
						sendingMessage[0] = 0;
						sendingMessage[1] = 3;
						sendingMessage[2] = 0;
						sendingMessage[3] = block;
						block++;
						System.arraycopy(sendingData, 0, sendingMessage, 4, sendingData.length);
						DatagramPacket fileTransfer = new DatagramPacket(sendingMessage, x + 4, InetAddress.getLocalHost(), received.getPort());
						sendReceive.send(fileTransfer);
						sendReceive.receive(received);
					}
					input.close();
				} else if (receive[0] == 0 && receive[1] == 3) { // Form read packet and send
					byte[] ack = new byte[4];
					ack[0] = 0;
					ack[1] = 4;
					ack[2] = 0;
					byte block = 0;
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("read.txt"));
					out.write(receive, 4, received.getLength() - 4);
					DatagramPacket acknowledge = new DatagramPacket(ack, 4, InetAddress.getLocalHost(), received.getPort());
					sendReceive.send(acknowledge);
					int x = received.getLength();
					while (x == 516) {
						ack[3] = block;
						block++;
						byte[] receiveFile = new byte[516];
						DatagramPacket fileTransfer = new DatagramPacket(receiveFile, receiveFile.length);						
						sendReceive.receive(fileTransfer);
						out.write(receiveFile, 4, fileTransfer.getLength() - 4);
						sendReceive.send(acknowledge);
						x = fileTransfer.getLength();
					}
					out.close();
				} else if (receive[0] == 0 && receive[1] == 5){
					System.out.println("Error in transmission is acknowlaged");
				}
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
