package Manager.Protocol;

import Manager.Requests.RequestUnknownException;

public abstract class Protocol<T> {
    public abstract Runnable process(T req) throws RequestUnknownException, NotifyFinishedException;
    public abstract boolean shouldTerminate();
}
