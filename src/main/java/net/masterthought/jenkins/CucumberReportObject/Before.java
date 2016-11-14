package net.masterthought.jenkins.CucumberReportObject;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by steve.jensen on 11/12/16.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "result",
        "match"
})
public class Before {

    @JsonProperty("result")
    private Result result;
    @JsonProperty("match")
    private After.Match_ match;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    /**
     *
     * @return
     * The result
     */
    @JsonProperty("result")
    public Result getResult() {
        return result;
    }

    /**
     *
     * @param result
     * The result
     */
    @JsonProperty("result")
    public void setResult(Result result) {
        this.result = result;
    }

    /**
     *
     * @return
     * The match
     */
    @JsonProperty("match")
    public After.Match_ getMatch() {
        return match;
    }

    /**
     *
     * @param match
     * The match
     */
    @JsonProperty("match")
    public void setMatch(After.Match_ match) {
        this.match = match;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
