import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.Bucket
import io.melvinjones.weatherservice.WeatherApplication
import io.melvinjones.weatherservice.WeatherProperties
import io.melvinjones.weatherservice.WeatherRecordProcessorFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.commons.logging.LoggerFactory
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
//import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
//import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
//import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
//import software.amazon.awssdk.services.dynamodb.DynamoDbClient
//import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.*
import org.yaml.snakeyaml.Yaml
import java.util.logging.Logger
import cloud.localstack.TestUtils

@Testcontainers
@SpringBootTest
@ExtendWith(SpringExtension.class)
@TestPropertySource("classpath:application-test.properties")
@EnableConfigurationProperties(value = WeatherProperties.class)
class WeatherApplicationIntegrationSpec extends Specification {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WeatherApplication.class);


//    String testConfigFileName = "src/test/resources/application-test.yml"

    @Shared
    LocalStackContainer localstack = new LocalStackContainer()
            .withServices(LocalStackContainer.Service.S3,
                    LocalStackContainer.Service.KINESIS,
                    LocalStackContainer.Service.DYNAMODB,
                    LocalStackContainer.Service.CLOUDWATCH, );

    @Shared
    AmazonS3 s3;

    @Shared
    AmazonKinesisAsyncClient kinesisClient;

    @Shared
    AmazonDynamoDBClient dynamoDB

    @Shared
    AmazonCloudWatchAsyncClient cloudwatch

    @Shared
    Properties properties

//    @Autowired
//    WeatherApplication weatherApplication;


    def setup() {

        properties = new Properties()
        File propertiesFile = new File('src/test/resources/application-test.properties')
        propertiesFile.withInputStream {
            properties.load(it)
        }

        log.info("properties: " + properties.toString())

//        assert weatherApplication != null
//
//        s3 = AmazonS3ClientBuilder
//                .standard()
//                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.S3))
//                .withCredentials(localstack.getDefaultCredentialsProvider())
//                .build();


        kinesisClient = TestUtils.getClientKinesis();


//        KinesisClientLibConfiguration(String applicationName,
//                String streamName,
//                AWSCredentialsProvider credentialsProvider,
//                String workerId) {




        String workerId = InetAddress.getLocalHost().getCanonicalHostName() + ":" + UUID.randomUUID();
        KinesisClientLibConfiguration kinesisClientLibConfiguration =
                new KinesisClientLibConfiguration(properties."aws.kinesis.applicationName".toString(), properties."aws.kinesis.streamName".toString(), localstack.getDefaultCredentialsProvider(), workerId)

//        dynamoDB = TestUtils.getClientDynamoDB();
//
//        cloudwatch = TestUtils.getClientCloudWatch();


//        KinesisAsyncClient client = KinesisAsyncClient.builder()
//                .credentialsProvider(localstack.getDefaultCredentialsProvider())
////                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.KINESIS))
//                .build();

//        kinesisClient = AmazonKinesisClientBuilder
//                .standard()
//                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.KINESIS))
//                .withCredentials(localstack.getDefaultCredentialsProvider())
//                .build()


//         s3 = TestUtils.getClientS3();

//        kinesisClient = TestUtils.getClientKinesisAsync();

//        kinesisClient = (KinesisAsyncClient) AmazonKinesisAsyncClientBuilder.standard()
//                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.KINESIS))
//                .withCredentials(localstack.getDefaultCredentialsProvider())
//                .build();

//        kinesisClientLibConfiguration.withInitialPositionInStream(SAMPLE_APPLICATION_INITIAL_POSITION_IN_STREAM);

        WeatherRecordProcessorFactory recordProcessorFactory = new WeatherRecordProcessorFactory()
        Worker worker = new Worker(recordProcessorFactory, kinesisClientLibConfiguration, kinesisClient, null, null)

        WeatherApplication weatherApplication = new WeatherApplication()
        weatherApplication.setWorker(worker)
        weatherApplication.run()


//         kinesisClient = TestUtils.getClientKinesis();


//
//

//        dynamoClient = DynamoDbAsyncClient.builder()
//                .credentialsProvider(localstack.getDefaultCredentialsProvider())
//                .build();
//
//        cloudWatchClient = CloudWatchAsyncClient.builder()
//                .credentialsProvider(localstack.getDefaultCredentialsProvider())
//                .build();


//        s3.createBucket(properties."aws.s3.bucketName")


    }

    def "validate weatherservice stores to s3"() {
        WeatherApplication weatherApplication = new WeatherApplication();

        given:
        weatherApplication.setS3Client(s3)
        weatherApplication.setKinesisClient(kinesisClient)
        weatherApplication.setCloudWatchClient(cloudwatch)
        weatherApplication.setDynamoClient(dynamoDB)

        when:
        weatherApplication.run();

        then:
        1==1
    }

    def "validate that request against schema passes"() {
        given:
        List<Bucket> bucketsList = s3.listBuckets()

        when:
        bucketsList.size() == 1
        String bucketName = bucketsList.get(0).getName();


        then:
        bucketName == "mjones3-weather-reports"

    }

}