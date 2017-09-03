package fredboat.util.rest.models.weather;

public class WeatherError implements RetrievedWeather {
    @Override
    public boolean IsError() {
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
}
