package Manager.Main;

import Manager.Requests.Request;

import java.util.concurrent.LinkedBlockingQueue;

public class RequestSelector {
    private LinkedBlockingQueue<Request> allRequestsQueue;
    private boolean closed;

    public RequestSelector(){
        this.allRequestsQueue = new LinkedBlockingQueue<>();
        this.closed = false;
    }

    public boolean isClosed(){
        return this.closed;
    }

    public void close(){
        this.closed = true;
    }

    public boolean isEmpty(){
        return allRequestsQueue.isEmpty();
    }

    public Request getRequest(){
        return this.allRequestsQueue.poll();
    }

    public void putMessage(Request request){
        while (!this.isClosed()) {
            try {
                this.allRequestsQueue.put(request);
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
