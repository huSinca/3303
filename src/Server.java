import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Stack;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

public class Server {
	
	/**
	 * The name of the file received from the request from the intermediate host
	 */
	private String receivedFileName;

	/**
	 *  The name of the mode received from the request from the intermediate host
	 */
	private String receivedMode;

	/**
	 * The socket for the server which will be set to use port 69
	 */
	private DatagramSocket transferSocket;
	
	private Stack<Integer> activeThreads = new Stack<Integer>();
	
	/**
	 *  Shutdown flag to show if shutdown has been requested
	 */
	private boolean shutdown;

	public Server() {
		try {
			transferSocket = new DatagramSocket(69);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method will check to ensure that the given packet data is a valid TFTP packet. This method will
	 * also extract the filename and the mode of a read or write request and place them in the global
	 * variables of the server.
	 * @param b the data of the received packet in the form of a byte array
	 * @param block the block number specified by a DATA or ACK packet. Value negligible for WRQ or RRQ
	 * @param port the port number of the intended sender. Used to determine unknown transfer ID errors
	 * @return true if the packet is valid
	 */
	public boolean isValid(byte[] b, byte block, int port) {
		//Initial checks to see if it is a valid packet
		if (b == null || b.length == 0) {
			System.out.println("No packet data!");
			return false;
		} else if (b[0] != 0) {
			System.out.println("Invalid opcode, does not start with 0!");
			return false;
		}
		System.out.println("Packet opcode: " + b[1]);
		switch (b[1]) {
		//If the packet is a WRQ or RRQ
		case (byte) 1: case (byte) 2:
			//Get the filename from the byte array
			StringBuilder builder = new StringBuilder();
			int index;
			for (index = 2; index < b.length; index++) {
				if(b[index] != 0) {
					builder.append((char) b[index]);
				} else {
					receivedFileName = builder.toString();
					break;
				}
			}
			//Get the mode from the byte array
			builder = new StringBuilder();
			for (int i = index+1; i < b.length; i++) {
				if(b[i] != 0) {
					builder.append((char) b[i]);
				} else {
					receivedMode = builder.toString();
					break;
				}
			}
			if (receivedMode != null && receivedFileName != null) {
				return true;
			} else {
				System.out.println("Null file name or mode!");
				return false;
			}
		//If the packet is a DATA or ACK
		case (byte) 3: case (byte) 4:
			return (b[3] == block);
		//If the packet is an ERROR
		case (byte) 5:
			System.out.println("ERROR packet acknowledged.");
			return true;
		default: 
			System.out.println("Invalid opcode!");
			return false;
		}
	}

	/**
	 * Handles write operations.
	 * @param receivedPacket data held in the write request
	 * @param port port number from which the write request came
	 */
	public void write(byte[] receivedPacket, int port) {
		DatagramSocket transferSocket;	//Socket through which the transfer is done
		byte block;	//The current block of data being transferred
		//Create data for an ACK packet
		byte[] connection = new byte[4];
		connection[0] = (byte) 0;
		connection[1] = (byte) 4;
		connection[2] = (byte) 0;
		connection[3] = (byte) 0;
		block = (byte) 0;
		try {
			DatagramPacket establishPacket = new DatagramPacket(connection, connection.length, 
					InetAddress.getLocalHost(), port);
			transferSocket = new DatagramSocket();
			//Send ACK packet
			transferSocket.send(establishPacket);

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("write.txt"));
			while(true) {
				byte[] receiveFile = new byte[516];
				establishPacket = new DatagramPacket(receiveFile, receiveFile.length);
				transferSocket.receive(establishPacket);
				System.out.println("Received a packet.");
				if (!isValid(establishPacket.getData(), block, port)) {
					System.out.println("Packet recieved from host has an invalid Opcode, reporting back to Host");
					error((byte)4, port);
				}
				else {
					out.write(receiveFile, 4, establishPacket.getLength() - 4);
					DatagramPacket acknowledge = new DatagramPacket(connection, connection.length, 
							InetAddress.getLocalHost(), establishPacket.getPort());
					System.out.println("Sending ACK packet.");
					transferSocket.send(acknowledge);
					//A packet of less than max size indicates the end of the transfer
					if (establishPacket.getLength() < 516) break;
					block++;
					connection[3] = block;	//Update block field in the packet
				}
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Handles read operations
	 * @param receivedPacket data held in the read request
	 * @param portport number from which the read request came
	 */
	public void read(byte[] receivedPacket, int port) {
		DatagramSocket transferSocket;	//Socket through which the transfer is done
		byte block;	//The current block of data being transferred
		byte[] receive = new byte[4];	//Buffer for incoming packets
		DatagramPacket received = new DatagramPacket(receive, receive.length);	//Holds incoming packets
		block = (byte) 1;
		try {
			transferSocket = new DatagramSocket();
			byte[] sendingData = new byte[512];
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(receivedFileName));
			int x;	//End of stream indicator
			while ((x = input.read(sendingData)) != -1) {
				byte[] connection = new byte[516];	//Buffer for outgoing packets
				connection[0] = (byte) 0;
				connection[1] = (byte) 3;
				connection[2] = (byte) 0;
				connection[3] = block;
				System.arraycopy(sendingData, 0, connection, 4, sendingData.length);
				DatagramPacket fileTransfer = new DatagramPacket(connection, x + 4, InetAddress.getLocalHost(), port);
				do {
					System.out.println("Sending DATA packet.");
					transferSocket.send(fileTransfer);
					received = receivePacket(transferSocket, received, port);
				} while (received.getData()[1] == (byte) 5);	//Re-send if an ERROR is received
				block++;
			}
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Sends an ERROR packet.
	 * @param ErrorCode TFTP error code of the ERROR packet
	 * @param port port to send the packet through
	 */
	public void error(byte ErrorCode, int port){
		DatagramSocket transferSocket;	//Socket to be sent through
		try {
			transferSocket = new DatagramSocket();
			byte[] connection = new byte[516];
			connection[0] = (byte) 0;
			connection[1] = (byte) 5;
			connection[2] = (byte) 0;
			connection[3] = (byte) ErrorCode;
			DatagramPacket ErrorMessage = new DatagramPacket(connection, 516, InetAddress.getLocalHost(), port);
			System.out.println("Sending ERROR(From Server) packet.");
			transferSocket.send(ErrorMessage);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Receives a packet from an intended sender specified by port. If the received packet is invalid (ie it is not
	 * from the intended sender, or it has an invalid opcode), the program will respond by sending an appropriate
	 * ERROR packet. It then continues waiting for a packet.
	 * 
	 * @param transferSocket socket to receive from
	 * @param receivePacket packet received
	 * @param port port number of the intended sender
	 * @return the received DatagramPacket
	 * @throws IOException
	 */
	private DatagramPacket receivePacket(DatagramSocket transferSocket, DatagramPacket receivePacket, int port)
	{
		boolean error = true;	//Indicates whether or not an invalid packet has been received and the server
								//must continue waiting
		try {
			while(error) {
				error = false;
				System.out.println("Waiting for a packet...");
				transferSocket.receive(receivePacket);
				System.out.println("Received a packet!");
				System.out.println("Expected TID: " + port);
				System.out.println("Received TID: " + receivePacket.getPort());
				//Ensure received packet is a valid TFTP operation
				if (!isValid(receivePacket.getData(), receivePacket.getData()[3], port)) {
					System.out.println("Error, illegal TFTP operation!");
					error((byte) 4, receivePacket.getPort());
					error = true;
				}
				//Ensure received packet came from the intended sender
				if (receivePacket.getPort() != port) {
					System.out.println("Error, unknown transfer ID");
					error((byte) 5, receivePacket.getPort());
					error = true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return receivePacket;
	}
	
	/**
	 * This method is used to run the Server
	 */
	public void runServer()
	{
		byte[] b = new byte[100];
		DatagramPacket receival = new DatagramPacket(b, b.length);
		
		/**
		 * Thread created to launch prompt for server shutdown.
		 * Thread is required as server could become blocked
		 * with the transferSocket.receive(receival) call as it will
		 * wait to receive rather than quit. The thread ensures
		 * that as long as no requests come in or no requests
		 * are currently running, it will override and quit.
		 */
		new Thread(new Runnable() {
			@Override
			public void run() {
				JLabel modeLabel = new JLabel("Shutdown Server?");
				int close;
				while (true) { // run till shutdown requested
					if(!shutdown) {
						close = JOptionPane.showConfirmDialog(null, modeLabel, "Warning", JOptionPane.CLOSED_OPTION);
						if (close == 0) { // ok has been selected, set shutdown to true
							shutdown = true;
						}
					}
					else if (shutdown && activeThreads.isEmpty()) { // wait till all active threads finish & shutdown requested
						System.out.println("Shutting server down");
						System.exit(0);
					}
				}
			}
		}).start();
		try {
			while (true) {
				System.out.println("---------------------------------");
				System.out.println("Waiting to Receive from Host");
				
				//Server receives a read or write request
				transferSocket.receive(receival);
				System.out.println("Received a packet.");
				int port = receival.getPort();
				if (isValid(b, (byte) 0, port)) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							System.out.println("New thread created.");
							activeThreads.push(0);
							if (b[1] == 1) {
								System.out.println("Read request recieved");
								read(b, port);
							} else if (b[1] == 2) {
								System.out.println("Write request recieved");
								write(b, port);
							} else {
								System.out.println("ERR");
							}
							activeThreads.pop();
							System.out.println("A thread has completed execution.");
						}
					}).start();
				} else {
					System.out.println("Not valid");
					//do nothing;
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main( String args[] ) {
		Server s = new Server();
		try {
			s.runServer();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
