package fredboat.util.rest.models.weather;

/**
 * Interface for getting weather data.
 */
public interface RetrievedWeather {
    /**
     * Error code to specify what kind of error.
     */
    enum ErrorCode {
        LOCATION_NOT_FOUND,
        SOMETHING_IS_WRONG,
        NO_ERROR
    }

    /**
     * Error indication for retrieving weather.
     *
     * @return True if there is an error, false if successful.
     */
    boolean isError();

    /**
     * Error code indicating what error.
     *
     * @return {@link ErrorCode}
     */
    ErrorCode errorType();

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

    /**
     * Get thumbnail url (if available).
     *
     * @return String representation of thumbnail url or empty string if there is an error.
     */
    String getThumbnailUrl();

    /**
     * Get icon url (if available).
     *
     * @return String representation of icon url or empty string if there is an error.
     */
    String getDataProviderIcon();

    /**
     * Get data provider credit string.
     *
     * @return String representation of data provider credit or empty string if there is an error.
     */
    String getDataProviderString();
}
