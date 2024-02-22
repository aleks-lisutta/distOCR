package Manager.Requests;


import software.amazon.awssdk.services.sqs.model.Message;

public abstract class Request<T> {

    protected T data;

    public void setData(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}
