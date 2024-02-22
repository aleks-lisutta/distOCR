package Manager.Connection;

import Manager.Requests.AppToManagerRequest;
import Manager.Requests.ManagerToAppRequest;
import Manager.Requests.Request;
import Manager.Requests.RequestUnknownException;
import software.amazon.awssdk.services.sqs.model.Message;

import java.net.MalformedURLException;
import java.net.URL;

public class ApplicationEncoderDecoder extends EncoderDecoder<String, String> {

    @Override
    public String encode(Request<String> request) throws RequestUnknownException {
        if (!(request instanceof  ManagerToAppRequest)){
            throw new RequestUnknownException();
        }
        return request.getData();
    }

    @Override
    public Request<String> decode(Message message) {
        AppToManagerRequest appToManagerRequest = new AppToManagerRequest();
        appToManagerRequest.setId(message.messageId());

            if (message.body().equals("terminate")){
                appToManagerRequest.setTerminationMessage(true);
            } else {
                appToManagerRequest.setData(message.body());
            }
        return appToManagerRequest;
    }
}
