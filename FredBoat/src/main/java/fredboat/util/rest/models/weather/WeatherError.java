package fredboat.util.rest.models.weather;

/*
 * Weather data model for error.
 */
public class WeatherError implements RetrievedWeather {
    @Override
    public boolean isError() {
        return true;
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
}
