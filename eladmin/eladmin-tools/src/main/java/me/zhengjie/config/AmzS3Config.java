package me.zhengjie.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import java.net.URI;

/**
 * @author Zheng Jie
 * @date 2025-06-19
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "amz.s3")
public class AmzS3Config {

	/**
	 * Amazon S3 region configuration, e.g. "us-west-2".
	 * This region determines the geographical location of the S3 bucket.
	 */
	private String region;

	/**
	 * Amazon S3 endpoint URL
	 * This endpoint is used to access S3 services.
	 */
	private String endPoint;

	/**
	 * Amazon S3 domain name
	 * This domain name is used to construct the full URL for accessing S3 services.
	 */
	private String domain;

	/**
	 * Amazon S3 access key ID, used for authentication.
	 * This key is used together with secretKey to authorize access to S3 services.
	 */
	private String accessKey;

	/**
	 * Amazon S3 secret access key, used for authentication.
	 * This key is used together with accessKey to authorize access to S3 services.
	 */
	private String secretKey;

	/**
	 * Default S3 bucket name.
	 * This bucket is used to store uploaded files and data.
	 */
	private String defaultBucket;

	/**
	 * Folder format for storing uploaded files, defaults to "yyyy-MM".
	 */
	private String timeformat;

	/**
	 * Create and return an AmazonS3 client instance.
	 * Uses endPoint, region, accessKey, and secretKey from current configuration class.
	 * Declared as @Bean, Spring manages it as a singleton and injects it automatically when needed.
	 *
	 * @return Configured AmazonS3 client instance
	 */
	@Bean
	public S3Client amazonS3Client() {
		return S3Client.builder().region(Region.of(region))
				.endpointOverride(URI.create(endPoint))
				.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
				.build();
	}
}