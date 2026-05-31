package br.com.orbitfire.hotspots.infrastructure.aws.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.com.orbitfire.hotspots.infrastructure.config.AwsProperties;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

/**
 * Generic reader for raw objects in the configured S3 bucket.
 *
 * <p>Knows nothing about CSV or metadata semantics — it only fetches bytes by
 * key. Parsing and domain interpretation live in higher layers.
 */
@Component
public class S3ObjectReader {

    private static final Logger log = LoggerFactory.getLogger(S3ObjectReader.class);

    private final S3Client s3Client;
    private final String bucket;

    public S3ObjectReader(S3Client s3Client, AwsProperties props) {
        this.s3Client = s3Client;
        this.bucket = props.s3().bucketName();
    }

    /**
     * Reads the full object at {@code key} as raw bytes.
     *
     * @throws S3ObjectNotFoundException if no object exists at that key
     */
    public byte[] readBytes(String key) {
        log.debug("Reading s3://{}/{}", bucket, key);
        try {
            return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()).asByteArray();
        } catch (NoSuchKeyException e) {
            throw new S3ObjectNotFoundException(bucket, key, e);
        }
    }
}
