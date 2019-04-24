import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Vector;
public class PeerClient extends UnicastRemoteObject implements PeerClientIF, Runnable 
{ 
	private static final long serialVersionUID = 1L;
	private PeerServerIF peerServer;	//the object of the server side of this peer client 
	private String[][] neighPeerServers; //array holding ip & port# of all neighboring peer-servers
	private ArrayList<String[]> msgHits = new ArrayList<String[]>();	//records meta data on message hits
	private ArrayList<Long> queryhittime = new ArrayList<Long>();
	private String peerName = null;	//name of peer
	private String peer_ip; //ip address of peer
	private String port_no;	//port number of peer	
	private int msgIDsuffix = 1;	//used to assign a unique ID to each message
	final static long TIME_TO_LIVE = 9;	//sets the maximum message hops per query
	final static long QUERY_WAIT_TIME = 600;	//in milliseconds, sets time client waits for query hits
	private long sum=0;
	
	protected PeerClient() throws RemoteException 
        {
		super();
	}
	public PeerClient(String name,String ip_address, String  port_no, int numofneighboringpeers, PeerServerIF inst) throws RemoteException 
        {
		this.peerName = name;
		this.peer_ip = ip_address;
		this.port_no = port_no;
		this.peerServer = inst; 	//gets object of its own server
		neighPeerServers = new String[numofneighboringpeers][2];
		peerServer.setClientInstance(this);	//sends instance of the client to the server
		if(peerServer.getPullprotocol())
			new Thread(new FileLazyPull(this, peerServer, peerServer.getTTR())).start();	
	}
	
