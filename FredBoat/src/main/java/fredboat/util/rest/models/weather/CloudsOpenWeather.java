package fredboat.util.rest.models.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "all"
})
@JsonIgnoreProperties(ignoreUnknown = true)

/*
 * Open weather data model.
 */
public class CloudsOpenWeather {

    @JsonProperty("all")
    private int all;

    @JsonProperty("all")
    public int getAll() {
        return all;
    }

    @JsonProperty("all")
    public void setAll(int all) {
        this.all = all;
    }
}
