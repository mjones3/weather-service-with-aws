import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.Bucket
import io.melvinjones.weatherservice.WeatherApplication
import io.melvinjones.weatherservice.WeatherProperties
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
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.*
import org.yaml.snakeyaml.Yaml
import java.util.logging.Logger

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
    AmazonKinesis kinesisClient;

    @Shared
    DynamoDbAsyncClient dynamoClient

    @Shared
    CloudWatchAsyncClient cloudWatchClient

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

        s3 = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.S3))
                .withCredentials(localstack.getDefaultCredentialsProvider())
                .build();

        kinesisClient = AmazonKinesisClientBuilder
                .standard()
                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.KINESIS))
                .withCredentials(localstack.getDefaultCredentialsProvider())
                .build()

//        kinesisClient = KinesisAsyncClient.builder()
//
//                .credentialsProvider(localstack.getDefaultCredentialsProvider())
//                .build();

//        dynamoClient = DynamoDbAsyncClient.builder()
//                .credentialsProvider(localstack.getDefaultCredentialsProvider())
//                .build();
//
//        cloudWatchClient = CloudWatchAsyncClient.builder()
//                .credentialsProvider(localstack.getDefaultCredentialsProvider())
//                .build();


        s3.createBucket(properties."aws.s3.bucketName")

    }

    def "validate weatherservice stores to s3"() {
        WeatherApplication weatherApplication = new WeatherApplication();

        weatherApplication.setS3Client(s3)
        weatherApplication.setKinesisClient(kinesisClient)
        weatherApplication.setCloudWatchClient(cloudWatchClient)
        weatherApplication.setDynamoClient(dynamoClient)

        weatherApplication.run();
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