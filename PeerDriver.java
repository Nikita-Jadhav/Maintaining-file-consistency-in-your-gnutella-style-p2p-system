import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class PeerDriver
{
        static String[] arrgs;
	static int numofneighbors;
	static PeerServer instance;
	public static void main(String[] args) throws RemoteException, MalformedURLException, NotBoundException {
		if (args.length >= 3) 
                {
			arrgs = new String[args.length];
			for (int i=0; i<arrgs.length; i++)
				arrgs[i] = args[i];
			
			numofneighbors = (args.length - 3)/2;
			new Thread(new lunchServerThenClient()).start();
			
		} 
                else 
                {
			System.err.println("Usage: PeerClientDriver <peer_name> < peer_ip> < peer_port_#> "
					+ "<neighboring-peer_ip>  <neighboring-peer_port_#>");
		}
	}
	
	static class lunchServerThenClient implements Runnable
	{
		public void run()
		{			
			try 
                        {
				System.setProperty("java.rmi.server.hostname",arrgs[1]);	//set server property
				PeerServer peerserver = new PeerServer(arrgs[1], arrgs[2]);
				instance = peerserver;
				//rebind server to ip(localhost) and args[1](port_#)
				Naming.rebind("rmi://"+arrgs[1]+":"+arrgs[2]+"/peerserver",peerserver);
				
				System.out.println("||----------------PEER-TO-PEER FILE SHARING SYSTEM----------------||");
				System.out.println("       		     <*"+arrgs[0]+"* SERVER IS UP AND RUNNING>                  ");
				int count=0;
				//creating peer-client object
				PeerClient clientserver = new PeerClient(arrgs[0],arrgs[1],arrgs[2],numofneighbors, instance);
				for (int i = 3; i < arrgs.length; i += 2)
                                {
					clientserver.addNeighboringPeer(arrgs[i], arrgs[i+1], count);
					count++;
					System.out.println("Connected to neighboring peer with credential: "+arrgs[i]+":"+arrgs[i+1]);
				}
				new Thread(clientserver).start();
				
			} 
                        catch (RemoteException | MalformedURLException e)
                        {
				System.out.println("Error running PeerServer Thread");
				e.printStackTrace();
			}
		}
	}
}