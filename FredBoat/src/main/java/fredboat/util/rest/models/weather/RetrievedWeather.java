package fredboat.util.rest.models.weather;

public interface RetrievedWeather {
    String getLocation();
    String getWeatherDescription();
    String getFormattedDate();
    String getTemperature();
}
