

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.Bucket
import org.junit.platform.commons.logging.LoggerFactory
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.*
import org.yaml.snakeyaml.Yaml
import java.util.logging.Logger

@Testcontainers
class GetRequestIntegrationSpec extends Specification {

//    private static final Logger log = LoggerFactory.getLogger(GetRequestIntegrationSpec.class);

    String testConfigFileName = "src/test/resources/application-test.yml"

    @Shared
    LocalStackContainer localstack = new LocalStackContainer()
            .withServices(LocalStackContainer.Service.S3,
                    LocalStackContainer.Service.KINESIS);

    @Shared
    AmazonS3 s3;


    def setup() {

        s3 = AmazonS3ClientBuilder
                .standard()
        //                .withRegion(config.getRegion())
                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.S3))
                .withCredentials(localstack.getDefaultCredentialsProvider())
                .build();

        s3.createBucket("weather-requests")

//        log.info("Set up S3.")

    }


    def "validate that request against schema passes"() {
        given:
        List<Bucket> bucketsList = s3.listBuckets()

        when:
        bucketsList.size() == 1
        String bucketName = bucketsList.get(0).getName();


        then:
        bucketName == "weather-requests"

    }

}