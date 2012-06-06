package net.masterthought.jenkins.json;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.util.regexp.Regexp;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class Feature {

    private String name;
    private String uri;
    private String description;
    private String keyword;
    private Element[] elements;
    private Tag[] tags;

    public Feature(String name, String uri, String description, String keyword) {
        this.name = name;
        this.uri = uri;
        this.description = description;
        this.keyword = keyword;
    }

    public Element[] getElements() {
        return elements;
    }

    public boolean hasTags() {
        return Util.itemExists(tags);
    }

    public List<String> getTagList() {
        List<String> tagList = new ArrayList<String>();
        for (Tag tag : tags) {
            tagList.add(tag.getName());
        }
        return tagList;
    }

    public String getTags() {
        String result = "<div class=\"feature-tags\"></div>";
        if (Util.itemExists(tags)) {
            String tagList = StringUtils.join(getTagList().toArray(), ",");
            result = "<div class=\"feature-tags\">" + tagList + "</div>";
        }
        return result;
    }

    public Util.Status getStatus() {
        Closure<String, Element> scenarioStatus = new Closure<String, Element>() {
            public Util.Status call(Element step) {
                return step.getStatus();
            }
        };
        List<Util.Status> results = Util.collectScenarios(elements, scenarioStatus);
        return results.contains(Util.Status.FAILED) ? Util.Status.FAILED : Util.Status.PASSED;
    }

    private List<Util.Status> lookUpSteps() {
        List<Util.Status> stepStatuses = new ArrayList<Util.Status>();
        for (Element element : elements) {
            for (Step step : element.getSteps()) {
                stepStatuses.add(step.getStatus());
            }
        }
        return stepStatuses;
    }

    public String getName() {
        return Util.itemExists(name) ? Util.result(getStatus()) + "<div class=\"feature-line\"><span class=\"feature-keyword\">Feature:</span> " + name + "</div>" + Util.closeDiv() : "";
    }

    public String getRawName() {
        return Util.itemExists(name) ? name : "";
    }

    public String getDescription() {
        String result = "";
        if (Util.itemExists(description)) {
            String content = description.replaceFirst("As an", "<span class=\"feature-role\">As an</span>");
            content = content.replaceFirst("I want to", "<span class=\"feature-action\">I want to</span>");
            content = content.replaceFirst("So that", "<span class=\"feature-value\">So that</span>");
            content = content.replaceAll("\n", "<br/>");
            result = "<div class=\"feature-description\">" + content + "</div>";
        }
        return result;
    }

    public String getFileName() {
        List<String> matches = new ArrayList<String>();
        for (String line : Splitter.onPattern("/|\\\\").split(uri)) {
            String modified = line.replaceAll("\\)|\\(", "");
            modified = StringUtils.deleteWhitespace(modified).trim();
            matches.add(modified);
        }

        List<String> sublist = matches.subList(1, matches.size());

        matches = (sublist.size() == 0) ? matches : sublist;
        String fileName = Joiner.on("-").join(matches) + ".html";
        return fileName;
    }

    public int getNumberOfScenarios() {
        return elements.length;
    }

    public int getNumberOfSteps() {
        return lookUpSteps().size();
    }

    public int getNumberOfPasses() {
        return Util.findStatusCount(lookUpSteps(), Util.Status.PASSED);
    }

    public int getNumberOfFailures() {
        return Util.findStatusCount(lookUpSteps(), Util.Status.FAILED);
    }

    public int getNumberOfPending() {
        return Util.findStatusCount(lookUpSteps(), Util.Status.UNDEFINED);
    }

    public int getNumberOfSkipped() {
        return Util.findStatusCount(lookUpSteps(), Util.Status.SKIPPED);
    }

    public String getRawStatus() {
        return getStatus().toString().toLowerCase();
    }

    public String getDurationOfSteps() {
        Long totalDuration = 0L;
        for (Element element : elements) {
            for (Step step : element.getSteps()) {
                totalDuration = totalDuration + step.getDuration();
            }
        }
        return Util.formatDuration(totalDuration);
    }

    private String getTimestamp() {
        DateTime dateTime = new DateTime();
        return dateTime.getYear() + "" + dateTime.getMonthOfYear() + "" + dateTime.getDayOfMonth() + "" + dateTime.getHourOfDay() + "" + dateTime.getMinuteOfHour() + "" + dateTime.getSecondOfMinute() + "" + dateTime.getMillis();
    }

}



