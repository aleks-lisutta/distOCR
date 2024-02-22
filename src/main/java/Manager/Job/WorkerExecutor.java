package Manager.Job;

import SQS.SQSClass;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class WorkerExecutor implements JobExecutor {
    private Ec2Client ec2;
    private String amiId;
    private String inputSQS;
    private String outputSQS;
    private int messagesPerWorker;
    private SqsClient sqsClient;
    private String bucketName;
    private static Tag workerTag = Tag.builder()
            .key("Name")
            .value("Worker")
            .build();

    public WorkerExecutor(String inputSQS, String outputSQS, SqsClient sqsClient, int messagesPerWorker, String bucketName) {
        this.bucketName = bucketName;
        this.sqsClient = sqsClient;
        this.messagesPerWorker = messagesPerWorker;
        this.ec2 = GetEc2();
        this.amiId = "ami-0b0dcb5067f052a63";
        this.inputSQS = SQS.SQSClass.getQueueByName(sqsClient, inputSQS);
        this.outputSQS = SQS.SQSClass.getQueueByName(sqsClient, outputSQS);
    }

    private Ec2Client GetEc2() {
        Region region = Region.US_EAST_1;
        return Ec2Client.builder()
                .region(region)
                .build();
    }

    private String getECuserData(String inputSQS, String outputSQS) throws IOException {

        String userData = "";
        userData = userData + "#!/bin/bash" + "\n";
          userData = userData + "cd /home/ec2-user\n";
       userData = userData + "pwd\n";
        userData = userData + "mkdir ~/.aws\n";
        userData = userData + "cd ~/.aws\n";
        userData = userData + "pwd\n";
       userData = userData + "cd -\n";
       userData = userData + "pwd\n";
       userData = userData + "cp -rf ~/.aws . &>/dev/null\n";
        userData = userData + "wget https://download.oracle.com/java/19/latest/jdk-19_linux-x64_bin.rpm &>/dev/null\n";
        userData = userData + "sudo rpm -Uvh jdk-19_linux-x64_bin.rpm &>/dev/null\n";
        userData = userData + "sudo aws s3 cp s3://alexbarhamudi123456/BarAlexHamudi-1.jar bar.jar &>/dev/null\n";
        userData = userData + String.format("sudo java -cp bar.jar Worker %s %s %s\n", inputSQS, outputSQS, this.bucketName);
        String base64UserData = null;
        try {
            base64UserData = new String( Base64.getEncoder().encode(userData.getBytes("UTF-8")), "UTF-8" );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return base64UserData;
    }

    private int getNumberOfWorkers(){
        int workersNum = 0;
            try {
                String nextToken = null;

                do {
                    Filter filter = Filter.builder()
                            .name("instance-state-name")
                            .values("running")
                            .build();

                    DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                            .filters(filter)
                            .build();

                    DescribeInstancesResponse response = ec2.describeInstances(request);

                    for (Reservation reservation : response.reservations()) {
                        for (Instance instance : reservation.instances()) {
                            if (instance.tags().contains(this.workerTag)){
                                workersNum++;
                            }
                        }
                    }
                    nextToken = response.nextToken();

                } while (nextToken != null);

            } catch (Ec2Exception e) {
                System.err.println(e.awsErrorDetails().errorMessage());
            }
        return workersNum;
    }
    public static void describeEC2Instances(Ec2Client ec2) {

        boolean done = false;
        String nextToken = null;

        try {

            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().maxResults(6).nextToken(nextToken).build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        System.out.println("Instance Id is " + instance.instanceId());
                        System.out.println("Image id is " + instance.imageId());
                        System.out.println("Instance type is " + instance.instanceType());
                        System.out.println("Instance state name is " + instance.state().name());
                        System.out.println("monitoring information is " + instance.monitoring().state());

                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    @Override
    public void deleteJobExecutor(String id) {
        StopInstancesRequest runRequest = StopInstancesRequest.builder().instanceIds(id).build();
        this.ec2.stopInstances(runRequest);
    }

    @Override
    public synchronized void createWorkers() {
        System.out.println("making workers");
        int currWorkers = this.getNumberOfWorkers();
        float workersNeeded = SQSClass.getNumberOfMessagesInQueue(sqsClient, inputSQS) / this.messagesPerWorker;
        if (currWorkers < workersNeeded){
            for (int i = currWorkers; i < workersNeeded && i <= 10; i++){
                this.createJobExecutor();
            }

        }
        while (this.getNumberOfWorkers() < workersNeeded){
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deleteJobExecutors() {
        try {
            String nextToken = null;

            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder()
                        .build();

                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        for(Tag t : instance.tags())
                            if(t.key().equals("Name") && t.value().equals("Worker")) {
                                this.deleteJobExecutor(instance.instanceId());
                        }
                    }
                }
                nextToken = response.nextToken();

            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }

        System.out.println("Deleted all job executors.");
    }



    @Override
    public String createJobExecutor() {

        RunInstancesRequest runRequest = null;
        try {
            runRequest =  RunInstancesRequest.builder()
                    .imageId(this.amiId)
                    .instanceType(InstanceType.T2_MICRO)
                    .userData(getECuserData(this.inputSQS, this.outputSQS))
                    .maxCount(1)
                    .minCount(1)
                    .iamInstanceProfile(IamInstanceProfileSpecification.builder().name("LabInstanceProfile").build())
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }

        RunInstancesResponse response = this.ec2.runInstances(runRequest);
        String instanceId = response.instances().get(0).instanceId();

        Tag tag = Tag.builder()
                .key("Name")
                .value("Worker")
                .build();

        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 Instance %s based on AMI %s\n",
                    instanceId, this.amiId);

            return instanceId;

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

        return "";
    }
}
