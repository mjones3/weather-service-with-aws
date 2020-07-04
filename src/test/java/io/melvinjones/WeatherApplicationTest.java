package io.melvinjones;

import cloud.localstack.TestUtils;
import cloud.localstack.docker.LocalstackDockerExtension;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import groovy.util.logging.Slf4j;
import io.melvinjones.weatherservice.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.ByteBuffer;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ExtendWith(LocalstackDockerExtension.class)
@Slf4j
@SpringBootTest(classes = WeatherApplication.class)
@ContextConfiguration(classes = WeatherApplicationTest.WeatherContextConfiguration.class, loader = AnnotationConfigContextLoader.class)
@TestPropertySource("classpath:application-test.properties")
@Testcontainers
public class WeatherApplicationTest {

    private static final Logger log = LoggerFactory.getLogger(WeatherApplicationTest.class);

    @ClassRule
    public static final LocalStackContainer localstack = new LocalStackContainer()
            .withServices(LocalStackContainer.Service.KINESIS,
                    LocalStackContainer.Service.S3,
                    LocalStackContainer.Service.DYNAMODB,
                    LocalStackContainer.Service.CLOUDWATCH);


    private static AmazonS3 s3;

    @Autowired
    WeatherApplication weatherApplication;

    @Autowired
    WeatherProperties properties;

    private static AmazonKinesis kinesisClient;

    @Configuration
    static class WeatherContextConfiguration {

        @Bean
        public WeatherUtils weatherUtils() {

            WeatherUtils weatherUtils = new WeatherUtils();
            s3 = AmazonS3ClientBuilder.standard().
                    withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.S3)).
                    withCredentials(localstack.getDefaultCredentialsProvider())
                    .build();

            weatherUtils.setS3Client(s3);
            return weatherUtils;
        }

        @Bean
        public WeatherServiceManager weatherServiceManager() {

            TestUtils.setEnv("AWS_CBOR_DISABLE", "1");

            kinesisClient = AmazonKinesisClientBuilder
                    .standard()
                    .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.KINESIS))
                    .withCredentials(localstack.getDefaultCredentialsProvider())
                    .build();

            kinesisClient.createStream("weather-requests", 1);

            String workerId = UUID.randomUUID().toString();

            KinesisClientLibConfiguration kinesisClientLibConfiguration =
                    new KinesisClientLibConfiguration("weather11",
                            "weather-requests",
                            localstack.getDefaultCredentialsProvider(),
                            workerId)
                            .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);


            AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder
                    .standard()
                    .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.DYNAMODB))
                    .withCredentials(localstack.getDefaultCredentialsProvider()).build();

            WeatherRecordProcessorFactory recordProcessorFactory = new WeatherRecordProcessorFactory();

            Worker worker = new Worker(recordProcessorFactory, kinesisClientLibConfiguration, kinesisClient, dynamoDB, null);

            WeatherServiceManager weatherServiceManager = new WeatherServiceManager();
            weatherServiceManager.setWorker(worker);

            return weatherServiceManager;
        }
    }

    @Before
    public void setUp() {



    }

    @Test
    public void testApplication() {

        PutRecordRequest putRecordRequest = new PutRecordRequest()
                .withStreamName(properties.getStreamName())
                .withPartitionKey("partition_key")
                .withData(ByteBuffer.wrap("{\"zip\": \"19096\"}".getBytes()));

        kinesisClient.putRecord(putRecordRequest);

        s3.createBucket(properties.getBucketName());
        Assert.assertEquals(s3.listBuckets().size(), 1);

        weatherApplication.run();

        ObjectListing listing = s3.listObjects(properties.getBucketName());

        listing.getObjectSummaries().forEach(s -> log.info("found key: {} ", s.getKey()));

        Assert.assertEquals(1, listing.getObjectSummaries().size());
    }
}

