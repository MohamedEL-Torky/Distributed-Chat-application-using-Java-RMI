package chat;

import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NodeI extends Remote {

    int numberOFNodes = 3;
    String[] ipAddr = {"127.0.0.1", "127.0.0.1", "127.0.0.1"};
    String[] files = {"asset/messageCache1.txt", "asset/messageCache2.txt", "asset/messageCache3.txt"};
    String[] services = {"A", "B", "C"};
    Integer[] ports = {2000, 3000, 4000};

    void performMessages(Messages t) throws RemoteException, NotBoundException;

    void ack(Messages t) throws RemoteException;
    void election(int server) throws RemoteException;
}
