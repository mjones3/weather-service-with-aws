package io.melvinjones.weatherservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

@Component
public class OpenWeatherMapReader {

    private static final Logger log = LoggerFactory.getLogger(OpenWeatherMapReader.class);

    @Autowired
    WeatherProperties properties;

    public String getWeatherData(String zip) {

        String json = new String();

        String weatherURL = properties.getWeatherURL() +"zip=" + zip + "&appid=" + properties.getApiKey();

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
