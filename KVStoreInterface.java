import java.rmi.Remote;
import java.rmi.RemoteException;

public interface KVStoreInterface extends Remote {
  String get(String key) throws RemoteException;
  String put(String key, String value) throws RemoteException;
  String delete(String key) throws RemoteException;
}

