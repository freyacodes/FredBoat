package fredboat.util.rest.models.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "weather",
        "base",
        "main",
        "visibility",
        "clouds",
        "dt",
        "weatherSystemOpenWeather",
        "id",
        "name",
        "cod"
})
@JsonIgnoreProperties(ignoreUnknown = true)

/*
 * Open weather data model.
 */
public class OpenWeatherCurrent implements RetrievedWeather {

    // Url for retrieving thumbnails.
    private static final String THUMBNAIL_ICON_URL = "https://openweathermap.org/img/w/%s.png";
    private static final String PROVIDER_ICON_URL = "https://i.imgur.com/YqZuqEB.jpg";
    private static final String PROVIDER_CREDIT_STRING = "Provided by OpenWeatherMap.org";

    @JsonProperty("weather")
    private List<WeatherOpenWeather> weather;
    @JsonProperty("base")
    private String base;
    @JsonProperty("main")
    private WeatherMainOpenWeather weatherMainOpenWeather;
    @JsonProperty("visibility")
    private int visibility;
    @JsonProperty("clouds")
    private CloudsOpenWeather cloudsOpenWeather;
    @JsonProperty("dt")
    private int datetime;
    @JsonProperty("sys")
    private WeatherSystemOpenWeather weatherSystemOpenWeather;
    @JsonProperty("id")
    private int id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("cod")
    private int statusCode;

    private SimpleDateFormat simpleDateFormat;

    public OpenWeatherCurrent() {
        weather = new ArrayList<>();
        simpleDateFormat = new SimpleDateFormat("EEE, d MMM yyyy, hh:mm aaa");
    }

    @JsonProperty("weather")
    public List<WeatherOpenWeather> getWeather() {
        return weather;
    }

    @JsonProperty("main")
    public WeatherMainOpenWeather getMain() {
        return weatherMainOpenWeather;
    }

    @JsonProperty("visibility")
    public int getVisibility() {
        return visibility;
    }

    @JsonProperty("clouds")
    public CloudsOpenWeather getClouds() {
        return cloudsOpenWeather;
    }

    @JsonProperty("dt")
    public int getDatetime() {
        return datetime;
    }

    @JsonProperty("sys")
    public WeatherSystemOpenWeather getSys() {
        return weatherSystemOpenWeather;
    }

    @JsonProperty("id")
    public int getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(int id) {
        this.id = id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("cod")
    public int getCode() {
        return statusCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isError() {
        return statusCode != 200;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ErrorCode errorType() {
        return ErrorCode.NO_ERROR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocation() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWeatherDescription() {
        if (weather.size() > 0) {
            return weather.get(0).getMain() + " - " + weather.get(0).getDescription();
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFormattedDate() {
        return simpleDateFormat.format(new Date((long) getDatetime() * 1000));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTemperature() {
        return String.format("%.2f C / %.2f F", getCelsius(), getFahrenheit());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getThumbnailUrl() {

        String iconName = "";
        String url = "";
        if (weather != null && weather.size() > 0) {
            iconName = weather.get(0).getIcon();
        }
        if (iconName.length() != 0) {
            url = String.format(THUMBNAIL_ICON_URL, iconName);
        }
        return url;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataProviderIcon() {
        return PROVIDER_ICON_URL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDataProviderString() {
        return PROVIDER_CREDIT_STRING;
    }

    private double getFahrenheit() {
        double fahrenheit = 0;
        double kelvin = weatherMainOpenWeather.getTemp();
        fahrenheit = (((kelvin - 273) * 9d / 5) + 32);
        return fahrenheit;
    }

    private double getCelsius() {
        double celsius = 0;
        double kelvin = weatherMainOpenWeather.getTemp();
        celsius = kelvin - 273.16;
        return celsius;
    }
}
