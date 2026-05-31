package br.com.orbitfire.hotspots.infrastructure.aws.s3;

/**
 * Raised when an expected object is missing from S3 (e.g. metadata or a CSV for
 * a period that has not been ingested yet). Mapped to HTTP 404 by the API layer.
 */
public class S3ObjectNotFoundException extends RuntimeException {

    public S3ObjectNotFoundException(String bucket, String key, Throwable cause) {
        super("Object not found: s3://%s/%s".formatted(bucket, key), cause);
    }
}
