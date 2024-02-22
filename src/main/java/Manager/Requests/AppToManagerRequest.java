package Manager.Requests;

import java.net.URL;

/**
 * The Data here represents the request from the Local application.
 * Each file and analysing method is represented as a Pair<String htmlFile, String analysingMethod>.
 * The request is a list of the pairs described above.
 */
public class AppToManagerRequest extends Request<String> {

    private boolean terminationMessage;
    private String id;

    public void setTerminationMessage(boolean terminationMessage) {
        this.terminationMessage = terminationMessage;
    }

    public void setId(String id){
        this.id = id;
    }

    public String getId(){
        return this.id;
    }

    public boolean isTermination() {
        return this.terminationMessage;
    }
}
