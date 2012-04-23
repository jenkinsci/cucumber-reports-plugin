package net.masterthought.jenkins.json;

import java.util.ArrayList;
import java.util.List;

public class Feature {

    private String name;
    private String uri;
    private String description;
    private String keyword;
    private Element[] elements;

    public Feature(String name, String uri, String description, String keyword) {
        this.name = name;
        this.uri = uri;
        this.description = description;
        this.keyword = keyword;
    }

    public Element[] getElements() {
        return elements;
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
        for(Element element : elements){
            for(Step step : element.getSteps()){
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
	    return uri.replaceAll("/", "-").replaceAll("\\\\", "-") + ".html";
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

    public int getNumberOfSkipped() {
        return Util.findStatusCount(lookUpSteps(), Util.Status.SKIPPED);
    }

    public String getRawStatus() {
        return getStatus().toString().toLowerCase();
    }

}



