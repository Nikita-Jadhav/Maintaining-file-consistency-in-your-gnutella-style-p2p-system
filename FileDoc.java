import java.io.Serializable;
import java.rmi.RemoteException;

public class FileDoc implements Serializable
{
	private PeerServerIF pServer;
	protected String filename;
	private String originServerID;
	private String folderType;
	private String consistency;
	private long lastModified;
	private int versionNum;
	private int timeToRefresh;
	
	public FileDoc(PeerServer ps, String fn, String oSID, String fType, String state, long lastMT, int vNum, int ttr)
        {
		pServer = ps;
		filename = fn;
		originServerID = oSID;
		folderType = fType;
		consistency = state;
		lastModified = lastMT;
		versionNum = vNum;
		timeToRefresh = ttr;
		
		try 
                {
			if (pServer.getPullprotocol() && folderType.equals("copied"))
				new Thread(new pollFile(this, filename, consistency, timeToRefresh)).start();
		} 
                catch (RemoteException e) 
                {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getOriginServerID() {
		return originServerID;
	}
	public void setOriginServerID(String originServerID) {
		this.originServerID = originServerID;
	}
	public String getFolderType() {
		return folderType;
	}
	public void setFolderType(String folderType) {
		this.folderType = folderType;
	}
	public long getLastModified() {
		return lastModified;
	}
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
	public int getVersionNum() {
		return versionNum;
	}
	public void setVersionNum(int versionNum) {
		this.versionNum = versionNum;
	}
	public int getTimeToRefresh() {
		return timeToRefresh;
	}
	public void setTimeToRefresh(int timeToRefresh) {
		this.timeToRefresh = timeToRefresh;
	}
	public String getConsistency() {
		return consistency;
	}
	public void setConsistency(String consistency) {
		this.consistency = consistency;
		
		try {
			if (pServer.getPullprotocol() && folderType.equals("copied"))
				new Thread(new pollFile(this, filename, consistency, timeToRefresh)).start();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static class pollFile implements Runnable
	{
		private FileDoc fInstance;
		String fname;
		String state;
		int ttr;
		public pollFile(FileDoc fileDoc, String filename, String consistency, int timeToRefresh) {
			fInstance = fileDoc;
			fname = filename;
			state = consistency;
			ttr = timeToRefresh;
		}

		public void run()
		{	
			if (state.equals("valid")) {
				try {
					Thread.sleep((long) ttr*60*1000);
					fInstance.setConsistency("expired");
					System.out.println("Message Interrupt: File Polling; File '"+fname+"' has expired");
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}
	}
}