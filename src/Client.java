import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Client {

	/**
	 * The DatagramSocket that the client will use to send and receive
	 */
	private DatagramSocket sendReceive;

	public Client() {
		try {
			sendReceive = new DatagramSocket();
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * This method will print the byte array and also print it as a string
	 * @param array the array that needs to be printed
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
	 * This method will create the request that the server will send to the intermediate host
	 * @param requestType The type of request being set (Write, Read, Invalid). Default is Read
	 * @param fileName the name of the file
	 * @param mode the mode being used
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
		if (requestType.equals("Write")) b[1] = 2;
		System.arraycopy(fileName.getBytes(), 0, b, 2, fileName.length());
		b[fileName.length() + 2] = 0;
		System.arraycopy(mode.getBytes(), 0, b, fileName.length() + 3, mode.length());
		b[fileName.length() + mode.length() + 3] = 0;
		return b;
	}

	/**
	 * This method is used to run the Client
	 */
	public void runClient() {
		System.out.println("--------------------------------------");
		for (int i = 0; i < 11; i++) {
			String file = "assign1.txt";
			String mode = "ocTEt";
			byte[] request;
			if (i == 10) {
				System.out.println("Sending Invalid Request");
				request = createRequest("Invalid", file, mode);
			} else {
				if (i % 2 == 0) {
					System.out.println("Read Request");
					request = createRequest("Read", file, mode);
				} else {
					System.out.println("Write Request");
					request = createRequest("Write", file, mode);
				}
			}

			System.out.println("Sending following data to port 68: ");

			DatagramPacket p;
			try {
				//send to host
				p = new DatagramPacket(request, request.length, InetAddress.getLocalHost(), 68);
				printByteArray(request, request.length);
				sendReceive.send(p);

				//Receive from host
				byte[] receive = new byte[4];
				DatagramPacket received = new DatagramPacket(receive, receive.length);
				sendReceive.receive(received);
				System.out.println("Received the following from host:");
				printByteArray(receive, received.getLength());
				System.out.println("--------------------------------------");
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}

	public static void main( String args[] ) {
		Client c = new Client();
		c.runClient();
	}

}
