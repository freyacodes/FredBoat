package fredboat.util.rest;

import fredboat.util.rest.models.weather.RetrievedWeather;

import javax.annotation.Nonnull;

/**
 * Interface for the command class to call the model to get different
 * implementation(s) of weather provider.
 * <p>
 * To add other provider, just implement this interface for their data model.
 */
public interface Weather {
    /**
     * Get current weather by the city.
     *
     * @param query Query for querying the weather.
     * @return Weather object that contains information from the query.
     * @throws APILimitException If the API rate has been exceeded.
     */
    RetrievedWeather getCurrentWeatherByCity(@Nonnull String query) throws APILimitException;
}
