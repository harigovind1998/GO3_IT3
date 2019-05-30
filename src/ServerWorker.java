import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.Paths;
import java.util.Arrays;

public class ServerWorker extends Thread {
	
	private final int BLOCK_SIZE = 516;
	private DatagramPacket initialPacket, RecievedResponse, SendingResponse;
	private int interHostPort;
	private String fileName;
	private DatagramSocket SendRecieveSocket; 
	private ComFunctions com;
	private int job, mode;

	/**
	 * Gets the name of the file that is being written into or read from
	 */
	private void getFileName() {
		byte[] data = initialPacket.getData();
		int[] secondZero = {3,0,0};
		int track = 1;
		for(int i = 3; i<data.length ; i ++) {
			if(data[i] == 0) {
				secondZero[track] = i;
				track++;
				if (track == 3) {
					break;
				}
			}
		}
		byte[] file = Arrays.copyOfRange(data, 2 , secondZero[1]);
		this.fileName = new String(file);

	}
	
	/**
	 * Decodes the incoming packet to get the necessary information, namely the file name and weather the its a read or write request
	 */
	private void decodePacket() {
		job = initialPacket.getData()[1]; //format of the message has been checked so second bit will determine if the request is a read or write
		interHostPort = initialPacket.getPort();
		getFileName();
	}
	
	
	/**
	 * Sends the contents over to the client
	 */
	private void readServe() {
		
		byte [] fileByteReadArray = com.readFileIntoArray("./Server/" + fileName);
		int blockNum = 1;
		//Keeps looping until it is the entire file has been sent over
		mainLoop:
		while(true){
			byte[] msg = com.generateDataPacket(com.intToByte(blockNum), com.getBlock(blockNum, fileByteReadArray));
			RecievedResponse = com.createPacket(100);
			SendingResponse = com.createPacket(msg, interHostPort);
			//Loop that is in charge of sending/resending the data packet
			outterSend:
				while(true) {
					com.sendPacket(SendingResponse, SendRecieveSocket); //Send message
					if(mode == 1) {
						System.out.println(com.verboseMode("Sent Packet:", SendingResponse));
					}
					try {
						//Loop that ensures that the incoming AckPackets are correct, if it isn't, it will loop back to listening until the correct Ack is recieved or the the socket receive times out
						innerSend:
							while(true) {
								SendRecieveSocket.receive(RecievedResponse);
								if(mode == 1) {
									System.out.println(com.verboseMode("Recieved Packet:", RecievedResponse));
								}
								if(com.CheckAck(RecievedResponse, blockNum)) {
									break innerSend;
								}else {
									System.out.println("Wrong block recieved, continue waiting...");
								}
							}
						break outterSend; //DataPacket sent and the right AckReceived hence continues on to the next packet
					} catch (IOException e) {
						if(mode == 1) {
							System.out.println(com.verboseMode("Preparing to resend packet:", SendingResponse));
						}
					}
				}
			if(SendingResponse.getData()[SendingResponse.getLength() -1] == 0){ //Checks to see if the file has come to an end
				System.out.println("End of file reached");
				break mainLoop;
			}
			blockNum ++ ;
		}
	}
	
	/**
	 * Handles the write request
	 */
	private void writeServe(){
		File yourFile = new File("./Server/" + fileName);
		//If the specified file doesn't exit, it will create it
		try {
			yourFile.createNewFile();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		byte[] ackMsg = null;
		int blockNum = 0;
		byte[] incomingBlock = new byte[2];
		int last;
		RecievedResponse = com.createPacket(BLOCK_SIZE);
		
		SendingResponse = com.createPacket(com.generateAckMessage(com.intToByte(blockNum)), interHostPort);
		
		blockNum++;
		
		writeLoop:
		while (true) {//Loop to write the entire file
			mainLoop:
				while(true) { //Loop that sends and resends AckPackets accordingly
					com.sendPacket(SendingResponse, SendRecieveSocket);
					if(mode == 1) {
						System.out.println(com.verboseMode("Sent Packet:", SendingResponse));
					}
					try {
						innerLoop:
							while(true) { //Loop that listens for the incoming packet, if the packet is incorrect it keeps listing until the correct one is received 
								SendRecieveSocket.receive(RecievedResponse);
								if(mode == 1) {
									System.out.println(com.verboseMode("Recieved Packet:", RecievedResponse));
								}
								//Checks to see if the Data Packet received is the correct packet, if it isn't waits for next incoming packet
								incomingBlock[0] = RecievedResponse.getData()[2];
								incomingBlock[1] = RecievedResponse.getData()[3];
								if( (blockNum == ByteBuffer.wrap(incomingBlock).getShort())) {
									com.writeArrayIntoFile(com.parseBlockData(RecievedResponse.getData()), Paths.get("./Server/" + fileName));
									last = RecievedResponse.getData()[RecievedResponse.getLength() -1];
									ackMsg = com.generateAckMessage(com.intToByte(blockNum));
									SendingResponse = com.createPacket(ackMsg, interHostPort);
									if(last == 0){ //Checks for if the Data Packet is the last packet
										com.sendPacket(SendingResponse, SendRecieveSocket);
										if(mode == 1) {
											System.out.println(com.verboseMode("Sent", SendingResponse));
										}
										System.out.println("End of file reached");
										break writeLoop; //End of file receive so breaks out of all loops
									}
									break innerLoop; //The correct data Packet was received so it leaves the inner loop
								
								}else {
									System.out.println("Wrong data packet recieved");
								}
						
							}
						break mainLoop; //block was written in to file so the server can send the ack packet and start listening for n+1 packet
					} catch (Exception e) {
						// TODO: handle exception
						//Receive timed out
						if(mode == 1) {
							System.out.println(com.verboseMode("Preparing to resend packet:", SendingResponse));
						}
					}
				}
			
			++blockNum;
		}
	}
	
	/**
	 * decodes and then performs the necessary task
	 */
	public void run() {
		decodePacket();
		if(job == 1) {
			readServe();
		}else if (job ==2) {
			writeServe();
		}
	}
	
	public ServerWorker(String name, DatagramPacket packet, int mode) {
		// TODO Auto-generated constructor stub
		super(name);
		com = new ComFunctions();
		SendRecieveSocket = com.startSocket();
		try {
			SendRecieveSocket.setSoTimeout(1000);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		initialPacket = packet;
		this.mode = mode;
	}
	
}
