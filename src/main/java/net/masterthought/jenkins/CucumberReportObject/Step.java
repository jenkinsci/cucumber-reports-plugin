package net.masterthought.jenkins.CucumberReportObject;

import com.fasterxml.jackson.annotation.*;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by steve.jensen on 11/12/16.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("org.jsonschema2pojo")
@JsonPropertyOrder({
        "result",
        "line",
        "name",
        "match",
        "keyword",
        "matchedColumns",
        "comments"
})
public class Step {

    @JsonProperty("result")
    private Result result;
    @JsonProperty("line")
    private Integer line;
    @JsonProperty("name")
    private String name;
    @JsonProperty("match")
    private Match match;
    @JsonProperty("keyword")
    private String keyword;
    @JsonProperty("matchedColumns")
    private List<Integer> matchedColumns = new ArrayList<Integer>();
    @JsonProperty("comments")
    private List<Comment> comments = new ArrayList<Comment>();
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
     * The line
     */
    @JsonProperty("line")
    public Integer getLine() {
        return line;
    }

    /**
     *
     * @param line
     * The line
     */
    @JsonProperty("line")
    public void setLine(Integer line) {
        this.line = line;
    }

    /**
     *
     * @return
     * The name
     */
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    /**
     *
     * @param name
     * The name
     */
    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return
     * The match
     */
    @JsonProperty("match")
    public Match getMatch() {
        return match;
    }

    /**
     *
     * @param match
     * The match
     */
    @JsonProperty("match")
    public void setMatch(Match match) {
        this.match = match;
    }

    /**
     *
     * @return
     * The keyword
     */
    @JsonProperty("keyword")
    public String getKeyword() {
        return keyword;
    }

    /**
     *
     * @param keyword
     * The keyword
     */
    @JsonProperty("keyword")
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     *
     * @return
     * The matchedColumns
     */
    @JsonProperty("matchedColumns")
    public List<Integer> getMatchedColumns() {
        return matchedColumns;
    }

    /**
     *
     * @param matchedColumns
     * The matchedColumns
     */
    @JsonProperty("matchedColumns")
    public void setMatchedColumns(List<Integer> matchedColumns) {
        this.matchedColumns = matchedColumns;
    }

    /**
     *
     * @return
     * The comments
     */
    @JsonProperty("comments")
    public List<Comment> getComments() {
        return comments;
    }

    /**
     *
     * @param comments
     * The comments
     */
    @JsonProperty("comments")
    public void setComments(List<Comment> comments) {
        this.comments = comments;
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
