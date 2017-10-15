package fredboat.util.rest.models.weather;

/*
 * Weather data model for error.
 */
public class WeatherError implements RetrievedWeather {
    private ErrorCode errorCode;

    public WeatherError(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public boolean isError() {
        return true;
    }

    @Override
    public ErrorCode errorType() {
        return this.errorCode;
    }

    @Override
    public String getLocation() {
        return "";
    }

    @Override
    public String getWeatherDescription() {
        return "";
    }

    @Override
    public String getFormattedDate() {
        return "";
    }

    @Override
    public String getTemperature() {
        return "";
    }

    @Override
    public String getThumbnailUrl() {
        return "";
    }

    @Override
    public String getDataProviderIcon() {
        return "";
    }

    @Override
    public String getDataProviderString() {
        return "";
    }
}
