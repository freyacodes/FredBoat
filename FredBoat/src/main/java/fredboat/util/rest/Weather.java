package fredboat.util.rest;

import fredboat.util.rest.models.weather.RetrievedWeather;

import javax.validation.constraints.NotNull;

/**
 * Interface for the command class to call the model to get different
 * implementation(s) of weather provider.
 *
 * To add other provider, just implement this interface for their data model.
 */
public interface Weather {
    RetrievedWeather getCurrentWeatherByCity(@NotNull String query);
}
