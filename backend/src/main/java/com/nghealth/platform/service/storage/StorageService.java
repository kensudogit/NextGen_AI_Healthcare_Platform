package com.nghealth.platform.service.storage;

import com.nghealth.platform.config.AppProperties;
import com.nghealth.platform.config.AwsProperties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class StorageService {

    private final AppProperties appProperties;
    private final AwsProperties awsProperties;
    private final S3Client s3Client;

    public StorageService(AppProperties appProperties, AwsProperties awsProperties) {
        this.appProperties = appProperties;
        this.awsProperties = awsProperties;
        this.s3Client = buildS3Client();
        ensureBucket();
        ensureLocalDir();
    }

    public String storeDicom(byte[] data, String key) throws IOException {
        if (appProperties.storage().useS3()) {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(awsProperties.s3Bucket()).key(key).build(),
                    RequestBody.fromBytes(data));
            return "s3://" + awsProperties.s3Bucket() + "/" + key;
        }
        Path path = localPath(key);
        Files.createDirectories(path.getParent());
        Files.write(path, data);
        return path.toString();
    }

    public byte[] readDicom(String keyOrPath) throws IOException {
        if (keyOrPath.startsWith("s3://")) {
            String key = keyOrPath.substring(keyOrPath.indexOf('/', 5) + 1);
            return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(awsProperties.s3Bucket()).key(key).build()).asByteArray();
        }
        return Files.readAllBytes(Paths.get(keyOrPath));
    }

    public Path localPath(String key) {
        return Paths.get(appProperties.storage().localPath(), key);
    }

    public String previewKey(String dicomKey) {
        return dicomKey + ".png";
    }

    public void storePreview(byte[] png, String key) throws IOException {
        if (appProperties.storage().useS3()) {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(awsProperties.s3Bucket())
                            .key(key)
                            .contentType("image/png")
                            .build(),
                    RequestBody.fromBytes(png));
        } else {
            Path path = localPath(key);
            Files.createDirectories(path.getParent());
            Files.write(path, png);
        }
    }

    public byte[] readPreview(String keyOrPath) throws IOException {
        if (appProperties.storage().useS3()) {
            return s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(awsProperties.s3Bucket()).key(keyOrPath).build()).asByteArray();
        }
        return Files.readAllBytes(Paths.get(keyOrPath));
    }

    public boolean previewExists(String keyOrPath) {
        if (appProperties.storage().useS3()) {
            try {
                s3Client.headObject(b -> b.bucket(awsProperties.s3Bucket()).key(keyOrPath));
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return Files.exists(Paths.get(keyOrPath));
    }

    private S3Client buildS3Client() {
        if (!appProperties.storage().useS3()) {
            return null;
        }
        S3ClientBuilder builder = S3Client.builder().region(Region.of(awsProperties.region()));
        if (awsProperties.endpoint() != null && !awsProperties.endpoint().isBlank()) {
            builder.endpointOverride(URI.create(awsProperties.endpoint()))
                    .forcePathStyle(true);
        }
        if (awsProperties.accessKey() != null && !awsProperties.accessKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsProperties.accessKey(), awsProperties.secretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    private void ensureBucket() {
        if (!appProperties.storage().useS3() || s3Client == null) {
            return;
        }
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(awsProperties.s3Bucket()).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(awsProperties.s3Bucket()).build());
        }
    }

    private void ensureLocalDir() {
        try {
            Files.createDirectories(Paths.get(appProperties.storage().localPath()));
        } catch (IOException ignored) {
        }
    }
}
