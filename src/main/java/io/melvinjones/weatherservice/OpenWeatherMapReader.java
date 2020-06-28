package io.melvinjones.weatherservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class OpenWeatherMapReader {

    private static final Logger log = LoggerFactory.getLogger(GetRequestService.class);

    public String getWeatherData(String zip) {

        String json = new String();

        String apiKey = "b20dd9b79f7b37e0c2c5d5ef9d1c782e";

        String weatherURL = "http://api.openweathermap.org/data/2.5/weather?zip=" + zip + "&appid=" + apiKey;

        try {
            URL url = new URL(weatherURL);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    log.info(line);
                    json += line;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;


    }

}
