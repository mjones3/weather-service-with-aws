package io.melvinjones;

import cloud.localstack.LocalstackTestRunner;
import cloud.localstack.TestUtils;
import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import groovy.util.logging.Slf4j;
import io.melvinjones.weatherservice.*;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.nio.ByteBuffer;
import java.util.UUID;

@RunWith(LocalstackTestRunner.class)
@LocalstackDockerProperties(services = { "s3", "kinesis", "dynamodb", "cloudwatch" })
@ExtendWith(LocalstackDockerExtension.class)
@Slf4j

@SpringBootTest(classes = WeatherApplication.class)
@ContextConfiguration(classes = {WeatherApplicationTest.WeatherContextConfiguration.class, WeatherProperties.class}, loader = AnnotationConfigContextLoader.class)
@TestPropertySource("classpath:application-test.properties")

public class WeatherApplicationTest {

    private static final Logger log = LoggerFactory.getLogger(WeatherApplicationTest.class);

    private static AmazonKinesis kinesis = TestUtils.getClientKinesis();

    private static AmazonS3 s3 = TestUtils.getClientS3();

    private static AmazonDynamoDB dynamoDB = TestUtils.getClientDynamoDB();


    @Autowired
    WeatherApplication weatherApplication;

    @Autowired
    private  WeatherProperties properties;


    @Configuration
    @TestPropertySource("classpath:application-test.properties")
    static class WeatherContextConfiguration {

        @Autowired
        private WeatherProperties weatherProperties;

        @Bean
        public WeatherUtils weatherUtils() {

            WeatherUtils weatherUtils = new WeatherUtils();
            weatherUtils.setS3Client(s3);
            return weatherUtils;
        }

        @Bean
        public WeatherServiceManager weatherServiceManager() {

            String workerId = UUID.randomUUID().toString();

            KinesisClientLibConfiguration kinesisClientLibConfiguration =
                    new KinesisClientLibConfiguration(weatherProperties.getApplicationName(),
                            weatherProperties.getStreamName(),
                            TestUtils.getCredentialsProvider(),
                            workerId)
                            .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);

            WeatherRecordProcessorFactory recordProcessorFactory = new WeatherRecordProcessorFactory();
            Worker worker = new Worker(recordProcessorFactory, kinesisClientLibConfiguration, kinesis, dynamoDB, null);

            WeatherServiceManager weatherServiceManager = new WeatherServiceManager();
            weatherServiceManager.setWorker(worker);

            return weatherServiceManager;
        }
    }

    @Before
    public void setUp() {

        try {
            new TestContextManager(getClass()).prepareTestInstance(this);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testApplication() {

        TestUtils.setEnv("AWS_CBOR_DISABLE", "1");

        kinesis.createStream(properties.getStreamName(), 1);

        //give kinesis time to create the stream
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        PutRecordRequest putRecordRequest = new PutRecordRequest()
                .withStreamName(properties.getStreamName())
                .withPartitionKey("partition_key")
                .withData(ByteBuffer.wrap("{\"zip\": \"19096\"}".getBytes()));

        kinesis.putRecord(putRecordRequest);


        s3.createBucket(properties.getBucketName());
        Assert.assertEquals(s3.listBuckets().size(), 1);

        weatherApplication.run();

        ObjectListing listing = s3.listObjects(properties.getBucketName());

        listing.getObjectSummaries().forEach(s -> log.info("found key: {} ", s.getKey()));

        Assert.assertEquals(1, listing.getObjectSummaries().size());
    }
}

