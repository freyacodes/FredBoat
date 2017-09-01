package fredboat.util.rest.models.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "weather",
        "base",
        "main",
        "visibility",
        "clouds",
        "dt",
        "weatherSystem",
        "id",
        "name",
        "cod"
})
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenWeatherCurrent implements RetrievedWeather {

    @JsonProperty("weather")
    private List<OpenWeather> weather = null;
    @JsonProperty("base")
    private String base;
    @JsonProperty("main")
    private WeatherMain weatherMain;
    @JsonProperty("visibility")
    private int visibility;
    @JsonProperty("clouds")
    private Clouds clouds;
    @JsonProperty("dt")
    private int dt;
    @JsonProperty("sys")
    private WeatherSystem weatherSystem;
    @JsonProperty("id")
    private int id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("cod")
    private int cod;

    @JsonProperty("weather")
    public List<OpenWeather> getWeather() {
        return weather;
    }

    @JsonProperty("weather")
    public void setWeather(List<OpenWeather> weather) {
        this.weather = weather;
    }

    @JsonProperty("main")
    public WeatherMain getMain() {
        return weatherMain;
    }

    @JsonProperty("main")
    public void setMain(WeatherMain weatherMain) {
        this.weatherMain = weatherMain;
    }

    @JsonProperty("visibility")
    public int getVisibility() {
        return visibility;
    }

    @JsonProperty("visibility")
    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }

    @JsonProperty("clouds")
    public Clouds getClouds() {
        return clouds;
    }

    @JsonProperty("clouds")
    public void setClouds(Clouds clouds) {
        this.clouds = clouds;
    }

    @JsonProperty("dt")
    public int getDt() {
        return dt;
    }

    @JsonProperty("dt")
    public void setDt(int dt) {
        this.dt = dt;
    }

    @JsonProperty("sys")
    public WeatherSystem getSys() {
        return weatherSystem;
    }

    @JsonProperty("sys")
    public void setSys(WeatherSystem sys) {
        this.weatherSystem = sys;
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

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("cod")
    public int getCod() {
        return cod;
    }

    @JsonProperty("cod")
    public void setCod(int cod) {
        this.cod = cod;
    }

    @Override
    public String getLocation() {
        return name;
    }

    @Override
    public String getWeatherDescription() {
        if (weather.size() > 0) {
            return weather.get(0).getMain() + " - " + weather.get(0).getDescription();
        }
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
