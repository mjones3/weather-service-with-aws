package io.melvinjones;

import cloud.localstack.LocalstackTestRunner;
import cloud.localstack.TestUtils;
import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
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
import org.junit.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
//import xyz.fabiano.spring.localstack.LocalstackService;
//import xyz.fabiano.spring.localstack.annotation.SpringLocalstackProperties;
//import xyz.fabiano.spring.localstack.autoconfigure.LocalstackAutoConfiguration;
//import xyz.fabiano.spring.localstack.junit.SpringLocalstackDockerRunner;
//import xyz.fabiano.spring.localstack.legacy.LocalstackDocker;
//import xyz.fabiano.spring.localstack.support.AmazonDockerClientsHolder;

import java.nio.ByteBuffer;
import java.util.UUID;

//@RunWith(SpringJUnit4ClassRunner.class)
//@SpringBootTest
//@RunWith(SpringLocalstackDockerRunner.class)
//@SpringLocalstackProperties(services = { LocalstackService.S3, LocalstackService.KINESIS, LocalstackService.DYNAMO, LocalstackService.CLOUDFORMATION })

@RunWith(LocalstackTestRunner.class)
@LocalstackDockerProperties(services = { "s3", "kinesis", "dynamodb", "cloudwatch" })
@ExtendWith(LocalstackDockerExtension.class)
@Slf4j

@SpringBootTest(classes = WeatherApplication.class)
@ContextConfiguration(classes = {WeatherApplicationTest.WeatherContextConfiguration.class, WeatherProperties.class}, loader = AnnotationConfigContextLoader.class)
@TestPropertySource("classpath:application-test.properties")
//@Testcontainers
//@ComponentScan
public class WeatherApplicationTest {

    private static final Logger log = LoggerFactory.getLogger(WeatherApplicationTest.class);

    private static AmazonKinesis kinesis = TestUtils.getClientKinesis();

    private static AmazonS3 s3 = TestUtils.getClientS3();

    private static AmazonDynamoDB dynamoDB = TestUtils.getClientDynamoDB();

//    @Autowired
//    private static AmazonKinesis kinesis;
//
//    @Autowired
//    private static AmazonS3 s3;
//
//    @Autowired
//    private static AmazonDynamoDB dynamoDB;
//
//    @Autowired
//    private static AmazonCloudWatch cloudWatch;


    @Autowired
    WeatherApplication weatherApplication;

    @Autowired
    WeatherProperties properties;


    @Configuration
    static class WeatherContextConfiguration {

        @Bean
        public WeatherUtils weatherUtils() {

            WeatherUtils weatherUtils = new WeatherUtils();
            weatherUtils.setS3Client(s3);
            return weatherUtils;
        }

        @Bean
        public WeatherServiceManager weatherServiceManager() {


//            kinesis.createStream("weather-requests", 1);

            String workerId = UUID.randomUUID().toString();

            KinesisClientLibConfiguration kinesisClientLibConfiguration =
                    new KinesisClientLibConfiguration("weather11",
                            "weather-requests",
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

//    @Configuration
//    public class SpringTestContext {
//
//        @Bean
//        public AmazonDockerClientsHolder amazonDockerClientsHolder() {
//            return new AmazonDockerClientsHolder(LocalstackDocker.getLocalstackDocker());
//        }
//
//        @Bean
//        public AmazonS3 amazonS3(AmazonDockerClientsHolder amazonDockerClientsHolder) {
//            return amazonDockerClientsHolder.amazonS3();
//        }
//    }

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

        kinesis.createStream("weather-requests", 1);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        kinesis.listStreams().getStreamNames().forEach(s -> log.info("Found stream: {}", s));

        PutRecordRequest putRecordRequest = new PutRecordRequest()
                .withStreamName("weather-requests")
                .withPartitionKey("partition_key")
                .withData(ByteBuffer.wrap("{\"zip\": \"19096\"}".getBytes()));

        kinesis.putRecord(putRecordRequest);


        s3.createBucket("mjones3-weather-reports");
        Assert.assertEquals(s3.listBuckets().size(), 1);

        weatherApplication.run();

        ObjectListing listing = s3.listObjects("mjones3-weather-reports");

        listing.getObjectSummaries().forEach(s -> log.info("found key: {} ", s.getKey()));

        Assert.assertEquals(1, listing.getObjectSummaries().size());
    }
}

