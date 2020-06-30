package io.melvinjones.weatherservice;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.kinesis.exceptions.InvalidStateException;
import software.amazon.kinesis.exceptions.ShutdownException;
import software.amazon.kinesis.lifecycle.events.*;
import software.amazon.kinesis.processor.ShardRecordProcessor;
import software.amazon.kinesis.retrieval.KinesisClientRecord;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class RecordsProcessor implements ShardRecordProcessor {


/**
 * The implementation of the ShardRecordProcessor interface is where the heart of the record processing logic lives.
 * In this example all we do to 'process' is log info about the records.
 */


    private static final String SHARD_ID_MDC_KEY = "ShardId";

    private static final Logger log = LoggerFactory.getLogger(RecordsProcessor.class);

    private String shardId;

    @Autowired
    WeatherProperties properties;

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
            OpenWeatherMapReader openWeatherMapReader = new OpenWeatherMapReader();
//            processRecordsInput.records().forEach(r -> writeToS3(openWeatherMapReader.getWeatherData(getWeatherRequest(r).getZip())));

        } catch (Throwable t) {

            log.error("Caught throwable while processing records. Aborting.");
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


    /** Called when the lease tied to this record processor has been lost. Once the lease has been lost,
     * the record processor can no longer checkpoint.
     *
     * @param leaseLostInput Provides access to functions and data related to the loss of the lease.
     */
//    public void leaseLost(LeaseLostInput leaseLostInput) {
//        MDC.put(SHARD_ID_MDC_KEY, shardId);
//        try {
//            log.info("Lost lease, so terminating.");
//        } finally {
//            MDC.remove(SHARD_ID_MDC_KEY);
//        }
//    }

    /**
     * Called when all data on this shard has been processed. Checkpointing must occur in the method for record
     * processing to be considered complete; an exception will be thrown otherwise.
     *
     * @param shardEndedInput Provides access to a checkpointer method for completing processing of the shard.
     */
//    public void shardEnded(ShardEndedInput shardEndedInput) {
//        MDC.put(SHARD_ID_MDC_KEY, shardId);
//        try {
//            log.info("Reached shard end checkpointing.");
//            shardEndedInput.checkpointer().checkpoint();
//        } catch (ShutdownException | InvalidStateException e) {
//            log.error("Exception while checkpointing at shard end. Giving up.", e);
//        } finally {
//            MDC.remove(SHARD_ID_MDC_KEY);
//        }
//    }

    /**
     * Invoked when Scheduler has been requested to shut down (i.e. we decide to stop running the app by pressing
     * Enter). Checkpoints and logs the data a final time.
     *
     * @param shutdownRequestedInput Provides access to a checkpointer, allowing a record processor to checkpoint
     *                               before the shutdown is completed.
     */
//    public void shutdownRequested(ShutdownRequestedInput shutdownRequestedInput) {
//        MDC.put(SHARD_ID_MDC_KEY, shardId);
//        try {
//            log.info("Scheduler is shutting down, checkpointing.");
//            shutdownRequestedInput.checkpointer().checkpoint();
//        } catch (ShutdownException | InvalidStateException e) {
//            log.error("Exception while checkpointing at requested shutdown. Giving up.", e);
//        } finally {
//            MDC.remove(SHARD_ID_MDC_KEY);
//        }
//    }

}