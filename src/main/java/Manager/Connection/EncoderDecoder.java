package Manager.Connection;

import Manager.Requests.Request;
import Manager.Requests.RequestUnknownException;
import software.amazon.awssdk.services.sqs.model.Message;

public abstract class EncoderDecoder<T, V> {
    public abstract String encode(Request<T> request) throws RequestUnknownException;
    public abstract Request<V> decode(Message message) throws RequestUnknownException;
}
