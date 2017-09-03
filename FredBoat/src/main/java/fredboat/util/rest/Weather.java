package fredboat.util.rest;

import fredboat.util.rest.models.weather.RetrievedWeather;

import javax.validation.constraints.NotNull;

public interface Weather {
    RetrievedWeather getCurrentWeatherByCity(@NotNull String query);
}