	//getters for global variables
	public String getName() {
		return peerName;
	}
	public String[][] getNeighPeerServers() {
		return neighPeerServers;
	}
	public String getport_no() {
		return port_no;
	}
	public String getpeer_ip() {
		return peer_ip;
	}
	public static long getTTL() {
		return TIME_TO_LIVE;
	}
	//adds credentials of neighboring peer to array
	public void addNeighboringPeer(String nPIP, String nPPN, int index) {
		neighPeerServers[index][0] = nPIP;
		neighPeerServers[index][1] = nPPN;
	}
	public void addMsgHits(String mssgID, String hitIP, String hitPNum, String hitPName) {
		String[] msgHitMetadata = {mssgID, hitIP, hitPNum, hitPName};
		msgHits.add(msgHitMetadata);
		//System.out.println("Adding time of queryhit");
		queryhittime.add(System.currentTimeMillis());
	}
	public synchronized void downloadFile(PeerServerIF peerWithFile, String filename) throws RemoteException {
		//request file directly from Peer
		try {
			if(peerWithFile.sendFile(this, filename)){
				System.out.println("   File has been downloaded");
			} else {
				System.out.println("Fault: File was NOT downloaded");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}		
	}
	public synchronized boolean findFile(String filename) throws InterruptedException 
        {
		try {
			System.out.println("   Query message sent, waiting for response from network...");
			//start the series of query from its server
			//returns an array of peers with file
			msgIDsuffix++;
			peerServer.query(peer_ip+"-"+port_no+"-"+msgIDsuffix , peer_ip, port_no, TIME_TO_LIVE, filename, peerName);
			Thread.sleep(QUERY_WAIT_TIME);	//pause and wait for response to be propagated back [throws InterruptedException]
			if (!msgHits.isEmpty()) {
				return true;
			} else {
				System.out.println("File not found. No Peer in network returned a hit within set wait time.");
				return false;
			}
		} catch (RemoteException e1) {
			e1.printStackTrace();
		}	
		return false;
	}
	public boolean acceptFile(String filename, byte[] data, int len, String originPeerID, String fileState,
			int versionNum) throws RemoteException {
		System.out.println("   File downloading...");
		peerServer.setRecentlyAddedFile(filename, originPeerID, fileState, versionNum);
        try{
        	File f=new File("copied\\"+filename);	//create file
        	f.createNewFile();
        	FileOutputStream out=new FileOutputStream(f,true);
        	out.write(data,0,len);	//write to file
        	out.flush();
        	out.close();
        }catch(Exception e){
        	e.printStackTrace();
        }
		return true;
	}
	public synchronized void responsetime(String filename) throws RemoteException, MalformedURLException, NotBoundException, InterruptedException{
		long starttime=0;
		long endtime=0;
		PeerClientIF[] peer;
		//run 200 times and average
		for(int i=0;i<200;i++)
		{
			sum=0;
			//start the series of query from its server
			//returns an array of peers with file
			msgIDsuffix++;
			starttime = System.currentTimeMillis();
			peerServer.query(peer_ip+"-"+port_no+"-"+msgIDsuffix , peer_ip, port_no, TIME_TO_LIVE, filename, peerName);
			Thread.sleep(QUERY_WAIT_TIME);	//pause and wait for response to be propagated back [throws InterruptedException]
			for(int j=0;j<queryhittime.size();j++)
			{
				sum+=(queryhittime.get(j)-starttime);
				System.out.println("      Queryhit "+(i+1)+" response time is "+((queryhittime.get(j)-starttime))+"ms");
			}
			System.out.println("      Average response time of the Peer is " + sum/(queryhittime.size()) + "ms");
			endtime+=(sum/(queryhittime.size()));
			queryhittime.clear();
			msgHits.clear();
		}
		System.out.println("   Average response time of the Peer measured 200 time is " + endtime/200 + "ms");
		sum=0;
	}
	public void run() {
		setup();
		//read messages from command line
		String command, task, filename = null;
		long start, endtime;
		System.out.println("Enter your choice and filename/Peer name:\n====================================\n"
				+ "1. Download File from Peer Server\n"
				+ "2. Calculate Average Response time\n"
				+ "3. Edit a file\n"
				+ "4. Update the file(Please enter the file name)\n"
				+ "5. Exit");	
		while (true) {	//continue reading commands
			Scanner cmdline = new Scanner(System.in);
			command = cmdline.nextLine();
			CharSequence symbol = " ";
			//wait till command is received and validate command
			if (command.contains(symbol)) {	
				//retrieve command line inputs separated by char " "
				task = command.substring(0, command.indexOf(' '));
				filename = command.substring(command.indexOf(' ')+1);
				
				if (task.equals("1")) {
					try {
						//verify that file peer is seeking to download is not already in the peer
						if(peerServer.getFileList().contains(filename)) {
							System.out.println("Please enter the filename which you don't possess");
							continue;
						} else {	
							queryhittime.clear();
							msgHits.clear();
							start = System.currentTimeMillis();
							if (findFile(filename)) {	//returns true if file is found
								for(int i=0;i<queryhittime.size();i++)
								{
									sum+=(queryhittime.get(i)-start);
									System.out.println("   Queryhit "+(i+1)+ " from Message ID "+(peer_ip+"-"+port_no+"-"+msgIDsuffix)+
											" response time is "+((queryhittime.get(i)-start))+"ms");
								}
								System.out.println("   Average response time of the Peer is " + sum/(queryhittime.size()) + "ms\n");
								
								//list peers with file
								System.out.println("   The following Peers have the file you want:");
								for (int i=0; i<msgHits.size(); i++) {
									System.out.println("     "+(i+1)+". "+msgHits.get(i)[3]);
								}
								//prompt user to choose Peer to download from
								System.out.println("   Enter number matching the Peer you will like to download from");
												
								int choice = cmdline.nextInt();	//initializes 'choice' with index matching the user's choice of peer to download file from
									if(choice>msgHits.size())
									{
										System.out.println("Please enter the Peer Number from above list");
										continue;
									}
								
								PeerServerIF peerServerIF;
								try {
									//connect peer directly to another peer server through RMI in order to download file
									String peerServerURL = "rmi://"+msgHits.get(choice-1)[1]+":"+msgHits.get(choice-1)[2]+"/peerserver";
									peerServerIF = (PeerServerIF) Naming.lookup(peerServerURL);
									downloadFile(peerServerIF, filename);	//download file from chosen peer
									msgHits.clear();	//remove message hits information
								} catch (RemoteException e) {
									e.printStackTrace();
								} catch (MalformedURLException e) {
									e.printStackTrace();
								} catch (NotBoundException e) {
									e.printStackTrace();
								}
							}
						}
					} catch (InterruptedException | RemoteException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
						
				} else if (task.equals("2")) {
					try {
						//verify that file peer is seeking to download is not already in the peer
						if(peerServer.getFileList().contains(filename)) {
							System.out.println("Please enter the filename which you don't possess");
							continue;
						} else {
							try {
								queryhittime.clear();
								msgHits.clear();
								sum=0;
								//calculate average response time
								responsetime(filename);
							} catch (MalformedURLException | NotBoundException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					} catch (InterruptedException | RemoteException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				
				} 
			} else if (command.equals("3")) {
				System.out.println("You may only edit files you own.\n"
						+ "Choose a file to edit by entering its corresponding number");
				try {
					Vector<FileDoc> fdata = peerServer.getFileData();
					ArrayList<String> cfiles = new ArrayList<String>();
					String filenm;
					for (int y=0; y<fdata.size(); y++){
						if (fdata.get(y).getFolderType().equals("owned")){
							filenm = fdata.get(y).getFilename();
							cfiles.add(filenm);
							System.out.println("  "+(y+1)+". "+filenm);
						}
					}
					int choice = cmdline.nextInt();
					while (choice > cfiles.size()){
						System.out.println("Enter a valid number");
						choice = cmdline.nextInt();
					}
					System.out.println("Type text to append to file of choice and press enter when done");
					Scanner input = new Scanner(System.in);
					String text = input.nextLine();
					//append text to file
					String filepath = "owned\\"+cfiles.get(choice-1);
				    FileWriter fw = new FileWriter(filepath,true); //the true will append the new data
				    fw.write("\n "+text+" \n");//appends the string to the file
				    fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (command.equals("4")) {
				try {
					for(int i=0;i<200;i++){
					peerServer.update(filename);
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}}
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	//close program and exit
			}
			else if (command.equals("5")) {
				System.exit(0);
			}else 
				System.out.println("Usage: <task #> <filename or Peer_name>");
		}
	}
	public void setup(){
		System.out.println("||----------------PEER-TO-PEER FILE SHARING SYSTEM----------------||");
		System.out.println("||----------------MENU----------------||");
		Scanner cmd = new Scanner(System.in);
		System.out.println("\n||---------------------------Configuration Settings-----------------------||");
		//update protocol settings
		System.out.println("Choose an update protocol settings.Enter your choice:- ");
		System.out.println("  1. Push-protocol"
				+ "\n  2. Pull-protocol");
		int option = cmd.nextInt();
		while(option<1 || option>2)
                {
			System.out.println("Please enter a valid input");
			option = cmd.nextInt();
		}
		
		try {
			if (option == 1){
				peerServer.setPushprotocol(true);
				peerServer.setPullprotocol(false);
			} else if (option == 2){
				System.out.println("Set time-to-refresh (TTR) in minutes. Enter a positive integer");
				option = cmd.nextInt();
				System.out.println("File update will refresh every "+option+" minutes");
				peerServer.setTTR(option);
				peerServer.setPushprotocol(false);
				peerServer.setPullprotocol(true);
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
		//stale file settings
		System.out.println("How do you want to handle stale files? Enter your choice:");
		System.out.println("  1. Delete old files"
				+ "\n  2. To Redownload updated copy of old files"
				+ "\n  3. Ignore and continue");
		option = cmd.nextInt();
		while(option<1 || option>3){
			System.out.println("Please enter a valid input");
			option = cmd.nextInt();
		}
		try {
			peerServer.setStaleFileAction(option);
		} catch (RemoteException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		System.out.println("\n||--------------------Configuration Settings Completed!-------------------||");
	}
	
	public int getMsgIDsuffix() throws RemoteException{
		msgIDsuffix++;
		return msgIDsuffix;
	}
	//end
}