package br.com.orbitfire.hotspots.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsS3Config {

    private static final Logger log = LoggerFactory.getLogger(AwsS3Config.class);

    @Bean
    public S3Client s3Client(AwsProperties props) {
        return S3Client.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(credentialsProvider(props))
                .build();
    }

    private AwsCredentialsProvider credentialsProvider(AwsProperties props) {
        if (props.hasProfile()) {
            log.info("Using AWS profile credentials: {}", props.profile());
            return ProfileCredentialsProvider.create(props.profile());
        }
        log.info("Using AWS default credentials provider chain");
        return DefaultCredentialsProvider.builder().build();
    }
}
