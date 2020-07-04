package io.melvinjones.weatherservice;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.model.Record;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class WeatherUtils {

    private static final Logger log = LoggerFactory.getLogger(WeatherUtils.class);

    @Autowired
    WeatherProperties properties;

    private AmazonS3 s3Client;

    public WeatherRequest getWeatherRequestFromRecord(Record record) {

        String s = new String();
        WeatherRequest request = new WeatherRequest();



        try {

            CharBuffer charBuffer = StandardCharsets.US_ASCII.decode(record.getData());
            s = charBuffer.toString();

            ObjectMapper mapper = new ObjectMapper();
            request = mapper.readValue(s, WeatherRequest.class);

        } catch (Exception e) {
            e.printStackTrace();
        }

        log.info("record data: " + s);



        return request;
    }

    public void writeToS3(String weatherJSON) {

        String keyName = UUID.randomUUID().toString();

        log.info("Uploading {} to S3 bucket {}...\n", keyName, properties.getBucketName());

        if (s3Client == null) {
            s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.fromName(properties.getS3RegionName()))
                    .withCredentials(new ProfileCredentialsProvider())
                    .build();
        }

        try {

            PutObjectResult result = s3Client.putObject(properties.getBucketName(), keyName, weatherJSON);
            log.info(result.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setS3Client(AmazonS3 s3Client) {
        this.s3Client = s3Client;
    }
}
