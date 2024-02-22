package SQS;

import software.amazon.awssdk.core.exception.AbortedException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SQSClass {

    public static String managerToAppQueueName = "managerToAppQueue";
    public static String appToManagerQueueName = "appToManagerQueue";
    public static String managerToWorkerQueueName = "managerToWorkerQueue";
    public static String workerToManagerQueueName = "workerToManagerQueue";
    public static String terminateMessage = "Terminate";


    public static String createQueue(SqsClient sqsClient,String queueName ) {

        try {
            System.out.println("\nCreate Queue");

            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .build();

            sqsClient.createQueue(createQueueRequest);

            System.out.println("\nGet queue url");

            GetQueueUrlResponse getQueueUrlResponse =
                    sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build());
            String queueUrl = getQueueUrlResponse.queueUrl();
            return queueUrl;

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
    }



    public static String getQueueByName(SqsClient sqsClient, String queueName) { // returns a queueURL
        ListQueuesRequest filterListRequest = ListQueuesRequest.builder()
                .queueNamePrefix(queueName).build();
        ListQueuesResponse listQueuesFilteredResponse = sqsClient.listQueues(filterListRequest);
        if (listQueuesFilteredResponse.hasQueueUrls()) {
            return listQueuesFilteredResponse.queueUrls().get(0);
        } else {
            return null;
        }
    }

    public static String sendMessageFromString(SqsClient sqsClient, String queueUrl, String messageString) {
            SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(messageString).build();
            return sqsClient.sendMessage(sendMessageRequest).messageId();
    }


    public static  List<Message> receiveMessages(SqsClient sqsClient, String queueUrl) {
        try {
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .build();
            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
            return messages;
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
        }
        return null;
    }


    public static void deleteMessages(SqsClient sqsClient, String queueUrl,  List<Message> messages) {
        System.out.println("\nDelete Messages");

        try {
            for (Message message : messages) {
                DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(message.receiptHandle())
                        .build();
                sqsClient.deleteMessage(deleteMessageRequest);
            }

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }

    public static void deleteMessage(SqsClient sqsClient, String queueURL, Message message){
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueURL)
                .receiptHandle(message.receiptHandle())
                .build();
        sqsClient.deleteMessage(deleteMessageRequest);
    }

    public static List<Message> receiveOneMessage(SqsClient sqsClient, String queueUrl) {


        try {
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1)
                    .build();
            List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
            return messages;
        } catch (AbortedException e){
            return null;
        }catch (SqsException e) {
            //e.printStackTrace();
            return null;
        }
    }


    public static int getNumberOfMessagesInQueue(SqsClient sqsClient, String queueUrl){
        List<QueueAttributeName> atts = new ArrayList();
        atts.add(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
        atts.add(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE);
        atts.add(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_DELAYED);

        GetQueueAttributesRequest attributesRequest= GetQueueAttributesRequest.builder()
                .queueUrl(queueUrl)
                .attributeNames(atts)
                .build();

        GetQueueAttributesResponse response = sqsClient.getQueueAttributes(attributesRequest);

        int requestsNum = 0;
        for (Map.Entry attr : response.attributesAsStrings().entrySet()){
            requestsNum+=Integer.parseInt(attr.getValue().toString());
        }

        return requestsNum;
    }
    public static void deleteQueue(SqsClient sqsClient, String queueUrl){
        try {
            DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                    .queueUrl(queueUrl)
                    .build();
            DeleteQueueResponse deleteQueueResponse = sqsClient.deleteQueue(deleteQueueRequest);
        } catch (Exception e){
            e.printStackTrace();
        }
    }


}