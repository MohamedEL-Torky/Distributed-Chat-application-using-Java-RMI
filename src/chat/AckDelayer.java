package chat;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class AckDelayer implements Runnable {

    private int port;
    private String serviceName;
    private Messages message;

    public AckDelayer(int port, String serviceName, Messages message) {
        this.port = port;
        this.serviceName = serviceName;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(7000);
            Registry reg = LocateRegistry.getRegistry(port);
            NodeI e = (NodeI) reg.lookup(serviceName);
            e.ack(message);
        }
        catch (RemoteException | NotBoundException | InterruptedException e1) {
            e1.printStackTrace();
        }
    }

}
