package fredboat.util.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fredboat.Config;
import fredboat.util.rest.models.weather.OpenWeatherCurrent;
import fredboat.util.rest.models.weather.RetrievedWeather;
import fredboat.util.rest.models.weather.WeatherError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class OpenWeatherAPI implements Weather {
    private static final Logger log = LoggerFactory.getLogger(OpenWeatherAPI.class);
    private static final String OPEN_WEATHER_BASE_URL = "https://api.openweathermap.org/data/2.5";
    private static final int MAX_CACHE_HOUR = 6;
    private static final int MAX_API_RATE = 60;
    private LoadingCache<String, RetrievedWeather> weatherCache;

    private final Bucket limitBucket;
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

        Refill refill = Refill.smooth(MAX_API_RATE, Duration.ofSeconds(60));
        Bandwidth limit = Bandwidth.classic(MAX_API_RATE, refill);
        limitBucket = Bucket4j.builder().addLimit(limit).build();

        weatherCache = CacheBuilder.newBuilder()
                .expireAfterWrite(MAX_CACHE_HOUR, TimeUnit.HOURS)
                .build(new CacheLoader<String, RetrievedWeather>() {
                    @Override
                    public RetrievedWeather load(@Nonnull String key) throws Exception {
                        return processGetWeatherByCity(key);
                    }
                });

    }

    /**
     * {@inheritDoc}
     */
    public RetrievedWeather getCurrentWeatherByCity(@Nonnull String query) throws APILimitException {
        // Strip all the query string that is not alphanumeric.
        query = query.replaceAll("[^A-Za-z0-9 ]", "");

        RetrievedWeather retrievedWeather = weatherCache.getUnchecked(query);

        if (retrievedWeather == null) {
            retrievedWeather = new WeatherError();
        }

        return retrievedWeather;
    }

    /**
     * Weather retrieving method.
     *
     * @param query String to query the weather.
     * @return Null if there is an error or retrieved weather.
     */
    private RetrievedWeather processGetWeatherByCity(@Nonnull String query) throws APILimitException {
        RetrievedWeather retrievedWeather = null;

        if (currentWeatherBaseUrl != null) {

            // Check if rate is exceeded.
            if (limitBucket.tryConsume(1)) {
                log.info("Retrieving " + query + " without queue.");
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
                            log.warn("Open weather search error status code " + response.code());
                            if (responseBody != null) {
                                log.warn(responseBody.string());
                            }
                            break;
                    }
                } catch (IOException e) {
                    log.warn("Open weather search: ", e);
                }
            } else {
                throw new APILimitException("OpenWeather API maximum rate exceeded.");
            }
        }
        return retrievedWeather;
    }
}
