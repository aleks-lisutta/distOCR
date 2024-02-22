package Manager.Protocol;

import Manager.Connection.ConnectionHandler;
import Manager.Job.DataStorageInterface;
import Manager.Job.JobExecutor;
import Manager.Requests.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AwsProtocol extends Protocol<Request>{

    private static Map<String, Lock> appMessagesLocksMap = new HashMap<>();
    private static boolean shouldTerminate = false;
    private ConnectionHandler workersConnection;
    private ConnectionHandler appConnection;
    private JobExecutor jobExecutor;
    private DataStorageInterface dataStorage;


    private static List <String[]> jobArray = new ArrayList <String[]>() ;



    public AwsProtocol(ConnectionHandler appConnection, ConnectionHandler workersConnection, JobExecutor jobExecutor, DataStorageInterface dataStorage){
        this.appConnection = appConnection;
        this.workersConnection = workersConnection;
        this.jobExecutor = jobExecutor;
        this.dataStorage = dataStorage;
    }

    @Override
    public Runnable process(Request req) throws RequestUnknownException, NotifyFinishedException { //retruns
        if (req instanceof AppToManagerRequest){
            if (((AppToManagerRequest) req).isTermination() || shouldTerminate){
                shouldTerminate = true;
                throw new NotifyFinishedException();
            }
            return this.processApplicationRequest((AppToManagerRequest) req);
        }
        if (req instanceof WorkerToManagerRequest){
            return this.processWorkerRequest((WorkerToManagerRequest) req);
        }
        throw new RequestUnknownException();
    }

    private boolean checkIfComplete(String id , String link , String result ){
         boolean finished = true;
        for( String[] i : jobArray)
        {
             if( i[0].equals(id) && i[1].equals(link)&& i[2].equals(""))
             {
                 i[2] = result;
             }
             if(i[2].equals(""))
             {
                 finished = false;
             }
        }
        return finished;

    }




    private Runnable processWorkerRequest(WorkerToManagerRequest req) {
        return () -> {
            System.out.println("Processing a worker request");
            System.out.println("req   :" + req);
            String[] x = req.getData();
            for(String i : x){
                System.out.print(i);
            }
           System.out.println("req.getData   :" + x);
            String appMessageId = x[0];
            System.out.println("appMessageId  "+appMessageId );
            if (appMessagesLocksMap.containsKey(appMessageId)){
                System.out.println("Hey1!!!");
                Lock currLock = appMessagesLocksMap.get(appMessageId);
                    currLock.lock();
                    if (checkIfComplete(appMessageId, req.getData()[1], req.getData()[2])) {
                        System.out.println("Hey2!!!");
                        currLock.unlock();
                        String path ="";
                       try {
                           System.out.println("Hey3!!!  " );
                            path = writeToFile();
                           dataStorage.uploadFile(path);
                       }catch(IOException e){
                           System.out.println("upload error"+e.getMessage());
                       }

                        ManagerToAppRequest managerToAppRequest = new ManagerToAppRequest();

                        managerToAppRequest.setData(appMessageId+"|"+path);
                        System.out.println("Hey4!!!  " );
                        try {
                            this.appConnection.sendMessage(managerToAppRequest);
                            jobArray = new ArrayList <String[]>();
                            System.out.println("Hey5!!!  " );
                        } catch (RequestUnknownException e) {
                            System.out.println("Hey6!!!  " + e.getMessage());
                            e.printStackTrace();
                        }
                        System.out.println("Finished working on application message: " + appMessageId);
                        appMessagesLocksMap.remove(appMessageId);
                    } else {
                        System.out.println("unlock!!!");
                        currLock.unlock();
                    }

            }
        };
    }
    public static String writeToFile() throws IOException {
        String res = "";
        for(String[] i : jobArray)
        {
            for (String j : i )
            {
                res = res + j +"|";
            }
            res = res + "!";
        }
        String x = "Results.txt";
        File file = new File(x);

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));


        writer.write(res);


        writer.close();

        return x;



    }



    private Runnable processApplicationRequest(AppToManagerRequest req) {
        return () -> {
            try {
                System.out.println("Processing an application request");
                List<ManagerToWorkerRequest> managerToWorkerRequests = new LinkedList<>();

               String key = req.getData();

                dataStorage.S3DownloadFileExample(key);

                System.out.println("Done Download!!!!!!");


                File myObj = new File(key);
                Scanner myReader = new Scanner(myObj);

                while (myReader.hasNextLine()) {
                    String data = myReader.nextLine();
                    String[] Temp = new String [3];
                    Temp[0] = req.getId();
                    Temp[1] = data;
                    Temp[2] = "";
                    jobArray.add(Temp);

                    System.out.println("data  "+data);
                    ManagerToWorkerRequest managerToWorkerRequest = new ManagerToWorkerRequest();
                    managerToWorkerRequest.setData(new AbstractMap.SimpleEntry<>( req.getId(), data));
                    managerToWorkerRequest.setAppMessageId(req.getId());
                    managerToWorkerRequests.add(managerToWorkerRequest);
                    AwsProtocol.appMessagesLocksMap.putIfAbsent(req.getId(), new ReentrantLock());
                    String messageId = this.workersConnection.sendMessage(managerToWorkerRequest);

                }
                myReader.close();


                jobExecutor.createWorkers();
            } catch (Exception  | RequestUnknownException e){};



        };
    }

    public boolean shouldTerminate(){
        return (shouldTerminate && appMessagesLocksMap.isEmpty());
    }
}
