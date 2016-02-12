import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringBufferInputStream;
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
	private DatagramSocket receiveSocket;
	
	private Stack<Integer> activeThreads = new Stack<Integer>();
	
	/**
	 *  Shutdown flag to show if shutdown has been requested
	 */
	private boolean shutdown;

	public Server() {
		try {
			receiveSocket = new DatagramSocket(69);
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
	 * @return true if the request is valid
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
		case (byte) 3: case (byte) 4:
			return (b[3] == block);
		default: 
			System.out.println("Invalid opcode!");
			error((byte)4, port);
			return false;
		}
	}

	public void write(byte[] receivedPacket, int port) {
		DatagramSocket errorSimSocket;
		byte block;
		byte[] connection = new byte[4];
		connection[0] = (byte) 0;
		connection[1] = (byte) 4;
		connection[2] = (byte) 0;
		connection[3] = (byte) 0;
		block = (byte) 0;
		try {
			DatagramPacket establishPacket = new DatagramPacket(connection, connection.length, 
					InetAddress.getLocalHost(), port);
			errorSimSocket = new DatagramSocket();
			errorSimSocket.send(establishPacket);

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("write.txt"));
			while(true) {
				byte[] receiveFile = new byte[516];
				establishPacket = new DatagramPacket(receiveFile, receiveFile.length);
				errorSimSocket.receive(establishPacket);
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
					errorSimSocket.send(acknowledge);
					if (establishPacket.getLength() < 516) break;
					block++;
					connection[3] = block;
				}
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void read(byte[] receivedPacket, int port) {
		DatagramSocket errorSimSocket;
		byte block;
		byte[] receive = new byte[4];
		DatagramPacket received = new DatagramPacket(receive, receive.length);
		block = (byte) 1;
		try {
			errorSimSocket = new DatagramSocket();
			byte[] sendingData = new byte[512];
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(receivedFileName));
			int x;
			while ((x = input.read(sendingData)) != -1) {
				byte[] connection = new byte[516];
				connection[0] = (byte) 0;
				connection[1] = (byte) 3;
				connection[2] = (byte) 0;
				connection[3] = block;
				System.arraycopy(sendingData, 0, connection, 4, sendingData.length);
				DatagramPacket fileTransfer = new DatagramPacket(connection, x + 4, InetAddress.getLocalHost(), port);
				System.out.println("Sending ACK packet.");
				errorSimSocket.send(fileTransfer);
				received = receivePacket(errorSimSocket, received, port);
				block++;
			}
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void error(byte ErrorCode, int port){
		DatagramSocket errorSimSocket;
		byte[] receive = new byte[4];
		DatagramPacket received = new DatagramPacket(receive, receive.length);
		try {
			errorSimSocket = new DatagramSocket();
			byte[] connection = new byte[516];
			connection[0] = (byte) 0;
			connection[1] = (byte) 5;
			connection[2] = (byte) 0;
			connection[3] = (byte) ErrorCode;
			DatagramPacket ErrorMessage = new DatagramPacket(connection, 516, InetAddress.getLocalHost(), port);
			System.out.println("Sending ERROR packet.");
			errorSimSocket.send(ErrorMessage);
			received = receivePacket(errorSimSocket, received, port);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Receives a packet from an intended sender specified by port. If the received packet is not from the
	 * intended sender, the Server will respond to that sender with an ERROR packet, and then continue waiting for
	 * a packet from the intended sender.
	 * 
	 * @param receiveSocket socket to receive from
	 * @param receivePacket packet received
	 * @param port port number of the intended sender
	 * @return the received DatagramPacket
	 * @throws IOException
	 */
	private DatagramPacket receivePacket(DatagramSocket receiveSocket, DatagramPacket receivePacket, int port) throws IOException
	{
		receiveSocket.receive(receivePacket);
		System.out.println("Received a packet!");
		System.out.println("Expected TID: " + port);
		System.out.println("Received TID: " + receivePacket.getPort());
		//Ensure received packet came from the intended sender
		while (receivePacket.getPort() != port) {
			error((byte) 5, receivePacket.getPort());
			receiveSocket.receive(receivePacket);
			System.out.println("Received a packet.");
		}
		return receivePacket;
	}
	
	/**
	 * This method is used to run the Server
	 * @throws Exception when an invalid request is received
	 */
	public void runServer() throws Exception {
		byte[] b = new byte[100];
		DatagramPacket receival = new DatagramPacket(b, b.length);
		
		/**
		 * Thread created to launch prompt for server shutdown.
		 * Thread is required as server could become blocked
		 * with the receiveSocket.receive(receival) call as it will
		 * wait to receive rather than quit. The thread ensures
		 * that as long as no requests come in or no requests
		 * are currently running, it will quit override and
		 * quit.
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
				receiveSocket.receive(receival);
				System.out.println("Received a packet.");
				int port = receival.getPort();
				if (isValid(b, (byte) 0, port)) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							System.out.println("New thread created.");
							activeThreads.push(0);
							if (b[1] == 1) {
								read(b, port);
								System.out.println("Read request recieved");
							} else if (b[1] == 2) {
								write(b, port);
								System.out.println("Write request recieved");
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
