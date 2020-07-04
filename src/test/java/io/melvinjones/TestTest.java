package io.melvinjones;

import cloud.localstack.docker.LocalstackDockerExtension;
import cloud.localstack.docker.annotation.LocalstackDockerProperties;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@RunWith(SpringJUnit4ClassRunner.class)
@ExtendWith(LocalstackDockerExtension.class)
@LocalstackDockerProperties() // randomizePorts = false, services = {"sqs", "s3"})
@Testcontainers
public class  TestTest {


    @Rule
    public LocalStackContainer localstack = new LocalStackContainer()
            .withServices(LocalStackContainer.Service.S3);

        // will be started before and stopped after each test method
//        @Container
//        private PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer()
//                .withDatabaseName("foo")
//                .withUsername("foo")
//                .withPassword("secret");
        @Test
        public void test() {
            Assert.assertTrue(localstack.isRunning());
//            Assert.assertTrue(localstack.isRunning());
        }
    }

