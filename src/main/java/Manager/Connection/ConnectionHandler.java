package Manager.Connection;

import Manager.Requests.Request;
import Manager.Requests.RequestUnknownException;

public abstract class ConnectionHandler implements Runnable {

    public abstract void listener();
    public abstract String sendMessage(Request request) throws RequestUnknownException;
    public abstract void terminate();
}
