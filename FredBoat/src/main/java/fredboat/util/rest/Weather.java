package fredboat.util.rest;

import fredboat.util.rest.models.weather.RetrievedWeather;

public interface Weather {
    RetrievedWeather getCurrentWeatherByCity();
}
