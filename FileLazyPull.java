import java.io.File;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Scanner;
import java.util.Vector;
class FileLazyPull implements Runnable 
{
	private PeerServerIF pserver;
	private PeerClientIF pclient;
	private long waittime;
	public FileLazyPull(PeerClient peerClient, PeerServerIF peerServer, long t) 
        {
		PeerServerIF pserver = peerServer;
		PeerClientIF pclient = peerClient;
		waittime = t;
	}
	public void run() {
		while (true){
			try 
                        {
				Thread.sleep(waittime*60*1000);
				Vector<FileDoc> mtdata = pserver.getFileData();
				FileDoc doc;
				String fname;
				//check for expired copied files
				for (int z=0; z<mtdata.size(); z++) {
					doc = mtdata.get(z);
					fname = doc.getFilename();
					if (doc.getConsistency().equals("expired")){
						String url = "rmi://"+doc.getOriginServerID()+"/peerserver";
						PeerServerIF originServer = (PeerServerIF) Naming.lookup(url);
						int response = originServer.pullValidation(fname, doc.getVersionNum());
						if (response > 0){
							doc.setTimeToRefresh(response);
							System.out.println("Interrupt Message: Lazy Pull; file '"+fname+"' is up to date");
						} else {
							invalidPullHandler(doc, fname);
							System.out.println("Interrupt Message: Lazy Pull; file '"+fname+"' is out of date");
						}
					}
				}
			} catch (InterruptedException | RemoteException | MalformedURLException | NotBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void invalidPullHandler(FileDoc foc, String fname) throws RemoteException, MalformedURLException, NotBoundException {
		foc.setConsistency("invalid");
		System.out.println("Interrupt Message: Invalidation Handler; Enter your choice:");
		System.out.println("  1. To Delete old file"
				+ "\n  2. To Redownload updated file"
				+ "\n  3. To Ignore and continue");
		Scanner cmd = new Scanner(System.in);
		int option = cmd.nextInt();
		while(option<1 || option>3){
			System.out.println("Please enter a valid input");
			option = cmd.nextInt();
		}
		if (option == 1){
			File d = new File(pserver.getPeerDir()+"\\"+foc.getFolderType()+"\\"+fname);
			d.delete();
			System.out.println("File '"+fname+"' is deleted");
		} else if (option == 2){
			//connect peer directly to origin peer server through RMI in order to download file
			String url = "rmi://"+foc.getOriginServerID()+"/peerserver";
			PeerServerIF originServer = (PeerServerIF) Naming.lookup(url);
			pclient.downloadFile(originServer, fname);	//download file from chosen peer
		}
	}

}