package Manager.Job;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class S3Storage implements DataStorageInterface {
    private String bucketName;
    private S3Client s3;
    private static final String idFileName= "ID-INFO.json";

    public S3Storage(String bucketName, S3Client s3) {
        this.s3 = s3;
        this.bucketName = bucketName;
    }

    public void uploadFile(String filePath ){


        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(this.bucketName).key(filePath).build();
        this.s3.putObject(request, RequestBody.fromFile(new File(filePath)));
    }

    public void S3DownloadFileExample( String key ) throws IOException {

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        ResponseInputStream<GetObjectResponse> response = s3.getObject(request);

        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(key));

        byte[] buffer = new byte[4096];
        int bytesRead = -1;

        while ((bytesRead = response.read(buffer)) !=  -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        response.close();
        outputStream.close();
    }


}
