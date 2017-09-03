package fredboat.util.rest.models.weather;

public interface RetrievedWeather {
    boolean IsError();
    String getLocation();
    String getWeatherDescription();
    String getFormattedDate();
    String getTemperature();
}
