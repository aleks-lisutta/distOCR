package Manager;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;


import java.io.*;
import java.util.Base64;


public class ManagerCreator {
    public static String credentialsPath = "";

    public static void main(String[] args) throws InterruptedException, IOException {
    }

    public static Ec2Client GetEc2(){
        Region region = Region.US_EAST_1;
        return Ec2Client.builder()
                .region(region)
                .build();
    }

    private static String getECuserData(String bucketName, int n) throws IOException {
        // Let's not touch this function, it is super sensitive :(

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
        userData = userData + "sudo aws s3 cp s3://alexbarhamudi123456/BarAlexHamudi-1.jar DistOcr.jar &>/dev/null\n";
        userData = userData + String.format("sudo java -cp DistOcr.jar Manager.Main.ManagerMain %s %d\n", bucketName, n);
        String base64UserData = null;
        try {
            base64UserData = new String( Base64.getEncoder().encode(userData.getBytes("UTF-8")), "UTF-8" );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return base64UserData;
    }

    public static void describeEC2Instances( Ec2Client ec2){

        boolean done = false;
        String nextToken = null;

        try {

            do {
                DescribeInstancesRequest request = DescribeInstancesRequest.builder().maxResults(6).nextToken(nextToken).build();
                DescribeInstancesResponse response = ec2.describeInstances(request);

                for (Reservation reservation : response.reservations()) {
                    for (Instance instance : reservation.instances()) {
                        System.out.println("Instance Id is " + instance.instanceId());
                        System.out.println("Image id is "+  instance.imageId());
                        System.out.println("Instance type is "+  instance.instanceType());
                        System.out.println("Instance state name is "+  instance.state().name());
                        System.out.println("monitoring information is "+  instance.monitoring().state());

                    }
                }
                nextToken = response.nextToken();
            } while (nextToken != null);

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public static void stopEC2Instance(Ec2Client ec2, String id) {
        StopInstancesRequest runRequest = StopInstancesRequest.builder().instanceIds(id).build();

        ec2.stopInstances(runRequest);
    }

    public static String getCredentials() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(credentialsPath));
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            return sb.toString();
        } finally {
            br.close();
        }
    }

    public static String createManagerInstance(String name, String bucketName, int n) throws IOException {

        String amiId = "ami-0b0dcb5067f052a63";
        Ec2Client ec2 = GetEc2();
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(amiId)
                .instanceType(InstanceType.T2_MICRO)
                .userData(getECuserData(bucketName, n))
                .maxCount(1)
                .minCount(1)
                .instanceInitiatedShutdownBehavior(ShutdownBehavior.TERMINATE)
                .iamInstanceProfile(IamInstanceProfileSpecification.builder().name("LabInstanceProfile").build())
                .build();
        System.out.println("runRequest  "+ runRequest);
        System.out.println("Ec2  "+ ec2);
        RunInstancesResponse response = ec2.runInstances(runRequest);
        String instanceId = response.instances().get(0).instanceId();

        Tag tag = Tag.builder()
                .key("Name")
                .value("Manager")
                .build();



        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();

        try {
            ec2.createTags(tagRequest);
            System.out.printf(
                    "Successfully started EC2 Instance %s based on AMI %s\n",
                    instanceId, amiId);

            return instanceId;

        } catch (Ec2Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

        return "";
    }
}
