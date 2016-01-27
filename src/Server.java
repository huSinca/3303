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
				System.out.println("Received the following from host: ");
				Client.printByteArray(b, receival.getLength());
				byte[] response = new byte[4];
				if (isValid(b)) {
					if (b[1] == 1) {	//Read Response
						response[0] = (byte) 0;
						response[1] = (byte) 3;
						response[2] = (byte) 0;
						response[3] = (byte) 1;
					} else {			//Write Response
						response[0] = (byte) 0;
						response[1] = (byte) 4;
						response[2] = (byte) 0;
						response[3] = (byte) 0;
					}
				} else {
					throw new Exception("Invalid Request");
				}

				//Sending response to host
				DatagramPacket sendHostPacket = new DatagramPacket(response, response.length, InetAddress.getLocalHost(), receival.getPort());
				System.out.println("Sending the following response to Host: ");
				Client.printByteArray(response, response.length);
				DatagramSocket tempSocket = new DatagramSocket();
				tempSocket.send(sendHostPacket);
				tempSocket.close();
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
