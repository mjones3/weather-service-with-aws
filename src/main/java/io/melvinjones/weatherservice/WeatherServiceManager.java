package io.melvinjones.weatherservice;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration;
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker;
import com.amazonaws.services.kinesis.model.Record;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Future;

@Component
public class WeatherServiceManager {

    private static final Logger log = LoggerFactory.getLogger(WeatherServiceManager.class);

    Worker worker;

    @Autowired
    WeatherProperties weatherProperties;


    public WeatherServiceManager(Worker worker) {
        this.worker = worker;
    }

    public WeatherServiceManager() {

    }

    public void run() {

        log.info("worker = " + worker);
        log.info("properties=" + weatherProperties);
        if (worker == null) {
            // Create a Worker.
            worker = new Worker.Builder()
                    .recordProcessorFactory(
                            new WeatherRecordProcessorFactory()
                    )
                    .config(
                            new KinesisClientLibConfiguration(
                                    weatherProperties.getApplicationName(),
                                    weatherProperties.getStreamName(),
                                    new ProfileCredentialsProvider(),
                                    UUID.randomUUID().toString()
                            ).withRegionName(weatherProperties.getKinesisRegionName())
                                    .withInitialLeaseTableReadCapacity(1)
                                    .withInitialLeaseTableWriteCapacity(1)
                                    .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)
                    )
                    .build();

            // Shutdown worker gracefully using shutdown hook.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    worker.startGracefulShutdown().get();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }));
        }

        log.info("application name: " + worker.getApplicationName());

        try {
            log.info("Getting ready to run worker...");
            worker.run();
        }
        catch (Exception e) {
            e.printStackTrace();
        }



    }

    @Scheduled(fixedDelay = 120000, initialDelay = 120000)
    public void shutdownWorker()
    {
        if (worker != null) {
            try {
                log.info("Shutting down KCL Worker...");
                worker.shutdown();
                Future<Boolean> future = worker.startGracefulShutdown();
            } catch (Exception e) {
                log.error("refreshWorker() - an unrecoverable exception occured: ", e);
                System.exit(1);
            }
        }
    }



    public void setWorker(Worker worker) {
        this.worker = worker;
    }
}
