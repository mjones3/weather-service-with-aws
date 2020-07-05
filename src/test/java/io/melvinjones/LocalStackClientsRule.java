package io.melvinjones;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.junit.ClassRule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.localstack.LocalStackContainer;

import java.lang.annotation.Annotation;

public class LocalStackClientsRule implements TestRule {

    private AmazonS3 s3;

    private AmazonKinesis kinesis;

    private LocalStackContainer localstack;

    public LocalStackClientsRule(LocalStackContainer localstack) {

//        localstack = new LocalStackContainer()
//                .withServices(LocalStackContainer.Service.KINESIS,
//                        LocalStackContainer.Service.S3,
//                        LocalStackContainer.Service.DYNAMODB,
//                        LocalStackContainer.Service.CLOUDWATCH);

        s3 = AmazonS3ClientBuilder.standard().
                withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.S3)).
                withCredentials(localstack.getDefaultCredentialsProvider())
                .build();


        kinesis = AmazonKinesisClientBuilder
                .standard()
                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.KINESIS))
                .withCredentials(localstack.getDefaultCredentialsProvider())
                .build();;

    }

    public AmazonS3 getS3() {
        return this.s3;
    }

    public AmazonKinesis getKinesis() {
        return this.kinesis;
    }

    public LocalStackContainer getLocalStackContainer() {
        return this.localstack;
    }


    @Override
    public Statement apply(Statement base, Description description) {

        return new Statement() {
            @Override public void evaluate() throws Throwable {

                try {
                    base.evaluate(); // This will run the test.
                } finally {

                }
            }
        };

    }
}
