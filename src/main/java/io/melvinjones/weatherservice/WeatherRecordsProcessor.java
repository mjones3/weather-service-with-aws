package io.melvinjones.weatherservice;

import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer;
import com.amazonaws.services.kinesis.clientlibrary.interfaces.v2.IShutdownNotificationAware;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason;
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownInput;
import com.amazonaws.services.kinesis.model.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class WeatherRecordsProcessor implements IRecordProcessor, IShutdownNotificationAware {

    private static final Logger log = LoggerFactory.getLogger(WeatherRecordsProcessor.class);


    public void initialize(String shardId) {
        log.info("Initializing for shardId: {} stream: {}, application name: {} and ", shardId, "weather-request4", "weather5");
    }

    public void shutdown(IRecordProcessorCheckpointer checkpointer, ShutdownReason reason) {

    }

    public void processRecords(List<Record> records, IRecordProcessorCheckpointer checkpointer) {
        log.info("Processed records: " + records.size());

        WeatherUtils weatherUtils = BeanUtil.getBean(WeatherUtils.class);

        OpenWeatherMapReader openWeatherMapReader = BeanUtil.getBean(OpenWeatherMapReader.class);

        records.forEach(r -> log.info("record.getData() -> {}", new String(r.getData().array())));
        records.forEach(r -> weatherUtils.writeToS3(openWeatherMapReader.getWeatherData(weatherUtils.getWeatherRequestFromRecord(r).getZip())));
    }


    public void shutdown(ShutdownInput shutdownInput) {
        // Record checkpoint at closing shard if shutdown reason is TERMINATE.
        if (shutdownInput.getShutdownReason() == ShutdownReason.TERMINATE) {
            recordCheckpoint(shutdownInput.getCheckpointer());
        }
    }


    public void shutdownRequested(IRecordProcessorCheckpointer checkpointer) {
        // Record checkpoint at graceful shutdown.
        recordCheckpoint(checkpointer);
    }

    private void recordCheckpoint(IRecordProcessorCheckpointer checkpointer) {
        retry(() -> {
            try {
                checkpointer.checkpoint();
            } catch (Throwable e) {
                throw new RuntimeException("Record checkpoint failed.", e);
            }
        });
    }

    private void retry(Runnable f) {
        try {
            f.run();
        } catch (Throwable e) {
            System.out.println(String.format("An error occurred %s. That will be retry...", e.getMessage()));
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
            retry(f);
        }
    }

}
