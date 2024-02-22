package Manager.Main;

import Manager.Connection.ApplicationEncoderDecoder;
import Manager.Connection.SQSConnectionHandler;
import Manager.Connection.WorkersEncoderDecoder;
import Manager.Job.S3Storage;
import Manager.Job.WorkerExecutor;
import Manager.Protocol.AwsProtocol;
import SQS.SQSClass;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ManagerMain {
    static String bucketName = "";
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
         System.out.println("Missing arguments");
         return;
         }
        bucketName = args[0];
        int messagesPerWorker = Integer.parseInt(args[1]);
        RequestSelector requestSelector = new RequestSelector();

        String sendAppMessagesSQSName = "M2APPSQS";
        String getAppMessagesName = "APP2MSQS";
        String sendWorkerMessagesSQSName = "M2WSQS";
        String getWorkerMessagesName = "W2MSQS";
        System.out.println("Initializing Manager!");

        SqsClient sqsClient = SqsClient.builder()
                .build();
        S3Client s3Client = S3Client.builder()
                .build();



        SQSConnectionHandler appSQSConnectionHandler = new SQSConnectionHandler(
                new ApplicationEncoderDecoder(),
                requestSelector,
                sendAppMessagesSQSName,
                getAppMessagesName,
                sqsClient);
        SQSConnectionHandler workerSQSConnectionHandler = new SQSConnectionHandler(
                new WorkersEncoderDecoder(),
                requestSelector,
                sendWorkerMessagesSQSName,
                getWorkerMessagesName,
                sqsClient);

        System.out.println("sendWorkerMessagesSQSName  "+sendWorkerMessagesSQSName);
        System.out.println("getWorkerMessagesName   "+ getWorkerMessagesName);




        while(SQSClass.getQueueByName(sqsClient, sendWorkerMessagesSQSName) == null ||
                SQSClass.getQueueByName(sqsClient, getWorkerMessagesName) == null){
            System.out.println("Manager couldn't find queues. Sleeping for 5 secs");
            TimeUnit.SECONDS.sleep(5);
        }


        WorkerExecutor workerExecutor = new WorkerExecutor(sendWorkerMessagesSQSName, getWorkerMessagesName, sqsClient, messagesPerWorker, bucketName);
        S3Storage s3Storage = new S3Storage(bucketName, s3Client);
        Manager manager = new Manager(
                requestSelector,
                () -> new AwsProtocol(appSQSConnectionHandler, workerSQSConnectionHandler, workerExecutor, s3Storage),
                10);


        System.out.println("Starting manager applications listener loop!");

        Thread appConnectionThread = new Thread(appSQSConnectionHandler);
        Thread workerConnectionThread = new Thread(workerSQSConnectionHandler);
        Thread managerThread = new Thread(manager);
        appConnectionThread.start();
        workerConnectionThread.start();
        managerThread.start();

        try {
            managerThread.join();
            appConnectionThread.join();
            workerConnectionThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerExecutor.deleteJobExecutors();
        }


        System.out.println("Manager closed!");

        try {
            Runtime.getRuntime().exec("sudo shutdown -h now");
        } catch (IOException e) {
            System.err.println("Could not execute shutdown command due to:\n");
            e.printStackTrace();
        }


    }


}
