package chat;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    static boolean primaryServerIsDown = false;
    static boolean secondaryOneDown = false;
    static boolean secondaryTwoDown = false;
    static boolean secondaryThreeDown = false;
    private static int desiredServer = 0;
    
    public static void main(String[] args) throws IOException {
        
        //int pId = Integer.parseInt(args[0]);
        int pId = 2; // 0 -> process 1, 1 -> process 2 ,etc....

        try {
            System.out.println("Process: " + NodeI.services[pId]);

            Node obj = new Node(pId);
            initServer(obj);
            initClient(obj, pId);

        }
        catch (RemoteException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        catch (NotBoundException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

    }

    public static void initServer(Node obj) throws RemoteException {
        Registry reg = LocateRegistry.createRegistry(obj.myPort);
        reg.rebind(obj.myService, obj);
    }

    private static void initClient(Node obj, int pId) throws RemoteException, NotBoundException, IOException {

        //intiate primary backup server interface to use RMI on backup server methods
        ServersI[] backupServer = new ServersI[ServersI.numberOfServers];
        for (int i = 0; i < ServersI.numberOfServers; i++) {
            Registry primaryBackupRegistry = LocateRegistry.getRegistry("127.0.0.1", ServersI.ports[i]);
            backupServer[i] = (ServersI) primaryBackupRegistry.lookup(ServersI.services[i]);
        }

        //Check if the backupserver is up or not every 100 ms
        //if not switch to the secondary server
        Timer heartBeatTimer = new Timer();
        heartBeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                //election
                desiredServer = obj.desired;
                try {
                    backupServer[0].heartBeat();
                    primaryServerIsDown = false;
                }
                catch (Exception ex1) {
                    primaryServerIsDown = true;
                    //First Secondary backup hearbeat
                    //Note this method won't heart beat the other seoncadry servers
                    //Unless the primary is down
                }
            }
        }, 0, 100);

        // setting physical_time
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

        //Open a file
        File file = new File(NodeI.files[pId]);
        /*
        check if the file exist, 
        if so read all text in file
        else get cached file in the backup servers
         */
        if (file.exists()) {
            //Check if the file is equal to the server recoreds
            Scanner fr = new Scanner(file);
            while (fr.hasNextLine()) {
                String s = fr.nextLine();
                System.out.println(s);
            }
            sendFileSize(pId, backupServer);
        }
        else {
            //if the file does not exist
            //Write your code here to get the cached chat from backup server
            System.out.println("Getting cach from server");
            backupServersHandlerDownload(pId, backupServer);
        }

        /*
        -----------------fetching message from queue part-----------------------
        reading others nodes messages
        @Timer: A facility for threads to schedule tasks for future execution 
        in a background thread. Tasks may be scheduled for one-time execution, 
        or for repeated execution at regular intervals.
        
        public void scheduleAtFixedRate(TimerTask task,long delay,long period)
        Schedules the specified task for repeated fixed-rate execution,
        Parameters:
        task - task to be scheduled.
        delay - delay in milliseconds before task is to be executed.
        period - time in milliseconds between successive task executions.
         */
        
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                try {
                    String recvMsg = obj.fetchNewMessages();
                    if (recvMsg.length() > 0) {
                        try (FileWriter fr = new FileWriter(file, true);
                                BufferedWriter br = new BufferedWriter(fr);
                                PrintWriter pr = new PrintWriter(br)) {
                            LocalDateTime now = LocalDateTime.now();
                            System.out.println(recvMsg + " " + dtf.format(now));
                            pr.println(recvMsg + " " + dtf.format(now));
                            pr.close();
                            br.close();
                            fr.close();
                            backupServersHandlerUpload(pId, backupServer);
                        }
                        catch (IOException ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                }
                catch (RemoteException ex) {
                    Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }, 0, 100);

        /*
        -----------------------chatting part------------------------------------
        the following method working infinitly to continously get input from the
        user and creating new message object, then multicast it to the rest of 
        nodes
         */
        
        while (true) {
            String msg = obj.scan.nextLine();
            try (FileWriter fr = new FileWriter(file, true);
                    BufferedWriter br = new BufferedWriter(fr);
                    PrintWriter pr = new PrintWriter(br)) {
                //Get message from client console
                pr.println(msg);
                pr.close();
                br.close();
                fr.close();
                //Initialize message ID
                String mId = UUID.randomUUID().toString();
                String sender = obj.myService;

                // Update local logical clock before sending a message
                obj.lClock++;
                //Create a message object to be sent (time-stamped with lClock value)
                Messages t = new Messages(mId, pId, sender, msg, obj.lClock);

                obj.multicastMessages(t);
                if(primaryServerIsDown){
                    while(desiredServer == 0){
                        System.out.println("Getting new server");
                    };
                }
                backupServersHandlerUpload(pId, backupServer);
            }
        }
    }

    private static void sendFileSize(int pId, ServersI[] backupServer) throws RemoteException, IOException {
        //Call file servant which handel the upload and downlaod
        File localFile = new File(NodeI.files[pId]);
        if (!primaryServerIsDown) {
            byte[] f1 = Files.readAllBytes(localFile.toPath());
            backupServer[0].setSize(f1, pId);
            System.out.println("Using the primary backup servers");
        }
        else {
            byte[] f1 = Files.readAllBytes(localFile.toPath());
            backupServer[desiredServer].setSize(f1, pId);
            System.out.println("Using the " + ServersI.services[desiredServer] + " servers");
        }
    }

    private static void backupServersHandlerUpload(int pId, ServersI[] backupServer) throws RemoteException {
        //Call file servant which handel the upload and downlaod
        File localFile = new File(NodeI.files[pId]);
        if (!primaryServerIsDown) {
            backupServer[0].upload(localFile, pId);
            System.out.println("Using the primary backup servers");
        }
        else {
            backupServer[desiredServer].upload(localFile, pId);
            System.out.println("Using the " + ServersI.services[desiredServer] + " servers");
        }
    }

    private static void backupServersHandlerDownload(int pId, ServersI[] backupServer) throws RemoteException, FileNotFoundException, IOException {
        //Call file servant which handel the upload and downlaod
        
        File localFile = new File(NodeI.files[pId]);
        if (!primaryServerIsDown) {
            desiredServer = 0;
            System.out.println("Using the " + ServersI.services[desiredServer] + " servers");
        }
        else {
            
            System.out.println("Using the " + ServersI.services[desiredServer] + " servers");
        }
        FileWriter localFileWriter = new FileWriter(localFile);
        File serverFile = backupServer[desiredServer].download(pId);
        Scanner fr = new Scanner(serverFile);
        while (fr.hasNextLine()) {
            String s = fr.nextLine();
            localFileWriter.write(s);
            localFileWriter.write("\n");
            System.out.println(s);
        }
        localFileWriter.close();
    }

    
}
