package io.melvinjones.weatherservice;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;


public class WeatherRequest
{

    private final static ObjectMapper JSON = new ObjectMapper();

    String zip;

    public static WeatherRequest fromJsonAsBytes(byte[] bytes) {
        try {
            return JSON.readValue(bytes, WeatherRequest.class);
        } catch (IOException e) {
            return null;
        }
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }


}
