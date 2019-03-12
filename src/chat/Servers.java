/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import static chat.Main.primaryServerIsDown;
import static chat.Node.n;
import static chat.NodeI.ports;
import static chat.NodeI.services;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author MrRadix
 */
public class Servers extends UnicastRemoteObject implements ServersI {

    private static boolean lock = false;
    private static int currentServer = 0;
    static boolean primaryServerIsDown = false;
    static boolean secondaryOneDown = false;
    static boolean secondaryTwoDown = false;
    static boolean secondaryThreeDown = false;
    private ArrayList<Boolean> activeServer = new ArrayList<Boolean>();
    private int my_id;
    private static int serverStaticID;
    private int r1 = 0, r2 = 1, r3 = 2;
    private int nr1 = 0, nr2 = 1, nr3 = 2;
    private byte[][] last_size = new byte[3][];
    private String directoryPath = "backupServer/";
    private int numberOfAccess = 1;
    private ArrayList<Integer> upServers = new ArrayList<Integer>();
    ReplicaI[] replica = new Replicas[ReplicaI.numberOfReplica];

    protected Servers(int server_id, ReplicaI[] replica) throws RemoteException, FileNotFoundException {
        this.my_id = server_id;
        this.replica = replica;
        this.serverStaticID = server_id;
        upServers.add(0);
        upServers.add(0);
        upServers.add(0);
        upServers.add(0);
        activeServer.add(true);
        activeServer.add(true);
        activeServer.add(true);
        activeServer.add(true);

        Timer timer = new Timer();

        Timer heartBeatTimer = new Timer();
        heartBeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (my_id != 0) {
                    try {
                        Registry serverRegistry = LocateRegistry.getRegistry("127.0.0.1", ServersI.ports[0]); //1 , currentServer =0
                        ServersI server = (ServersI) serverRegistry.lookup(ServersI.services[0]);
                        //if server is up
                    } catch (RemoteException | NotBoundException ex) {
                        //if server is down
                        activeServer.set(0, false);
                        try {
                        Registry serverRegistry = LocateRegistry.getRegistry("127.0.0.1", ServersI.ports[1]); //1 , currentServer =0
                        ServersI server = (ServersI) serverRegistry.lookup(ServersI.services[1]);
                        //if server is up
                        }
                        catch(RemoteException | NotBoundException ex1){
                            activeServer.set(1, false);
                        }
                        try {
                        Registry serverRegistry = LocateRegistry.getRegistry("127.0.0.1", ServersI.ports[2]); //1 , currentServer =0
                        ServersI server = (ServersI) serverRegistry.lookup(ServersI.services[2]);
                        //if server is up
                        }
                        catch(RemoteException | NotBoundException ex1){
                            activeServer.set(2, false);
                        }
                        try {
                        Registry serverRegistry = LocateRegistry.getRegistry("127.0.0.1", ServersI.ports[3]); //1 , currentServer =0
                        ServersI server = (ServersI) serverRegistry.lookup(ServersI.services[3]);
                        //if server is up
                        }
                        catch(RemoteException | NotBoundException ex1){
                            activeServer.set(3, false);
                        }
                    }

                }

            }
        }, 0, 100);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                if (!lock && !activeServer.get(currentServer)) {
                    System.out.println("Me Server " + ServersI.services[my_id] + " is now electing");
                    lock = true;
                    try {
                        System.out.println("Server " + ServersI.services[my_id] + " is electing the new server");
                        startElection();
                    } catch (RemoteException ex) {
                        Logger.getLogger(Servers.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (NotBoundException ex) {
                        Logger.getLogger(Servers.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    lock = false;
                }
            }
        }, 0, 10000);

    }

    public void currentUpServer(int upServer) {
        currentServer = upServer;
    }

    public void startElection() throws RemoteException, NotBoundException {
        ServersI[] server = new ServersI[ServersI.numberOfServers];
        for (int i = my_id + 1; i < ServersI.numberOfServers; i++) {
            try {
                Registry serverRegistry = LocateRegistry.getRegistry("127.0.0.1", ServersI.ports[i]); //1 , currentServer =0
                server[i] = (ServersI) serverRegistry.lookup(ServersI.services[i]);
                //if server is up
                upServers.set(i,ServersI.server_id[i]);
            } catch (RemoteException ex) {
                //if server is down
                Logger.getLogger(Servers.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(my_id == 3){
            upServers.set(3,ServersI.server_id[3]);
        }
        currentServer = Collections.max(upServers);
        System.out.println("Server " + ServersI.services[my_id] + " choosed " + ServersI.services[currentServer] + " as the new primary");
        NodeI[] e = new NodeI[NodeI.numberOFNodes];
        for (int i = 0; i < NodeI.numberOFNodes; i++) {
            Registry reg = LocateRegistry.getRegistry(NodeI.ports[i]);
            e[i] = (NodeI) reg.lookup(NodeI.services[i]);
            e[i].election(currentServer);
        }
        
        for (int i = my_id + 1; i < ServersI.numberOfServers; i++) {
            server[i].currentUpServer(currentServer);
        }

    }

    public void setSize(byte[] size, int node_id) throws RemoteException {
        last_size[node_id] = size;
        System.out.println("Recived " + size + " Bytes from the client");
    }

    @Override
    public void lastRoundRobin(int r1, int r2, int r3) throws RemoteException {
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
        System.out.println("Recived r1 = " + r1 + " r2 = " + r2 + " r3 =" + r3);
    }

    @Override
    public void upload(File fileToBackup, int node_id) throws RemoteException {

        System.out.println();

        replica[nr1].upload(fileToBackup, node_id);
        replica[nr2].upload(fileToBackup, node_id);
        replica[nr3].upload(fileToBackup, node_id);
        File test = new File(directoryPath + ReplicaI.services[nr1] + "/" + ReplicaI.files[node_id]);
        try {
            last_size[node_id] = Files.readAllBytes(test.toPath());
            for (int i = 1; i < ServersI.numberOfServers; i++) {
                try {
                    if (my_id != i) {
                        Registry reg = LocateRegistry.getRegistry(ServersI.ports[i]);
                        ServersI e = (ServersI) reg.lookup(ServersI.services[i]);
                        e.setSize(last_size[node_id], node_id);
                    }
                } catch (Exception ex) {
                    System.out.println("The" + ServersI.services[i] + " server is down !");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Servers.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Round robin
        if (numberOfAccess == 3) {
            numberOfAccess = 0;
            r1 = nr1;
            r2 = nr2;
            r3 = nr3;
            nr1++;
            nr2++;
            nr3++;
            nr1 = (nr1 % 4);
            nr2 = (nr2 % 4);
            nr3 = (nr3 % 4);
            for (int i = 1; i < ServersI.numberOfServers; i++) {
                try {
                    if (my_id != i) {
                        Registry reg = LocateRegistry.getRegistry(ServersI.ports[i]);
                        ServersI e = (ServersI) reg.lookup(ServersI.services[i]);
                        e.lastRoundRobin(nr1, nr2, nr3);
                    }
                } catch (Exception ex) {
                    System.out.println("The" + ServersI.services[i] + " server is down !");
                }
            }
        } else {
            numberOfAccess++;
        }

    }

    @Override
    public File download(int node_id) throws RemoteException {
        System.out.println("Checking replications consistincy before uploading to the client");
        isEqual(node_id);
        System.out.println("Sending file from replica " + Math.max(r1, Math.max(r2, r3)));
        return replica[Math.max(r1, Math.max(r2, r3))].download(node_id);
    }

    @Override
    public void heartBeat() throws RemoteException {
    }

    private void isEqual(int cache_id) throws RemoteException {
        try {
            File file1 = new File(directoryPath + ReplicaI.services[r1] + "/" + ReplicaI.files[cache_id]);
            File file2 = new File(directoryPath + ReplicaI.services[r2] + "/" + ReplicaI.files[cache_id]);
            File file3 = new File(directoryPath + ReplicaI.services[r3] + "/" + ReplicaI.files[cache_id]);

            byte[] f1 = Files.readAllBytes(file1.toPath());
            byte[] f2 = Files.readAllBytes(file2.toPath());
            byte[] f3 = Files.readAllBytes(file3.toPath());

            boolean fileVerify1 = Arrays.equals(f1, last_size[cache_id]);
            boolean fileVerify2 = Arrays.equals(f2, last_size[cache_id]);
            boolean fileVerify3 = Arrays.equals(f3, last_size[cache_id]);

            if (!fileVerify1) {
                System.out.println("Found Corrputed File: " + ReplicaI.files[cache_id] + " in replica " + (r1 + 1));
                if (fileVerify2) {
                    replica[r1].upload(file2, cache_id);
                    System.out.println("Fixed The courrpted from " + (r2 + 1));
                } else if (fileVerify3) {
                    replica[r1].upload(file3, cache_id);
                    System.out.println("Fixed The courrpted from " + (r3 + 1));
                }
            }
            if (!fileVerify2) {
                System.out.println("Found Corrputed File in replica " + (r2 + 1));
                if (fileVerify1) {
                    replica[r2].upload(file1, cache_id);
                    System.out.println("Fixed The courrpted from " + (r1 + 1));
                } else if (fileVerify3) {
                    replica[r2].upload(file3, cache_id);
                    System.out.println("Fixed The courrpted from " + (r3 + 1));
                }
            }
            if (!fileVerify3) {
                System.out.println("Found Corrputed File in replica " + (r3 + 1));
                if (fileVerify1) {
                    replica[r3].upload(file1, cache_id);
                    System.out.println("Fixed The courrpted from " + (r1 + 1));
                } else if (fileVerify2) {
                    replica[r3].upload(file2, cache_id);
                    System.out.println("Fixed The courrpted from " + (r2 + 1));
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Servers.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    static class ShutDown extends Thread {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            super.run();
            try {
                System.out.println("Terminating node");
                LocateRegistry.getRegistry().unbind(ServersI.services[serverStaticID]);
            } catch (AccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (NotBoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

}
