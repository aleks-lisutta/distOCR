package Manager.Main;

import Manager.Protocol.*;
import Manager.Requests.Request;
import Manager.Requests.RequestUnknownException;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class Manager implements Runnable{


    private final ExecutorService executorService;
    private RequestSelector requestSelector;
    private final Supplier<Protocol> protocolFactory;

    public Manager(RequestSelector requestSelector, Supplier<Protocol> protocolFactory, int threadAmount){
        this.executorService =  Executors.newFixedThreadPool(threadAmount);
        this.requestSelector = requestSelector;
        this.protocolFactory = protocolFactory;
    }

    private void cleanExit() {
    }

    public void run(){
        boolean finished = false;
        while (!this.requestSelector.isClosed()) {
            Request request = requestSelector.getRequest();
            Protocol protocol = protocolFactory.get();
            try {
                if (request != null) {
                    System.out.println("request   " + request);
                    executorService.execute(protocol.process(request));
                }
            } catch (RequestUnknownException e) {
                e.printStackTrace();
                continue;
            } catch (NotifyFinishedException e) {
                System.out.println("Got a termination message from a client!");
                finished = true;
            }
            if (finished && protocol.shouldTerminate()) {
                System.out.println("Finished processing exit program.");
                this.requestSelector.close();
                break;
            }
        }
    }
}
