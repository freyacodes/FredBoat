package fredboat.util.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import fredboat.Config;
import fredboat.util.rest.models.weather.OpenWeatherCurrent;
import fredboat.util.rest.models.weather.RetrievedWeather;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class OpenWeatherAPI implements Weather {
    private static final Logger log = LoggerFactory.getLogger(OpenWeatherAPI.class);
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5";
    protected OkHttpClient client;

    public OpenWeatherAPI(Interceptor interceptor) {
        client = new OkHttpClient();
    }

    public RetrievedWeather getCurrentWeatherByCity() {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(BASE_URL + "/weather")
                .newBuilder();

        urlBuilder.addQueryParameter("q", "san francisco");
        urlBuilder.addQueryParameter("appid", Config.CONFIG.getOpenWeatherKey());

        HttpUrl url = urlBuilder.build();
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (response.code() == 200) {
                if (responseBody != null) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    OpenWeatherCurrent currentWeather = objectMapper.readValue(responseBody.string(), OpenWeatherCurrent.class);
                    log.debug(currentWeather.toString());
                    return currentWeather;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
