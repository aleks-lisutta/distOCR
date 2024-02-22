import Manager.ManagerCreator;
import SQS.SQSClass;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


public class LocalApplication {
    static String managerName = "Manager_EC2";
    static String bucketName = "";
    static int n;
    static String outputFile = "";
    static boolean terminate = false;

    private static List <ResultEntry> resultsArray = new ArrayList<ResultEntry>() ;

    static class ResultEntry{
        public String job;
        public String inputLink;
        public String outputLink;
        public boolean hasFailed;

        public ResultEntry(String job, String inputLink, String outputLink, boolean hasFailed){
            this.job = job;
            this.inputLink = inputLink;
            this.outputLink = outputLink;
            this.hasFailed = hasFailed;
        }
        public String toString(){
            return "job  "+job+"  inputLink  "+ inputLink + "  outputLink   "+ outputLink +" HasFailed  "  + hasFailed;
        }
    }

    public static void createBucket(S3Client s3Client, String bucketName, Region region) {

        S3Waiter s3Waiter = s3Client.waiter();

        try {
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .createBucketConfiguration(
                            CreateBucketConfiguration.builder()
                                    .build())
                    .build();

            s3Client.createBucket(bucketRequest);
            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
            waiterResponse.matched().response().ifPresent(System.out::println);
            System.out.println(bucketName +" is ready");

        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }


    public static int fileLines(String path){


        File file = new File(path);

        int lineCount = 0;

        try {

            Scanner input = new Scanner(file);


            while (input.hasNextLine()) {
                input.nextLine();
                lineCount++;
            }


            input.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found");
        }


         return lineCount;

    }


    public static void main(String[] args) throws InterruptedException, IOException {
        long startTime = System.currentTimeMillis();
        Region region = Region.US_EAST_1;
        if(args.length < 3){
            System.out.println("Make sure to pass InputFile OutputFile n [terminate]");
            return;
        }
        String filePath = getFilePathOrTerminate(args[0]); //getfilepath to input-sample/txt
        outputFile = args[1];
        n = Integer.parseInt(args[2]);
        if(args.length > 3 && args[3].equals("terminate"))
            terminate = true;
        updateInfoFromConfig();

        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        createBucket(s3, bucketName, region);

    //    String jar = uploadFile("\\out\\artifacts\\BarAlexHamudi1_jar\\BarAlexHamudi1.jar", bucketName); //

        int urlNumber = fileLines(filePath);
        System.out.println(("before"));
        String fileKey = uploadFile(filePath, bucketName); //filekey = filename = input.txt
        System.out.println(("after"));
        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
        createManagerIfNeeded(n);
        SqsClient sqsClient = SqsClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();

        String outputURL = waitForQueue(sqsClient, "APP2MSQS");//L2M URL
        String inputURL = waitForQueue(sqsClient, "M2APPSQS");//M2L URL
        System.out.printf("Sending %s to outputSQS\n%n", fileKey);
        String id = SQSClass.sendMessageFromString(sqsClient, outputURL, fileKey);
        System.out.println("id - "+id);

        System.out.println("urlNumber "+ urlNumber);
        int i = 1;
        while(true) {
            List<Message> msgs = SQSClass.receiveMessages(sqsClient, inputURL);
            if(msgs!=null && !msgs.isEmpty())
                for(Message msg : msgs) {
                    System.out.println(" i   "+ i);
                    i = i+1;
                    String s = msg.body();
                    if (s.contains(id)) {
                        System.out.println("msg  " + msg.body());
                        parseMessage(msg.body().split(Pattern.quote("|"))[1] , bucketName ,s3 );
                        System.out.println("results array.size :  "+resultsArray.size());
                        SQSClass.deleteMessage(sqsClient, inputURL, msg);
                        if(terminate)
                            SQSClass.sendMessageFromString(sqsClient, outputURL, "terminate");
                    }
                }
            TimeUnit.SECONDS.sleep(1);
            if(resultsArray.size() == urlNumber )
            {
                System.out.println("finished!?!?!?");
                resultsArray.toString();
                createHtml(resultsArray);
                long endTime = System.currentTimeMillis();
                System.out.println("time" + (endTime-startTime));
                return;
            }
        }


    }



    public static void createHtml(List <ResultEntry> x) {
        for (ResultEntry i : x) {
            if (i != null) {
                System.out.println(i);
            }
            else
                System.out.println("i is null");
        }
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter( "result.html"));
            // Write the HTML content to the file
            bw.write("<!DOCTYPE html>");
            bw.newLine();
            bw.write("<html>");
            bw.newLine();
            bw.write("<head>");
            bw.newLine();
            bw.write("<title>My HTML File</title>");
            bw.newLine();
            bw.write("</head>");
            bw.newLine();
            bw.write("<body>");
            String y = x.get(0).inputLink;
            ListIterator<ResultEntry>  i = x.listIterator();
            while (i.hasNext()) {
                String m = i.next().inputLink;
                bw.newLine();
                bw.write("<img src=" + m + " alt=\"My Photo\">");
                bw.newLine();
                bw.write("<h1>" + m + "</h1>");
                bw.newLine();
            }
            bw.write("</body>");
            bw.newLine();
            bw.write("</html>");

            // Close the BufferedWriter
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateInfoFromConfig(){
        try (BufferedReader br = new BufferedReader(new FileReader("config.txt"))) {
            int line_counter = 0;
            String line;
            while ((line = br.readLine()) != null && line_counter < 2) {
                if(line_counter == 0)
                    ManagerCreator.credentialsPath = line;
                if(line_counter == 1)
                    bucketName = line;
                line_counter++;
            }
        } catch (IOException e) {
            System.out.println("Make sure you are using config.txt!");
            System.exit(1);
        }
    }

    public static String getInput(String output) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in));
        System.out.print(output);
        return reader.readLine();
    }

    public static String getFilePathOrTerminate(String filePath){
        File f = new File(filePath);
        if (!f.exists()) {
            System.out.printf("%s was not found!\n", filePath);
            System.exit(0);
        }
        return filePath;
    }

    public static ResultEntry[] parseResults(String id){

        String data = S3Helper.getFileData(id + "/ID-INFO.json");
        JsonObject json = Json.createReader(new StringReader(data)).readObject();
        if(json.get("files").toString().equals("[]"))
            return null;
        JsonArray files = (JsonArray) json.get("files");
        ResultEntry[] results = new ResultEntry[files.size()];
        for(int i = 0; i < files.size(); i++){
            JsonObject temp = (JsonObject) files.get(i);
            String output = temp.getString("output");
            String type = temp.getString("analysisType");
            String input = temp.getString("inputLink");
            results[i] = new ResultEntry(type, input, output, false);
        }
        return results;
    }


    public static void parseMessage(String fileName , String bucket , S3Client client) throws IOException {

        System.out.println("Bucket  " + bucket);
        System.out.println("file name"+ fileName);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();

        ResponseInputStream<GetObjectResponse> response = client.getObject(request);

        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(fileName));

        byte[] buffer = new byte[4096];
        int bytesRead = -1;

        while ((bytesRead = response.read(buffer)) !=  -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        response.close();
        outputStream.close();
        readResultFile(fileName);
    }


    public static void readResultFile(String fileName) throws FileNotFoundException {

        File file = new File(fileName);
        Scanner scanner = new Scanner(file);
        String data = scanner.nextLine();
        System.out.println(data);
        String[] msgs = data.split(Pattern.quote("!"));
        for( String msg : msgs)
        {
            String [] s = msg.split(Pattern.quote("|"));
            resultsArray.add( new ResultEntry(s[0],s[1],s[2],s[1].equals(s[2])));
        }

    }
    public static boolean isManagerOn( Ec2Client ec2){
        String nextToken = null;
        try {
            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().maxResults(6).nextToken(nextToken).build();
                DescribeInstancesResponse response = ec2.describeInstances(request);
                for (Reservation reservation : response.reservations())
                    for (Instance instance : reservation.instances())
                        if(instance.hasTags() && instance.state().name() == InstanceStateName.RUNNING)
                            for(Tag t : instance.tags())
                                if(t.key().equals("Name") && t.value().equals(managerName))
                                    return true;
                nextToken = response.nextToken();
            } while (nextToken != null);
        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return false;
    }

    private static void createManagerIfNeeded(int n) throws IOException {
        Ec2Client ec2 = Ec2Client.builder()
                .region(Region.US_EAST_1)
                .build();
        if (isManagerOn(ec2)){
            System.out.println("Manager EC2 found! No need to create a new one.");
            return;
        }
        System.out.println("Creating a Manager EC2 instance. This can take a while as we need to wait for the instance to start.");
        ManagerCreator.createManagerInstance(managerName, bucketName, n);
    }

    private static String waitForQueue(SqsClient sqsClient, String queueName) throws InterruptedException {
        try {
            System.out.printf("Waiting for %s... This might take a while...\n", queueName);
            String name = SQSClass.getQueueByName(sqsClient, queueName);
            while (name == null) {
                System.out.println("loop");
                TimeUnit.SECONDS.sleep(1);
                name = SQSClass.getQueueByName(sqsClient, queueName);
            }
            System.out.printf("%s is on!\n", queueName);
            return name;
        } catch (Exception e) {
            System.out.println(e.toString());
            return null;
        }
    }



    private static String uploadFile(String filePath, String bucketName){ //localuploadtos3
        // Split path either by '/' or by '\'
        String[] s = filePath.split("/|\\\\");
        String fileName = s[s.length - 1];

        if(!S3Helper.doesObjectExists(bucketName, fileName)){ //filename = input.txt
            System.out.println(("1"));
            S3Helper.putS3Object(bucketName, fileName, filePath);
            System.out.println(("2"));
            System.out.printf("Uploading %s succeeded!\n", fileName);
            return fileName;
        }
        else{
            int counter = 0;
            String tempFileName = String.format("%s%d", fileName, counter);
            while(S3Helper.doesObjectExists(bucketName, tempFileName)) {
                counter++;
                tempFileName = String.format("%s%d", fileName, counter);
            }
            S3Helper.putS3Object(bucketName, tempFileName, filePath);
            System.out.printf("Uploading %s succeeded under the name: %s!\n", fileName, tempFileName);
            return tempFileName;
        }
    }


}