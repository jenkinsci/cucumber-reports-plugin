package net.masterthought.jenkins.json;

import net.masterthought.jenkins.ConfigurationOptions;

import java.util.Arrays;
import java.util.List;

public class Step {

    private String name;
    private String keyword;
    private Result result;
    private Row[] rows;

    public Step(String name, String keyword) {
        this.name = name;
        this.keyword = keyword;

    }

    public Row[] getRows() {
        return rows;
    }

    public boolean hasRows() {
        boolean result = false;
        if (rows != null) {
            if (rows.length > 0) {
                result = true;
            }
        }
        return result;
    }

    public Long getDuration() {
        return result.getDuration();
    }

    private Util.Status getInternalStatus() {
        return Util.resultMap.get(result.getStatus());
    }

    public Util.Status getStatus() {
//        return Util.resultMap.get(result.getStatus());

        Util.Status status = getInternalStatus();
        Util.Status result = status;

        if (ConfigurationOptions.skippedFailsBuild()) {
            if (status == Util.Status.SKIPPED || status == Util.Status.FAILED) {
                result = Util.Status.FAILED;
            }
        }

        if (ConfigurationOptions.undefinedFailsBuild()) {
            if (status == Util.Status.UNDEFINED || status == Util.Status.FAILED) {
                result = Util.Status.FAILED;
            }
        }

        if (status == Util.Status.FAILED) {
            result = Util.Status.FAILED;
        }
        return result;

    }

    public String getDataTableClass() {
        String content = "";
        Util.Status status = getStatus();
        if (status == Util.Status.FAILED) {
            content = "failed";
        } else if (status == Util.Status.PASSED) {
            content = "passed";
        } else if (status == Util.Status.SKIPPED) {
            content = "skipped";
        } else {
            content = "";
        }
        return content;
    }

    public String getName() {
        String content = "";
        if (getStatus() == Util.Status.FAILED) {
            String errorMessage = result.getErrorMessage();
            if (getInternalStatus() == Util.Status.SKIPPED) {
                errorMessage = "Mode: Skipped causes Failure<br/><span class=\"skipped\">This step was skipped</span>";
            }
            if (getInternalStatus() == Util.Status.UNDEFINED) {
                errorMessage = "Mode: Not Implemented causes Failure<br/><span class=\"undefined\">This step is not yet implemented</span>";
            }
            content = Util.result(getStatus()) + "<span class=\"step-keyword\">" + keyword + " </span><span class=\"step-name\">" + name + "</span>" + "<div class=\"step-error-message\"><pre>" + errorMessage + "</pre></div>" + Util.closeDiv();
        } else {
            content = Util.result(getStatus()) + "<span class=\"step-keyword\">" + keyword + " </span><span class=\"step-name\">" + name + "</span>" + Util.closeDiv();
        }
        return content;
    }

}
