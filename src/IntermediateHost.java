import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Scanner;

public class IntermediateHost {
	
	DatagramSocket sendRecieveSocket;
	DatagramPacket recievePacket, sendPacket;
	ComFunctions com;
	workerThread externalThread;
	int serverPort = 69;
	int clientPort;
	int mode;
	private int interHostPort = 23;
	
	private static int packetNumber;
	private static int packetDelay;
	private static int simulation; // 0 = no errors, 1 = lose data packet, 2 = delay packet, 3 = duplicate packet
	private static int dup;
	private int packetCounter = 1;
	
	/**
	 * Waits to receive a message from the client and passes that on to the server
	 */
	public void recieveMessage(){
		int packet = 0;
		int tempPort = 0;
		boolean serverNotSet = true;
		boolean clientNotSet = true ;
		//while(true) {
			switch (simulation) {
				case 0: 
					while(true) {
					//Passes the packet between the client to server and vice versa
					recievePacket = com.recievePacket(sendRecieveSocket, 516);
					tempPort = recievePacket.getPort();
					if((packet == 0) && clientNotSet) { //if the client TID has not been set, do so
						clientNotSet = false;
						clientPort = tempPort;
					}else if(!(tempPort == clientPort)&&serverNotSet) { //if the received Packet is not from the client and server has yet be set, set the server TID
						serverNotSet  = false;
						serverPort = tempPort;
					}

					if(tempPort == clientPort) {
						if(mode == 1) {
							System.out.println(com.verboseMode("Recieve from client", recievePacket));
						}
					}else if(tempPort ==  serverPort) {
						if(mode == 1) {
							System.out.println(com.verboseMode("Recieve from server", recievePacket));
						}
					}else { //If the received packed it from an unexpected TID, create a separate port to send the packet to the client through, this is the simulate the TID error on the client side
						intermediateHostRandomPort rando = new intermediateHostRandomPort(recievePacket, clientPort, sendRecieveSocket);
						rando.start();
					}
					if(tempPort  ==clientPort || tempPort  == serverPort) { //If the packet received was from an exptected TID, continue transfer as normal else allow the IntermediateHostRandomPort handle the rest
						packet ++;
						if(tempPort == clientPort) {
							sendPacket = com.createPacket(recievePacket, serverPort);
							if(mode == 1) {
								System.out.println(com.verboseMode("Send to Server", recievePacket));
							}
						}else if(tempPort == serverPort) {
							sendPacket = com.createPacket(recievePacket, clientPort);
							if(mode == 1) {
								System.out.println(com.verboseMode("Send to Client", recievePacket));
							}
						}


						com.sendPacket(sendPacket, sendRecieveSocket);
					}
					}
				
				case 1:
					while(true) {
						//Passes the packet between the client to server and vice versa
						recievePacket = com.recievePacket(sendRecieveSocket, 516);
						tempPort = recievePacket.getPort();
						
						if((packet == 0) && clientNotSet) {
							clientNotSet = false;
							clientPort = tempPort;
						}else if(!(tempPort == clientPort)&&serverNotSet) {
							serverNotSet  = false;
							serverPort = tempPort;
						}
						
						if(tempPort == clientPort) {
							if(mode == 1) {
								System.out.println(com.verboseMode("Recieve from client", recievePacket));
							}
						}else if(tempPort ==  serverPort) {
							if(mode == 1) {
								System.out.println(com.verboseMode("Recieve from server", recievePacket));
							}
						}else {
							intermediateHostRandomPort rando = new intermediateHostRandomPort(recievePacket, clientPort, sendRecieveSocket);
							rando.start();
						}
						
						packet ++;
						if(tempPort == clientPort) {
							sendPacket = com.createPacket(recievePacket, serverPort);
							
						}else if(tempPort == serverPort) {
							sendPacket = com.createPacket(recievePacket, clientPort);
							
						}
					
						if (!(packetCounter == packetNumber)) {
							
							com.sendPacket(sendPacket, sendRecieveSocket);
							packetCounter++;
							if((mode == 1)&& (tempPort == clientPort)) {
								System.out.println(com.verboseMode("Send to Server", recievePacket));
							}
							
							if((mode == 1) && (tempPort == serverPort)) {
								System.out.println(com.verboseMode("Send to Client", recievePacket));
							}
						}else { //if we the packetCount has reached to the same value as the packetNumer we want to lose, the error simulator doesn't do anything with the packet
							System.out.println("Simulating Lost Packet...");
							packetCounter++;
						}
					}
				case 2:
					while(true) {
						//Recieving a message to from the client, prints the message, created a new packet to send to the server, prints that message for clarification and sends it the server
						recievePacket = com.recievePacket(sendRecieveSocket, 516);
						tempPort = recievePacket.getPort();
						
						if((packet == 0) && clientNotSet) {
							clientNotSet = false;
							clientPort = tempPort;
						}else if(!(tempPort == clientPort)&&serverNotSet) {
							serverNotSet  = false;
							serverPort = tempPort;
						}
						
						if(tempPort == clientPort) {
							if(mode == 1) {
								System.out.println(com.verboseMode("Recieve from client", recievePacket));
							}
						}else if(tempPort ==  serverPort) {
							if(mode == 1) {
								System.out.println(com.verboseMode("Recieve from server", recievePacket));
							}
						}else {
							intermediateHostRandomPort rando = new intermediateHostRandomPort(recievePacket, clientPort, sendRecieveSocket);
							rando.start();
						}
						
						if(tempPort  ==clientPort || tempPort  == serverPort) {
							packet ++;
							if(tempPort == clientPort) {
								sendPacket = com.createPacket(recievePacket, serverPort);
							}else if(tempPort == serverPort) {
								sendPacket = com.createPacket(recievePacket, clientPort);
							}
							if(packetCounter != packetNumber) {
								com.sendPacket(sendPacket, sendRecieveSocket);
								packetCounter++;
								if((mode == 1)&& (tempPort == clientPort)) {
									System.out.println(com.verboseMode("Send to Server", recievePacket));
								}
								if((mode == 1) && (tempPort == serverPort)) {
									System.out.println(com.verboseMode("Send to Client", recievePacket));
								}
							}else{
								//if we the packetCount has reached to the same value as the packetNumer we want to lose, the error simulator Starts a deyalSimulator that sends the packet after a specified period of time
								packetCounter++;
								System.out.println("Delaying packet...");
								delaySimulator delay  = new delaySimulator(recievePacket, (long)packetDelay);
								delay.start();
							}
						}
						
						
					}
				case 3:
					while(true) { 
						//Receiving a message to from the client, prints the message, created a new packet to send to the server, prints that message for clarification and sends it the server
						//Same logic as case 0 except duplicates packet as needed1
						recievePacket = com.recievePacket(sendRecieveSocket, 516);
						tempPort = recievePacket.getPort();
						
						if((packet == 0) && clientNotSet) {
							clientNotSet = false;
							clientPort = tempPort;
						}else if(!(tempPort == clientPort)&&serverNotSet) {
							serverNotSet  = false;
							serverPort = tempPort;
						}
						
						if(tempPort == clientPort) {
							if(mode == 1) {
								System.out.println(com.verboseMode("Recieve from client", recievePacket));
							}
						}else if(tempPort ==  serverPort) {
							if(mode == 1) {
								System.out.println(com.verboseMode("Recieve from server", recievePacket));
							}
						}else {
							intermediateHostRandomPort rando = new intermediateHostRandomPort(recievePacket, clientPort, sendRecieveSocket);
							rando.start();
						}
						
						if(tempPort  ==clientPort || tempPort  == serverPort) {
							packet ++;
							if(tempPort == clientPort) {
								sendPacket = com.createPacket(recievePacket, serverPort);
							}else if(tempPort == serverPort) {
								sendPacket = com.createPacket(recievePacket, clientPort);
							}
			
							if(packetCounter != packetNumber) {
								com.sendPacket(sendPacket, sendRecieveSocket);
								packetCounter++;
								if((mode == 1)&& (tempPort == clientPort)) {
									System.out.println(com.verboseMode("Send to Server", recievePacket));
								}
								if((mode == 1) && (tempPort == serverPort)) {
									System.out.println(com.verboseMode("Send to Client", recievePacket));
								}
							}else {
							//if we the packetCount has reached to the same value as the packetNumer we want to lose, the error simulator duplicates the packet dup number of times
								for(int i = 0; i< dup; i ++) {
									com.sendPacket(sendPacket, sendRecieveSocket);
									if((mode == 1)&& (tempPort == clientPort)) {
									System.out.println(com.verboseMode("Duplicate send to Server", recievePacket));
									}
								
									if((mode == 1) && (tempPort == serverPort)) {
										System.out.println(com.verboseMode("Duplicate Send to Client", recievePacket));
									}
								}
								packetCounter++;
							}
						}
					}
		}
	}
	
	public IntermediateHost() {
		// TODO Auto-generated constructor stub
		//Getting user input on what error needs to be simulated
		Scanner sc1 = new Scanner(System.in);
		System.out.println("Select Mode : Quiet [0], Verbose [1]");
		mode = sc1.nextInt();
		
		System.out.println("Select Mode : Normal [0], Lost Packet [1], Delayed Packet [2], Duplicate Packet [3]");
		simulation = sc1.nextInt();
		
		
		if(simulation != 0) {
			System.out.println("Which packet would you like to simulate the error");
			packetNumber = sc1.nextInt();
			
			
			if(simulation == 2) {
				System.out.println("After how many milliseconds would you like to send the delayed one?");
				packetDelay = sc1.nextInt();
				
			} else if (simulation == 3) {
				System.out.println("How many times would you like to duplicate this packet?");
				dup = sc1.nextInt();
				
			}
		}
		sc1.close();
		com = new ComFunctions();
		sendRecieveSocket = com.startSocket(interHostPort);
		
	}
	
	public static void main(String[] args) {		
		IntermediateHost host = new IntermediateHost();
		host.recieveMessage();
	}
}
