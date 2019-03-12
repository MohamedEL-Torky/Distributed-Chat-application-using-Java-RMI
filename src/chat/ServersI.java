/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 *
 * @author MrRadix
 */
public interface ServersI extends Remote {

    int numberOfServers = 4;
    Integer[] ports = {5000, 6000, 7000, 8000};
    String[] services = {"primary", "secondry 1", "secondry 2", "secondry 3"};
    int[] server_id = {0, 1, 2, 3};
    
    void upload(File fileToBackup, int node_id) throws RemoteException;

    void heartBeat() throws RemoteException;

    void lastRoundRobin(int r1, int r2, int r3) throws RemoteException;

    void setSize(byte[] size, int node_id) throws RemoteException;
    
    void currentUpServer(int upServer) throws RemoteException;

    File download(int node_id) throws RemoteException;
}
