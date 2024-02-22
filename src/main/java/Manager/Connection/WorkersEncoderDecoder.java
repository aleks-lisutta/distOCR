package Manager.Connection;

import Manager.Requests.ManagerToWorkerRequest;
import Manager.Requests.Request;
import Manager.Requests.RequestUnknownException;
import Manager.Requests.WorkerToManagerRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.Map;
import java.util.regex.Pattern;

public class WorkersEncoderDecoder extends EncoderDecoder<Map.Entry<String, String>, String[]> {

    @Override
    public String encode(Request<Map.Entry<String, String>> request) throws RequestUnknownException {
        if (!(request instanceof ManagerToWorkerRequest)){
            throw new RequestUnknownException();
        }
        return
                request.getData().getKey().toString() + "|" + request.getData().getValue();
    }

    @Override
    public Request<String[]> decode(Message message) throws RequestUnknownException {

        String[] strings = message.body().split(Pattern.quote("|"));
        if (strings.length != 3){
            throw new RequestUnknownException();
        }
        WorkerToManagerRequest request = new WorkerToManagerRequest();
        request.setData(strings);
        return request;
    }

}
