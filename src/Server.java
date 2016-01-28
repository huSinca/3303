import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Server {

	/**
	 * The name of the file received from the request from the intermediate host
	 */
	private String receivedFileName;

	/**
	 * The name of the mode received from the request from the intermediate host
	 */
	private String receivedMode;

	/**
	 * The socket for the server which will be set to use port 69
	 */
	private DatagramSocket receiveSocket;

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

			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("out.txt"));
			while(true) {
				byte[] receiveFile = new byte[512];
				establishPacket = new DatagramPacket(receiveFile, receiveFile.length);
				errorSimSocket.receive(establishPacket);
				if (establishPacket.getLength() == 0) break;
				System.out.println("Write");
				out.write(receiveFile, 0, 512);
				block++;
				connection[3] = block;
				DatagramPacket acknowledge = new DatagramPacket(connection, connection.length, 
						InetAddress.getLocalHost(), establishPacket.getPort());
				errorSimSocket.send(acknowledge);
			}
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void read(byte[] receivedPacket, int port) {
		DatagramSocket errorSimSocket;
		byte[] b = receivedPacket;
		byte block;
		byte[] connection = new byte[4];
		connection[0] = (byte) 0;
		connection[1] = (byte) 3;
		connection[2] = (byte) 0;
		connection[3] = (byte) 1;
		block = 1;
	}

	/**
	 * This method is used to run the Server
	 * @throws Exception when an invalid request is received
	 */
	public void runServer() throws Exception {
		byte[] b = new byte[100];
		DatagramPacket receival = new DatagramPacket(b, b.length);
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
							if (b[1] == 1) {
								read(b, port);
							} else {
								write(b, port);
							}
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
