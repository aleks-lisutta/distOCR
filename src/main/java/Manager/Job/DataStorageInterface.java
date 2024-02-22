package Manager.Job;

import java.io.IOException;

public interface DataStorageInterface {

    void S3DownloadFileExample(String data) throws IOException;

    public void uploadFile(String filePath );
    }
