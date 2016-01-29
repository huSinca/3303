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
	private DatagramSocket receiveSocket;

	private int threadCount = 0;
	
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
	 * This method will check to ensure that this is either a valid READ request 
	 * or a WRITE request. Also this method will extract the filename and the mode of
	 * the request and place them in the global variables of the server
	 * @param b the byte array received from the intermediate host
	 * @return true if the request is valid
	 */
	public boolean isValid(byte[] b) {
		//Initial checks to see if it is a valid read/write request
		if (b == null || b.length == 0) {
			return false;
		} else if (b[0] != 0) {
			return false;
		} else if (b[1] != 1 && b[1] != 2) {
			return false;
		}

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
			return false;
		}
	}

	/**
	 * This method is used to write from the client to the server
	 * @param receivedPacket the byte array received from the client
	 * @param port the port the packet was received from
	 */
	public void write(byte[] receivedPacket, int port) {
		DatagramSocket errorSimSocket;
		byte block;
		byte[] connection = new byte[4];
		connection[0] = (byte) 0;
		connection[1] = (byte) 4;
		connection[2] = (byte) 0;
		connection[3] = (byte) 0;
		block = 0;
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
				out.write(receiveFile, 4, establishPacket.getLength() - 4);
				DatagramPacket acknowledge = new DatagramPacket(connection, connection.length, 
						InetAddress.getLocalHost(), establishPacket.getPort());
				errorSimSocket.send(acknowledge);
				if (establishPacket.getLength() < 516) break;
				block++;
				connection[3] = block;
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This function is used to read from the server and send back to the client
	 * @param receivedPacket the byte array received from the client
	 * @param port the port number received the packet was received from
	 */
	public void read(byte[] receivedPacket, int port) {
		DatagramSocket errorSimSocket;
		byte block;
		byte[] receive = new byte[4];
		DatagramPacket received = new DatagramPacket(receive, receive.length);
		block = 1;
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
				errorSimSocket.send(fileTransfer);
				errorSimSocket.receive(received);
				block++;
			}
			input.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * This method is used to run the Server
	 * @throws Exception when an invalid request is received
	 */
	public void runServer() throws Exception {
		byte[] b = new byte[100];
		Stack <Integer> activeThreads = new Stack<Integer>();
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
					else if (shutdown && threadCount == 0) { // wait till all active threads finish & shutdown requested
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
				int port = receival.getPort();
				if (isValid(b)) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							activeThreads.push(1);
							threadCount++;
							if (b[1] == 1) {
								read(b, port);
							} else {
								write(b, port);
							}
							threadCount--;
							activeThreads.pop();
						}
					}).start();
				} else {
					throw new Exception("Invalid Request");
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
