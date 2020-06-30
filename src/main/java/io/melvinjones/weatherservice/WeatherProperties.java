package io.melvinjones.weatherservice;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ConfigurationProperties // no prefix, root level.
public class WeatherProperties


{
    @Value("${aws.kinesis.streamName}")
    private String streamName;

    @Value("${aws.kinesis.region}")
    private String kinesisRegionName;

    @Value("${aws.kinesis.applicationName}")
    private String applicationName;

    @Value("${aws.s3.region}")
    private String s3RegionName;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.url}")
    private String weatherURL;

    public String getStreamName() {
        return streamName;
    }

    public void setStreamName(String streamName) {
        this.streamName = streamName;
    }

    public String getKinesisRegionName() {
        return kinesisRegionName;
    }

    public void setKinesisRegionName(String kinesisRegionName) {
        this.kinesisRegionName = kinesisRegionName;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getS3RegionName() {
        return s3RegionName;
    }

    public void setS3RegionName(String s3RegionName) {
        this.s3RegionName = s3RegionName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWeatherURL() {
        return weatherURL;
    }

    public void setWeatherURL(String weatherURL) {
        this.weatherURL = weatherURL;
    }

    @Override
    public String toString() {
        return "WeatherProperties{" +
                "streamName='" + streamName + '\'' +
                ", kinesisRegionName='" + kinesisRegionName + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", s3RegionName='" + s3RegionName + '\'' +
                ", bucketName='" + bucketName + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", weatherURL='" + weatherURL + '\'' +
                '}';
    }
}
