package io.melvinjones.weatherservice;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WeatherRequest {

    @JsonProperty("zip")
    private String zip;

    @JsonProperty("zip")
    public String getZip() {
        return zip;
    }

    @JsonProperty("zip")
    public void setZip(String foo) {
        this.zip = foo;
    }
}
