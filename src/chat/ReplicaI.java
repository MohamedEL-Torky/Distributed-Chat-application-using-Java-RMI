/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author MrRadix
 */
public interface ReplicaI extends Remote {

    int numberOfReplica = 4;
    Integer[] ports = {5100, 6100, 7100, 8100};
    String[] services = {"Replica1", "Replica2", "Replica3", "Replica4"};
    String[] files = {"messageCache1.txt", "messageCache2.txt", "messageCache3.txt"};
    int[] replica_id = {0, 1, 2, 3};

    void upload(File fileToBackup, int node_id) throws RemoteException;

    File download(int node_id) throws RemoteException;

    void initializeFile() throws RemoteException;

}
