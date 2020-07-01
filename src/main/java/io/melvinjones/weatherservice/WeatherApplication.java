package io.melvinjones.weatherservice;
/*
 *  Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Amazon Software License (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/asl/
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */


/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

//import com.amazonaws.auth.profile.ProfileCredentialsProvider;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.awssdk.services.kinesis.model.PutRecordRequest;
import software.amazon.kinesis.common.ConfigsBuilder;
import software.amazon.kinesis.coordinator.Scheduler;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.processor.ShardRecordProcessorFactory;
import software.amazon.kinesis.retrieval.KinesisClientRecord;
import software.amazon.kinesis.retrieval.polling.PollingConfig;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class will run a simple app that uses the KCL to read data and uses the AWS SDK to publish data.
 * Before running this program you must first create a Kinesis stream through the AWS console or AWS SDK.
 */
@SpringBootApplication
@ComponentScan
public class WeatherApplication implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(WeatherApplication.class);

	@Autowired
	WeatherProperties properties;

	@Autowired
	OpenWeatherMapReader openWeatherMapReader;

	@PostConstruct
	public void init() {
		WeatherRecordProcessor.setProperties(properties);
		WeatherRecordProcessor.setOpenWeatherMapReader(openWeatherMapReader);
	}

	private Region region;

	public static Logger getLog() {
		return log;
	}

	private KinesisAsyncClient kinesisClient;
	private DynamoDbAsyncClient dynamoClient;
	private CloudWatchAsyncClient cloudWatchClient;
	private AmazonS3 s3Client;


	/**
	 * Invoke the main method with 2 args: the stream name and (optionally) the region.
	 * Verifies valid inputs and then starts running the app.
	 */


	public static void main(String[] args) {
		SpringApplication.run(WeatherApplication.class, args);
	}

	public void run(String... args) {

		/**
		 * Sets up configuration for the KCL, including DynamoDB and CloudWatch dependencies. The final argument, a
		 * ShardRecordProcessorFactory, is where the logic for record processing lives, and is located in a private
		 * class below.
		 */


		log.info(properties.toString());

		this.region = Region.of(ObjectUtils.firstNonNull(properties.getKinesisRegionName()));

		this.kinesisClient = KinesisAsyncClient.builder()
				.credentialsProvider(ProfileCredentialsProvider.create())
				.region(this.region).build();


		DynamoDbAsyncClient dynamoClient = DynamoDbAsyncClient.builder()
				.credentialsProvider(ProfileCredentialsProvider.create())
				.region(region).build();

		CloudWatchAsyncClient cloudWatchClient = CloudWatchAsyncClient.builder()
				.credentialsProvider(ProfileCredentialsProvider.create())
				.region(region).build();


		ConfigsBuilder configsBuilder = new ConfigsBuilder(properties.getStreamName(), properties.getApplicationName(), kinesisClient, dynamoClient, cloudWatchClient, UUID.randomUUID().toString(), new WeatherRecordProcessorFactory());

		/**
		 * The Scheduler (also called Worker in earlier versions of the KCL) is the entry point to the KCL. This
		 * instance is configured with defaults provided by the ConfigsBuilder.
		 */
		Scheduler scheduler = new Scheduler(
				configsBuilder.checkpointConfig(),
				configsBuilder.coordinatorConfig(),
				configsBuilder.leaseManagementConfig(),
				configsBuilder.lifecycleConfig(),
				configsBuilder.metricsConfig(),
				configsBuilder.processorConfig(),
				configsBuilder.retrievalConfig().retrievalSpecificConfig(new PollingConfig(properties.getStreamName(), kinesisClient))
		);

		/**
		 * Kickoff the Scheduler. Record processing of the stream of dummy data will continue indefinitely
		 * until an exit is triggered.
		 */
		Thread schedulerThread = new Thread(scheduler);
		schedulerThread.setDaemon(true);
		schedulerThread.start();

		/**
		 * Allows termination of app by pressing Enter.
		 */
		System.out.println("Press enter to shutdown");
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			reader.readLine();
		} catch (IOException ioex) {
			log.error("Caught exception while waiting for confirm. Shutting down.", ioex);
		}


		/**
		 * Stops consuming data. Finishes processing the current batch of data already received from Kinesis
		 * before shutting down.
		 */
		Future<Boolean> gracefulShutdownFuture = scheduler.startGracefulShutdown();
		log.info("Waiting up to 20 seconds for shutdown to complete.");
		try {
			gracefulShutdownFuture.get(20, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			log.info("Interrupted while waiting for graceful shutdown. Continuing.");
		} catch (ExecutionException e) {
			log.error("Exception while executing graceful shutdown.", e);
		} catch (TimeoutException e) {
			log.error("Timeout while waiting for shutdown.  Scheduler may not have exited.");
		}
		log.info("Completed, shutting down now.");
	}

	/**
	 * Sends a single record of dummy data to Kinesis.
	 */
	private void publishRecord() {
		PutRecordRequest request = PutRecordRequest.builder()
				.partitionKey(RandomStringUtils.randomAlphabetic(5, 20))
				.streamName(properties.getStreamName())
				.data(SdkBytes.fromByteArray(RandomUtils.nextBytes(10)))
				.build();
		try {
			kinesisClient.putRecord(request).get();
		} catch (InterruptedException e) {
			log.info("Interrupted, assuming shutdown.");
		} catch (ExecutionException e) {
			log.error("Exception while sending data to Kinesis. Will try again next cycle.", e);
		}
	}

	private static class WeatherRecordProcessorFactory implements ShardRecordProcessorFactory {
		public ShardRecordProcessor shardRecordProcessor() {
			return new WeatherRecordProcessor();
		}
	}



	private static WeatherRequest getWeatherRequest(KinesisClientRecord record) {

		String s = new String();
		WeatherRequest request = new WeatherRequest();

		try {

			CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(record.data());
			s = charBuffer.toString();

			ObjectMapper mapper = new ObjectMapper();
			request = mapper.readValue(s, WeatherRequest.class);

		} catch (Exception e) {
			e.printStackTrace();
		}

		log.info("record data: " + s);

		return request;
	}


	private static void writeToS3(String weatherJSON, String bucketName) {

		String keyName = UUID.randomUUID().toString();

		log.info("Uploading {} to S3 bucket {}...\n", keyName, bucketName);

		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withRegion(Regions.US_EAST_1)
				.withCredentials(new com.amazonaws.auth.profile.ProfileCredentialsProvider())
				.build();

		try {


			s3Client.putObject(bucketName, keyName, weatherJSON);


		} catch (Exception e) {
			e.printStackTrace();
//			System.exit(1);
		}
	}

	/**
	 * The implementation of the ShardRecordProcessor interface is where the heart of the record processing logic lives.
	 * In this example all we do to 'process' is log info about the records.
	 */
	private static class WeatherRecordProcessor implements ShardRecordProcessor {

		private static final String SHARD_ID_MDC_KEY = "ShardId";

		private static final Logger log = LoggerFactory.getLogger(WeatherRecordProcessor.class);

		private String shardId;

		private static WeatherProperties properties;

		private static OpenWeatherMapReader openWeatherMapReader;

		private static void setProperties(WeatherProperties properties) {
			WeatherRecordProcessor.properties = properties;
		}

		private static void setOpenWeatherMapReader(OpenWeatherMapReader openWeatherMapReader) {
			WeatherRecordProcessor.openWeatherMapReader = openWeatherMapReader;
		}

		/**
		 * Invoked by the KCL before data records are delivered to the ShardRecordProcessor instance (via
		 * processRecords). In this example we do nothing except some logging.
		 *
		 * @param initializationInput Provides information related to initialization.
		 */
		public void initialize(InitializationInput initializationInput) {
			shardId = initializationInput.shardId();
			MDC.put(SHARD_ID_MDC_KEY, shardId);
			try {
				log.info("Initializing @ Sequence: {}", initializationInput.extendedSequenceNumber());
			} finally {
				MDC.remove(SHARD_ID_MDC_KEY);
			}
		}


		/**
		 * Handles record processing logic. The Amazon Kinesis Client Library will invoke this method to deliver
		 * data records to the application. In this example we simply log our records.
		 *
		 * @param processRecordsInput Provides the records to be processed as well as information and capabilities
		 *                            related to them (e.g. checkpointing).
		 */
		public void processRecords(ProcessRecordsInput processRecordsInput) {
			MDC.put(SHARD_ID_MDC_KEY, shardId);
			try {

				log.info("Processing {} record(s)", processRecordsInput.records().size());

				processRecordsInput.records().forEach(r -> writeToS3(openWeatherMapReader.getWeatherData(getWeatherRequest(r).getZip()), properties.getBucketName()));

			} catch (Throwable t) {

				log.error("Caught throwable while processing records. Aborting.");
				t.printStackTrace();
				Runtime.getRuntime().halt(1);
			} finally {
				MDC.remove(SHARD_ID_MDC_KEY);
			}
		}

		/** Called when the lease tied to this record processor has been lost. Once the lease has been lost,
		 * the record processor can no longer checkpoint.
		 *
		 * @param leaseLostInput Provides access to functions and data related to the loss of the lease.
		 */
		public void leaseLost(LeaseLostInput leaseLostInput) {
			MDC.put(SHARD_ID_MDC_KEY, shardId);
			try {
				log.info("Lost lease, so terminating.");
			} finally {
				MDC.remove(SHARD_ID_MDC_KEY);
			}
		}

		/**
		 * Called when all data on this shard has been processed. Checkpointing must occur in the method for record
		 * processing to be considered complete; an exception will be thrown otherwise.
		 *
		 * @param shardEndedInput Provides access to a checkpointer method for completing processing of the shard.
		 */
		public void shardEnded(ShardEndedInput shardEndedInput) {
			MDC.put(SHARD_ID_MDC_KEY, shardId);
			try {
				log.info("Reached shard end checkpointing.");
				shardEndedInput.checkpointer().checkpoint();
			} catch (ShutdownException | InvalidStateException e) {
				log.error("Exception while checkpointing at shard end. Giving up.", e);
			} finally {
				MDC.remove(SHARD_ID_MDC_KEY);
			}
		}


		/**
		 * Invoked when Scheduler has been requested to shut down (i.e. we decide to stop running the app by pressing
		 * Enter). Checkpoints and logs the data a final time.
		 *
		 * @param shutdownRequestedInput Provides access to a checkpointer, allowing a record processor to checkpoint
		 *                               before the shutdown is completed.
		 */
		public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
			MDC.put(SHARD_ID_MDC_KEY, shardId);
			try {
				log.info("Scheduler is shutting down, checkpointing.");
				shutdownRequestedInput.checkpointer().checkpoint();
			} catch (ShutdownException | InvalidStateException e) {
				log.error("Exception while checkpointing at requested shutdown. Giving up.", e);
			} finally {
				MDC.remove(SHARD_ID_MDC_KEY);
			}
		}
	}

	public KinesisAsyncClient getKinesisClient() {
		return kinesisClient;
	}

	public void setKinesisClient(KinesisAsyncClient kinesisClient) {
		this.kinesisClient = kinesisClient;
	}

	public DynamoDbAsyncClient getDynamoClient() {
		return dynamoClient;
	}

	public void setDynamoClient(DynamoDbAsyncClient dynamoClient) {
		this.dynamoClient = dynamoClient;
	}

	public CloudWatchAsyncClient getCloudWatchClient() {
		return cloudWatchClient;
	}

	public void setCloudWatchClient(CloudWatchAsyncClient cloudWatchClient) {
		this.cloudWatchClient = cloudWatchClient;
	}

	public AmazonS3 getS3Client() {
		return s3Client;
	}

	public void setS3Client(AmazonS3 s3Client) {
		this.s3Client = s3Client;
	}


}
