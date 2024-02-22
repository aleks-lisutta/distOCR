package Manager.Connection;

import Manager.Main.RequestSelector;
import Manager.Requests.Request;
import Manager.Requests.RequestUnknownException;
import SQS.SQSClass;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

public class SQSConnectionHandler extends ConnectionHandler {

    private EncoderDecoder encoderDecoder;
    private String sendMessageUrl;
    private String getMessageUrl;
    private SqsClient sqsClient;
    private RequestSelector requestSelector;

    public SQSConnectionHandler(EncoderDecoder encoderDecoder, RequestSelector requestSelector, String sendMessageName, String getMessageName, SqsClient sqsClient) {
        this.encoderDecoder = encoderDecoder;
        this.sqsClient = sqsClient;
        this.sendMessageUrl = this.getQueueUrl(sendMessageName);
        this.getMessageUrl = this.getQueueUrl(getMessageName);
        this.requestSelector = requestSelector;
    }

    private String getQueueUrl(String queueName) {
        String queueUrl = SQSClass.getQueueByName(this.sqsClient, queueName);
        if (queueUrl == null) {
            queueUrl = SQSClass.createQueue(this.sqsClient, queueName);

        }
        return queueUrl;
    }

    @Override
    public String sendMessage(Request request) throws RequestUnknownException {
            try {
                System.out.println("body   " +request.getData());
                String message = this.encoderDecoder.encode(request);
                System.out.println("Sending message to SQS: " + this.sendMessageUrl);
                return SQSClass.sendMessageFromString(this.sqsClient, this.sendMessageUrl, message);
            } catch (RequestUnknownException | Exception e){
                e.printStackTrace();
            }
        return null;
    }

    @Override
    public void listener() {

        while (true) {
                if (Thread.interrupted() || this.requestSelector.isClosed()){
                    this.terminate();
                    return;
                }
                System.out.println("this.getMessageUrl   " +this.getMessageUrl );
            System.out.println("this.sqsClient   " +this.sqsClient );
                List<Message> appMessages = SQSClass.receiveOneMessage(this.sqsClient, this.getMessageUrl);
                if (appMessages == null){
                    return;
                }
                if (appMessages.isEmpty()){
                    continue;
                }
            System.out.println("recived message!!!  " + appMessages + "Quoue name  "+this.getMessageUrl);
            Request<String> req = null;
                try {
                    req = this.encoderDecoder.decode(appMessages.get(0));
                } catch (RequestUnknownException e) {
                    continue;
                }
                if (req != null) {
                    System.out.println("Getting message from SQS: " + this.getMessageUrl);
                    SQSClass.deleteMessages(this.sqsClient, this.getMessageUrl, appMessages);
                    this.requestSelector.putMessage(req);
                }
            }
    }

    public void terminate(){
        SQSClass.deleteQueue(sqsClient, this.sendMessageUrl);
        SQSClass.deleteQueue(sqsClient, this.getMessageUrl);
    }

    @Override
    public void run() {
        this.listener();
    }
}
