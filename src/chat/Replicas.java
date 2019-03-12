/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MrRadix
 */
public class Replicas extends UnicastRemoteObject implements ReplicaI {

    String directoryPath = "";
    private ArrayList<File> backedData = new ArrayList<File>();

    protected Replicas(int replica_id) throws RemoteException {
        directoryPath = "backupServer" + "/" + ReplicaI.services[replica_id];
    }

    @Override
    public void upload(File fileToBackup, int file_id) throws RemoteException {
        BufferedWriter bwrite;
        try {
            bwrite = new BufferedWriter(new FileWriter(backedData.get(file_id), false));
            Scanner fr = new Scanner(fileToBackup);

            while (fr.hasNextLine()) {
                String s = fr.nextLine();
                bwrite.write(s);
                bwrite.newLine();
            }
            fr.close();
            bwrite.close();
        }
        catch (IOException ex) {
            Logger.getLogger(Replicas.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public File download(int file_id) throws RemoteException {
        File localFile = new File(directoryPath + "/" + ReplicaI.files[file_id]);

        if (!localFile.exists()) {
            localFile.getParentFile().mkdir();
        }
        return localFile;
    }

    @Override
    public void initializeFile() throws RemoteException {

        for (int i = 0; i < NodeI.numberOFNodes; i++) {
            new File(directoryPath).mkdirs();
            backedData.add(new File(directoryPath + "/" + ReplicaI.files[i]));
            try {
                backedData.get(i).createNewFile();
            }
            catch (IOException ex) {
                Logger.getLogger(Replicas.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void main(String[] args) {
        Replicas[] replica = new Replicas[ReplicaI.numberOfReplica];
        try {
            for (int i = 0; i < ReplicaI.numberOfReplica; i++) {
                replica[i] = new Replicas(i);
                Registry registry = LocateRegistry.createRegistry(ReplicaI.ports[i]);
                registry.rebind(ReplicaI.services[i], replica[i]);
                replica[i].initializeFile();
                System.out.println("Replicas: " + i + " is running...");
            }
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        while (true);
    }

}
