package io.melvinjones;

import cloud.localstack.TestUtils;
import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import groovy.util.logging.Slf4j;
import io.melvinjones.weatherservice.*;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"mock"})
@Tag("unit")
//@SpringBootTest(
//        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
//        classes = {WeatherApplication.class})
@ExtendWith(LocalstackDockerExtension.class)
@LocalstackDockerProperties() // randomizePorts = false, services = {"sqs", "s3"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@ContextConfiguration(classes = {})
@Slf4j
@SpringBootTest(classes = WeatherApplication.class)
//@ContextConfiguration(classes = WeatherApplicationTest.KinesisConfiguration.class)
//@ContextConfiguration(loader= AnnotationConfigContextLoader.class)
@ContextConfiguration(classes = WeatherApplicationTest.WeatherContextConfiguration.class, loader = AnnotationConfigContextLoader.class)
//@ComponentScan
//@ConfigurationProperties("classpath:application-test.properties")
@TestPropertySource("classpath:application-test.properties")

@Testcontainers
public class WeatherApplicationTest {

    private static final Logger log = LoggerFactory.getLogger(WeatherApplicationTest.class);


    private String shardId;

//    @ClassRule
//    LocalStackContainer localstack = new LocalStackContainer()
//            .withServices(LocalStackContainer.Service.KINESIS, LocalStackContainer.Service.S3);

    @ClassRule
    public static final LocalStackContainer localstack = new LocalStackContainer()
            .withServices(LocalStackContainer.Service.KINESIS,
                    LocalStackContainer.Service.S3,
                    LocalStackContainer.Service.DYNAMODB,
                    LocalStackContainer.Service.CLOUDWATCH);

    private static Worker worker;

    private static AmazonS3 s3;

//    public Worker worker;

//    @Autowired
//    WeatherServiceManager weatherServiceManager;

    @Autowired
    WeatherApplication weatherApplication;

    @Autowired
    WeatherProperties properties;

//    @ClassRule
//    private  static KinesisClientLibConfiguration kinesisClientLibConfiguration =
////                    new KinesisClientLibConfiguration(properties.getApplicationName(), properties.getStreamName(), localstack.getDefaultCredentialsProvider(), workerId);
//            new KinesisClientLibConfiguration("weather-requests", "weather", localstack.getDefaultCredentialsProvider(), UUID.randomUUID().toString());

    private  static  AmazonKinesis kinesisClient;

    @Configuration
    static class WeatherContextConfiguration {

//        @ClassRule
//        LocalStackContainer localstack = new LocalStackContainer()
//                .withServices(LocalStackContainer.Service.KINESIS, LocalStackContainer.Service.S3);

//        @Bean
//        public WeatherProperties weatherProperties() {
//
//            WeatherProperties properties = new WeatherProperties();
//            properties.setApiKey("xxx-xxxx-xxxxx");
//            properties.setApplicationName("test-application");
//            properties.setBucketName("test-bucket");
//            properties.setKinesisRegionName("us-east-1");
//            properties.setWeatherURL("http://localhost");
//            properties.setStreamName("test-stream");
//            properties.setS3RegionName("us-east-1");
//
//            return properties;
//        }
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

        // this bean will be injected into the OrderServiceTest class
        @Bean
        public WeatherServiceManager weatherServiceManager() {

            TestUtils.setEnv("AWS_CBOR_DISABLE", "1");

            kinesisClient = AmazonKinesisClientBuilder
                    .standard()
                    .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.KINESIS))
                    .withCredentials(localstack.getDefaultCredentialsProvider())
                    .build();


            kinesisClient.createStream("weather-requests", 1);

//            LocalStackContainer localstack = new LocalStackContainer()
//                    .withServices(LocalStackContainer.Service.KINESIS, LocalStackContainer.Service.S3);

            WeatherServiceManager weatherServiceManager = new WeatherServiceManager();

//            weatherServiceManager.setProperties(properties);

            String workerId = UUID.randomUUID().toString();
//            KinesisClientLibConfiguration kinesisClientLibConfiguration =
//                    new KinesisClientLibConfiguration(properties.getApplicationName(), properties.getStreamName(), localstack.getDefaultCredentialsProvider(), workerId);
//                    new KinesisClientLibConfiguration(properties.getApplicationName(), properties.getStreamName(), localstack.getDefaultCredentialsProvider(), workerId);

            KinesisClientLibConfiguration kinesisClientLibConfiguration =
//                    new KinesisClientLibConfiguration(properties.getApplicationName(), properties.getStreamName(), localstack.getDefaultCredentialsProvider(), workerId);
                    new KinesisClientLibConfiguration("weather11", "weather-requests", localstack.getDefaultCredentialsProvider(), UUID.randomUUID().toString())
                            .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON);



            AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder
                    .standard()
                    .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.DYNAMODB))
                    .withCredentials(localstack.getDefaultCredentialsProvider()).build();

