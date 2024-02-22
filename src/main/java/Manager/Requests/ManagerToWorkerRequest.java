package Manager.Requests;

import java.util.Map;

public class ManagerToWorkerRequest extends Request<Map.Entry<String, String>> {

    private String appMessageId;

    public String getAppMessageId() {
        return this.appMessageId;
    }

    public void setAppMessageId(String id) {
        this.appMessageId = id;
    }
}
