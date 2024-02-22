import com.asprise.ocr.Ocr;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;






class Msg {
    public String container;

    public String inputLink;

    private Msg(String container,  String inputLink){
        this.container = container;
        this.inputLink = inputLink;
    }

    public static Msg parseMsg(String st) throws Exception {
        // Assuming that the message will be MSGID|JOB|INPUTLINK

        String[] result = st.split("[|]");
        if(result.length != 2){
            throw new Exception("Message is of an unknown pattern");
        }
        String id = result[0];
        String inputLink = result[1];
        return new Msg( id, inputLink);
    }
}

public class Worker {

    static String inputSQS;
    static String outputSQS;
    static String bucketName;
    static String messageId;
    static String receiptHandle;


    public static String downloadImage(Msg m )
    {
        String savedPath = null;
        BufferedImage image =null;
        try{

            URL url =new URL(m.inputLink);
            image = ImageIO.read(url);
            savedPath = String.format("%s.png",messageId);
            ImageIO.write(image, "png",new File(savedPath));

        }catch(IOException e){
            e.printStackTrace();
        }
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        return savedPath;

    }


    public static String ocrImage(URL[] imgPath)
    {
       try {
            Ocr.setUp();
            Ocr ocr = new Ocr();
            ocr.startEngine("eng", Ocr.SPEED_FASTEST); // English
            String output = ocr.recognize(imgPath ,
                    Ocr.RECOGNIZE_TYPE_TEXT , Ocr.OUTPUT_FORMAT_PLAINTEXT);
             System.out.println("Result: " + output);
            ocr.stopEngine();
           return output;
        }catch (Exception e) {
           return e.getMessage();
        }
    }




    public static void main(String[] args) throws Exception {
        if(args.length < 3)
            return;

        Worker.inputSQS = args[0];
        Worker.outputSQS = args[1];
        Worker.bucketName = args[2];
        SqsClient sqsClient = SqsClient.builder()
                .region(Region.US_EAST_1)
                .build();
        while(true) {
            String msg = getMsg(sqsClient);
            if(!msg.equals("")) {
                Msg m = Msg.parseMsg(msg);
                String answer;
                answer = process(m);
                sendResult(answer, sqsClient, m);
                deleteMessage(sqsClient);

            }
            else{
                TimeUnit.SECONDS.sleep(5);
            }
        }
    }


    private static void deleteMessage(SqsClient sqsClient){
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(inputSQS)
                .receiptHandle(Worker.receiptHandle)
                .build();
        sqsClient.deleteMessage(deleteMessageRequest);
        System.out.println("delete message!!!!!");
    }

    private static void sendResult(String resultURL, SqsClient sqsClient, Msg m){
        String returnMsg = String.format("%s|%s|%s", m.container, m.inputLink, resultURL);
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(outputSQS)
                .messageBody(returnMsg).build();
        sqsClient.sendMessage(sendMessageRequest);
        System.out.printf("Result was sent to: %s\n", resultURL);
    }

    private static String getMsg(SqsClient sqsClient){

        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(inputSQS)
                .maxNumberOfMessages(1)
                .build();

        List<Message> messages = sqsClient.receiveMessage(receiveMessageRequest).messages();
        if(messages.isEmpty())
            return "";
        Worker.messageId = messages.get(0).messageId();
        Worker.receiptHandle = messages.get(0).receiptHandle();
        String body = messages.get(0).body();
        System.out.printf("Received msg!\nBody: %s\n%n", body);
        ChangeVisibility(sqsClient, receiptHandle, 1800);
        return messages.get(0).body();
    }

    private static void ChangeVisibility(SqsClient sqsClient, String receiptHandle, int timeout){
        try {
            ChangeMessageVisibilityRequest visibilityRequest = ChangeMessageVisibilityRequest.builder()
                    .queueUrl(inputSQS)
                    .receiptHandle(receiptHandle)
                    .visibilityTimeout(timeout)
                    .build();
            sqsClient.changeMessageVisibility(visibilityRequest);

        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }

    }
    private static String process(Msg m) throws IOException {
        String errMsg;
        URL[] urls = new URL[1] ;
        URL url = new URL(m.inputLink);
        urls[0] = url;
        String resultPath = String.format("%s_result.txt", messageId);
        File resultFile = new File(resultPath);
        if(!resultFile.createNewFile()){
            errMsg = String.format("File named %s already exists", resultFile);
            System.out.println(errMsg);
            return errMsg;
        }
        System.out.println("File path  "+ urls[0] );
        System.out.printf("Processing the message into file: %s\n", resultPath);

        String result = urls[0].toString();
        System.out.println("result::   "+result);
        System.out.println("Processing Finished!");
        String ObjectKey = String.format("%s/%s.txt", m.container, messageId);
        if(result.equals("1"))
            return S3Helper.putS3Object(Worker.bucketName, ObjectKey, resultPath);
        else
            return result;
    }


    private static String saveFileFromWeb(Msg m) throws IOException {
        try {
            URL url = new URL(m.inputLink);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(url.openStream()));

            StringBuilder stringBuilder = new StringBuilder();

            String inputLine;
            while ((inputLine = bufferedReader.readLine()) != null) {
                stringBuilder.append(inputLine);
                stringBuilder.append(System.lineSeparator());
            }

            bufferedReader.close();
            String savedPath = String.format("%s.txt", messageId);
            try (PrintWriter out = new PrintWriter(savedPath)) {
                out.println(stringBuilder.toString());
            }
            return savedPath;
        }
        catch (Exception e) {
            return null;
        }
    }
}
