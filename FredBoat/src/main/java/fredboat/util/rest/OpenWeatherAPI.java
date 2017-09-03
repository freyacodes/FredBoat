package fredboat.util.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import fredboat.Config;
import fredboat.util.rest.models.weather.OpenWeatherCurrent;
import fredboat.util.rest.models.weather.RetrievedWeather;
import fredboat.util.rest.models.weather.WeatherError;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;

public class OpenWeatherAPI implements Weather {
    private static final Logger log = LoggerFactory.getLogger(OpenWeatherAPI.class);
    private static final String OPEN_WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5";
    protected OkHttpClient client;
    private ObjectMapper objectMapper;
    private HttpUrl currentWeatherBaseUrl;

    public OpenWeatherAPI() {
        client = new OkHttpClient();
        objectMapper = new ObjectMapper();

        currentWeatherBaseUrl = HttpUrl.parse(OPEN_WEATHER_BASE_URL + "/weather");
        if (currentWeatherBaseUrl == null) {
            log.debug("Open weather search unable to build URL");
        }
    }

    public RetrievedWeather getCurrentWeatherByCity(@NotNull String query) {
        RetrievedWeather retrievedWeather = null;

        if (currentWeatherBaseUrl != null) {
            // Strip all the query string that is not alphanumeric.
            query = query.replaceAll("[^A-Za-z0-9 ]", "");
            HttpUrl.Builder urlBuilder = currentWeatherBaseUrl.newBuilder();

            urlBuilder.addQueryParameter("q", query);
            urlBuilder.addQueryParameter("appid", Config.CONFIG.getOpenWeatherKey());

            HttpUrl url = urlBuilder.build();
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                ResponseBody responseBody = response.body();

                switch (response.code()) {
                    case 200:
                        if (responseBody != null) {
                            retrievedWeather = objectMapper.readValue(responseBody.string(), OpenWeatherCurrent.class);
                        }
                        break;

                    default:
                        log.debug("Open weather search error status code ", response.code());
                        break;
                }
            } catch (IOException e) {
                log.debug("Open weather search: ", e);
            }
        }

        if (retrievedWeather == null) {
            retrievedWeather = new WeatherError();
        }

        return retrievedWeather;
    }
}
