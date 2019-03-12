package chat;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.Set;

public class Node extends UnicastRemoteObject implements NodeI {

    private static final long serialVersionUID = 1L;

    final static int n = ipAddr.length;
    static double balance = 1000;
    public  int desired = 0;
    int lClock, myPort;
    String myIp, myService;
    PriorityQueue<Messages> messageQueue;
    HashMap<String, Integer> messageMapAcks;
    Scanner scan;

    protected Node(int idx) throws RemoteException {
        myIp = ipAddr[idx];
        myPort = ports[idx];
        myService = services[idx];
        lClock = 0;
        messageQueue = new PriorityQueue<Messages>();
        messageMapAcks = new HashMap<String, Integer>();
        scan = new Scanner(System.in);
    }

    public void multicastMessages(Messages t) throws RemoteException, NotBoundException {
        boolean delay = false;
        for (int i = 0; i < n; i++) {
            if (delay && t.sender.equals("A")) { // Delay Sending Messages from A to C
                if (i == 2) {
                    Delayer delayer = new Delayer(ports[i], services[i], t);
                    new Thread(delayer).start();
                    continue;
                }
            }
            Registry reg = LocateRegistry.getRegistry(ports[i]);
            NodeI e = (NodeI) reg.lookup(services[i]);
            e.performMessages(t);
        }
        //displayMessagess();
    }

    @Override
    public void performMessages(Messages t) throws RemoteException, NotBoundException {
        messageQueue.add(t);
        if (!t.sender.equalsIgnoreCase(myService)) {
            lClock = Math.max(lClock, t.clock) + 1;
        }
        multicastAck(t);
    }

    private void multicastAck(Messages t) throws RemoteException, NotBoundException {
        boolean delay = false;
        for (int i = 0; i < n; i++) {
            // Delay Sending Ack from A to C
            if (delay && myService.equals("A")) {
                if (i == 2) {
                    System.out.println("|||||||||||||||||||||||||||||||||||||||||");
                    AckDelayer ackDelayer = new AckDelayer(ports[i], services[i], t);
                    new Thread(ackDelayer).start();
                    continue;
                }
            }
            Registry reg = LocateRegistry.getRegistry(ports[i]);
            NodeI e = (NodeI) reg.lookup(services[i]);
            e.ack(t);
        }
    }

    @Override
    public void ack(Messages t) throws RemoteException {
        if (messageMapAcks.containsKey(t.mId)) {
            messageMapAcks.put(t.mId, messageMapAcks.get(t.mId) + 1);
        }
        else {
            messageMapAcks.put(t.mId, 1);
        }
    }

    /*
    @fetchNewMessages return messages for others nodes, 
    and if node == itself, it will return blank
     */
    public String fetchNewMessages() throws RemoteException {
        if (messageQueue.size() > 0 && messageMapAcks.containsKey(messageQueue.peek().mId)
                && messageMapAcks.get(messageQueue.peek().mId) == n) {

            Messages t = messageQueue.poll();
            messageMapAcks.remove(t.mId);

            if (!t.sender.equalsIgnoreCase(myService)) {
                return "\t\t\t\t\t" + t;
            }
            else {
                return "";
            }
        }
        return "";
    }
    
    public void election(int server) {
        desired = server;
    }

}
