import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Scanner;


public class Server {
	private final int REQUEST_SIZE = 100;
	DatagramSocket recieveSocket, errorSocket;
	DatagramPacket recievePacket, errorPacket;
	ComFunctions com;
	int mode;
	/**
	 * loops and keeps serving all the incoming requests
	 */
	public void serve() {
		while(true) {
			recievePacket = com.recievePacket(recieveSocket, REQUEST_SIZE); 
			//if the received packet is valid, passes the message onto a worker thread that takes care of all request until it is complete 
			if (com.checkMessage(recievePacket.getData())) {
				if(mode == 1) {
					System.out.println(com.verboseMode("Recieve", recievePacket));
				}
				ServerWorker worker = new ServerWorker(Integer.toString((recievePacket.getPort())), recievePacket,mode);
				worker.start();
			}else {
				System.out.println("Error Packet");
				errorPacket = com.createPacket(com.generateErrMessage(new byte[] {0, 0}, "The Read or write request was of invalid format"),recievePacket.getPort());
				errorSocket = com.startSocket();
				com.sendPacket(errorPacket, errorSocket);
				errorSocket.close();
			}			
		}
	}
	
	/**
	 * Initializes the server at the start
	 */
	public Server() {
		// TODO Auto-generated constructor stub
		Scanner sc = new Scanner(System.in);
		System.out.println("Select Mode : Quiet [0], Verbose [1]");
		mode = sc.nextInt();
		com = new ComFunctions();
		recieveSocket = com.startSocket(69);
		ServerExitListener exit = new ServerExitListener("Exit listener");
		exit.start();
	}

	
	public static void main(String[] args) {
		Server server = new Server();
		server.serve();
	}
}

