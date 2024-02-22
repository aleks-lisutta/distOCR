import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
// snippet-end:[s3.java2.s3_object_upload.import]

/**
 * To run this AWS code example, ensure that you have setup your development environment, including your AWS credentials.
 *
 * For information, see this documentation topic:
 *
 * https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/get-started.html
 */

public class S3Helper {

    public static String getFileData(String filePath) {
        try {
            Region region = Region.US_EAST_1;
            S3Client s3 = S3Client.builder()
                    .region(region)
                    .build();
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(filePath)
                    .bucket(LocalApplication.bucketName)
                    .build();
            ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
            byte[] data = objectBytes.asByteArray();
            return new String(data, StandardCharsets.UTF_8);
        } catch (S3Exception e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
        return "";
    }


    public static int getFilesNum(String bucketName, String prefix){
        int sum = 0;
        S3Client client = S3Client.builder().region(Region.US_EAST_1).build();
        ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix).build();
        ListObjectsV2Iterable response = client.listObjectsV2Paginator(request);
        for (ListObjectsV2Response page : response) {
            sum += page.contents().size();
        }
        return sum;
    }

    public static String putS3Object(String bucketName, String objectKey,String objectPath) {
        Region region = Region.US_EAST_1;
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();

        try {

            Map<String, String> metadata = new HashMap<>();
            metadata.put("x-amz-meta-myVal", "test");
            System.out.println(("1.1"));
            PutObjectRequest putOb = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .metadata(metadata)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();
            System.out.println(("1.2"));
            byte[] s = getObjectFile(objectPath);
            s3.putObject(putOb, RequestBody.fromBytes(s));s3.close();
            System.out.println(("1.3"));
            GetUrlRequest request = GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            System.out.println(("1.4"));
            URL url = s3.utilities().getUrl(request);
            return url.toString();

        } catch (S3Exception e) {
            System.out.println(("1.5"));
            s3.close();
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return "";
    }

    public static boolean doesObjectExists(String bucketName, String objectKey){
        Region region = Region.US_EAST_1;
        S3Client s3 = S3Client.builder()
                .region(region)
                .build();
        try {
            HeadObjectRequest objectRequest = HeadObjectRequest.builder()
                    .key(objectKey)
                    .bucket(bucketName)
                    .build();

            HeadObjectResponse objectHead = s3.headObject(objectRequest);
            String type = objectHead.contentType();
            return true;
        } catch (S3Exception e) {
            return false;
        }

    }
    // Return a byte array
    private static byte[] getObjectFile(String filePath) {

        FileInputStream fileInputStream = null;
        byte[] bytesArray = null;

        try {
            File file = new File(filePath);
            bytesArray = new byte[(int) file.length()];
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bytesArray);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bytesArray;
    }
    // snippet-end:[s3.java2.s3_object_upload.main]
}