//            AmazonKinesis kinesisClient = TestUtils.getClientKinesis();
//            AmazonDynamoDB dynamoDB = TestUtils.getClientDynamoDB();
//            AmazonCloudWatch cloudWatch = TestUtils.getClientCloudWatch();

            WeatherRecordProcessorFactory recordProcessorFactory = new WeatherRecordProcessorFactory();
            Worker worker = new Worker(recordProcessorFactory, kinesisClientLibConfiguration, kinesisClient, dynamoDB, null);


            log.info("Setting worker.");
            weatherServiceManager.setWorker(worker);
            return weatherServiceManager;
        }
    }


//    @Autowired
//    @Shared
//    WeatherServiceManager manager = new WeatherServiceManager();

//    @Autowired
//    WeatherApplication weatherApplication;

    @BeforeAll
    void setUp() {

//        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
//        System.setProperty(SDKGlobalConfiguration.AWS_CBOR_DISABLE_SYSTEM_PROPERTY, "true");
//        System.setProperty("com.amazonaws.sdk.disableCbor", "true");
//        System.setProperty("aws.cborEnabled", "false");


        ListStreamsResult listStreamsResult = kinesisClient.listStreams();
        listStreamsResult.getStreamNames().forEach(s -> log.info("Stream found: {}" + s));





    }


    @Test
    public void someTestMethod() {

        PutRecordRequest putRecordRequest = new PutRecordRequest()
                .withStreamName("weather-requests")
                .withPartitionKey("partition_key")
                .withData(ByteBuffer.wrap("{\"zip\": \"19096\"}".getBytes()));


        PutRecordResult putRecordResult = kinesisClient.putRecord(putRecordRequest);

        s3.createBucket(properties.getBucketName());

        weatherApplication.run();

//        try {
//            Thread.sleep(30000);
//        }
//        catch (Exception e) {
//
//        }


        ObjectListing listing = s3.listObjects(properties.getBucketName());

//        AmazonS3 s3 = TestUtils.getClientS3();



        listing.getObjectSummaries().forEach(s -> log.info("found key: {} ", s.getKey()));


        Assert.assertNotEquals(0, listing.getObjectSummaries().size());

//        String shardId = putRecordResult.getShardId();

//        log.info("shardId=" + shardId);
//        GetShardIteratorRequest getShardIteratorRequest = new GetShardIteratorRequest();
//        getShardIteratorRequest.setStreamName("weather-requests");
//        getShardIteratorRequest.setShardId(shardId);
//        getShardIteratorRequest.setShardIteratorType("TRIM_HORIZON");





//
//        GetShardIteratorResult getShardIteratorResult = kinesisClient.getShardIterator(getShardIteratorRequest);
//
//        GetRecordsRequest recordsRequest = new GetRecordsRequest();
//        recordsRequest.setShardIterator(getShardIteratorResult.getShardIterator());
//        recordsRequest.setLimit(25);
//
//        GetRecordsResult getRecordsResult = kinesisClient.getRecords(recordsRequest);
//        Assert.assertNotNull(getRecordsResult);
//
//        List<Record> records = getRecordsResult.getRecords();
//
//        Assert.assertNotEquals(0,records.size());
//
//        records.forEach(r -> log.info("Records: " + r.getData().toString()));

//        weatherServiceManager.run();
        assert (1 == 1);

    }
}

//    public static class KinesisConfiguration {


//        @Bean
//        public S3DownUpLoader s3DownUpLoader(ResourceLoader resourceLoader){
//            return new S3DownUpLoader(resourceLoader);
////        }
////
//        @Bean()
//        public Worker getKinesisWorker() {
//
//            String workerId = UUID.randomUUID().toString();
//            KinesisClientLibConfiguration kinesisClientLibConfiguration =
//                    new KinesisClientLibConfiguration("weather", "weather-requests", localstack.getDefaultCredentialsProvider(), workerId);
//
//            AmazonKinesis kinesisClient = AmazonKinesisClientBuilder
//                .standard()
//                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.KINESIS))
//                .withCredentials(localstack.getDefaultCredentialsProvider())
//                .build();

//            AmazonKinesis kinesisClient = TestUtils.getClientKinesis();
//            AmazonDynamoDB dynamoDB = TestUtils.getClientDynamoDB();
//            AmazonCloudWatch cloudWatch = TestUtils.getClientCloudWatch();

//            WeatherRecordProcessorFactory recordProcessorFactory = new WeatherRecordProcessorFactory();
//            Worker worker = new Worker(recordProcessorFactory, kinesisClientLibConfiguration, kinesisClient, null, null);
//
//            return worker;

//            return AmazonS
//                    .standard()
//                    .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
//                    .withCredentials(localstack.getDefaultCredentialsProvider())
//                    .build();
//        }
//
//        @Bean
//        public WeatherServiceManager getWeatherServiceManager(){
//            return new WeatherServiceManager(this.getKinesisWorker());
//        }

//    }

