package fredboat.util.rest.models.weather;

/**
 * Interface for getting weather data.
 */
public interface RetrievedWeather {

    /**
     * Error indication for retrieving weather.
     *
     * @return True if there is an error, false if successful.
     */
    boolean isError();

    /**
     * Get the location of the search result.
     *
     * @return Location from the search result or empty string if there is an error.
     */
    String getLocation();

    /**
     * Get the weather description.
     *
     * @return Weather description, or empty string is there is an error.
     */
    String getWeatherDescription();

    /**
     * Get date as string from the search result.
     *
     * @return Date time in string format or empty string if there is an error.
     */
    String getFormattedDate();

    /**
     * Get search result weather temperature.
     *
     * @return String representation of temperature or empty string if there is an error.
     */
    String getTemperature();
}
