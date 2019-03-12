/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package chat;

import java.io.File;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

/**
 *
 * @author MrRadix
 */
public class ServerMain {

    public static void main(String[] args) throws IOException, InterruptedException, NotBoundException {
        Servers[] server = new Servers[ServersI.numberOfServers];
        ReplicaI[] replica = new ReplicaI[ReplicaI.numberOfReplica];
        for (int i = 0; i < ReplicaI.numberOfReplica; i++) {
            Registry replicaRegistery = LocateRegistry.getRegistry("127.0.0.1", ReplicaI.ports[i]);
            replica[i] = (ReplicaI) replicaRegistery.lookup(ReplicaI.services[i]);
        }

        try {
            int i =3;
            server[i] = new Servers(i, replica);

            Registry registry = LocateRegistry.createRegistry(ServersI.ports[i]);
            registry.rebind(ServersI.services[i], server[i]);
            System.out.println("Server: " + ServersI.services[i] + " is running...");

        }
        catch (RemoteException e) {
            e.printStackTrace();
        }

    }
}
