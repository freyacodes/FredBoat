package fredboat.util.rest.models.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "temp",
        "pressure",
        "humidity",
        "temp_min",
        "temp_max"
})
@JsonIgnoreProperties(ignoreUnknown = true)

/*
 * Open weather data model.
 */
public class WeatherMainOpenWeather {

    @JsonProperty("temp")
    private double temp;
    @JsonProperty("pressure")
    private int pressure;
    @JsonProperty("humidity")
    private int humidity;
    @JsonProperty("temp_min")
    private double tempMin;
    @JsonProperty("temp_max")
    private double tempMax;

    @JsonProperty("temp")
    public double getTemp() {
        return temp;
    }

    @JsonProperty("temp")
    public void setTemp(double temp) {
        this.temp = temp;
    }

    @JsonProperty("pressure")
    public int getPressure() {
        return pressure;
    }

    @JsonProperty("pressure")
    public void setPressure(int pressure) {
        this.pressure = pressure;
    }

    @JsonProperty("humidity")
    public int getHumidity() {
        return humidity;
    }

    @JsonProperty("humidity")
    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    @JsonProperty("temp_min")
    public double getTempMin() {
        return tempMin;
    }

    @JsonProperty("temp_min")
    public void setTempMin(double tempMin) {
        this.tempMin = tempMin;
    }

    @JsonProperty("temp_max")
    public double getTempMax() {
        return tempMax;
    }

    @JsonProperty("temp_max")
    public void setTempMax(double tempMax) {
        this.tempMax = tempMax;
    }
}
