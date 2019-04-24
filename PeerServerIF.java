import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Vector;

public interface PeerServerIF  extends Remote
{
	boolean sendFile(PeerClientIF peerClient, String filename) throws RemoteException;
	void query(String msgID, String cIP, String cPortNo, long timeToLive, String filename, String cPeerName) throws RemoteException;
	void queryhit(String msgID, long timeToLive, String filename, String hitPeerIP, String hitPeerPN, String hitPeerName, long l, boolean originmessage) throws RemoteException;
	String getPeerDir() throws RemoteException;
	
	void setClientInstance(PeerClientIF peerClient) throws RemoteException;
	String getIP() throws RemoteException;
	String getPN() throws RemoteException;
	
	Vector<FileDoc> getFileData() throws RemoteException;
	boolean getPullprotocol() throws RemoteException;
	void invalidate(String msgID, String pIP, String pPN, String originID, String fname, int vn, String pname) throws RemoteException;
	void setRecentlyAddedFile(String fn, String oID, String st, int vnum) throws RemoteException;
	void updateFileListCtr(String dirType) throws RemoteException;
	void updateFileListDel(String dirType) throws RemoteException;
	void updateFileListMod(String dirType) throws RemoteException;
	ArrayList<String> getFileList() throws RemoteException;
	int pullValidation(String fname, int versionNum) throws RemoteException;
	void setStaleFileAction(int staleFileAction) throws RemoteException;
	int getStaleFileAction() throws RemoteException;
	void setPullprotocol(boolean pullprotocol) throws RemoteException;
	void setPushprotocol(boolean pullprotocol) throws RemoteException;
	void setTTR(int ttr) throws RemoteException;
	 void update(String filename) throws RemoteException;
	int getTTR() throws RemoteException;
}