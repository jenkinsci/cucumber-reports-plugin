package net.masterthought.jenkins.json;

import net.masterthought.jenkins.json.Closure;
import net.masterthought.jenkins.json.Element;
import net.masterthought.jenkins.json.Step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Util {

    public static boolean itemExists(List<String> listItem) {
        return listItem.size() != 0;
    }

    public static boolean itemExists(Tag[] tags) {
        boolean result = false;
        if (tags != null) {
            result = tags.length != 0;
        }
        return result;
    }

    public static String passed(boolean value) {
        return value ? "<div class=\"passed\">" : "</div>";
    }

    public static String closeDiv() {
        return "</div>";
    }

    public static Map<String, Status> resultMap = new HashMap<String, Status>() {{
        put("passed", Util.Status.PASSED);
        put("failed", Util.Status.FAILED);
        put("skipped", Util.Status.SKIPPED);
    }};

    public static String result(Status status) {
        String result = "";
        if (status == Status.PASSED) {
            result = "<div class=\"passed\">";
        } else if (status == Status.FAILED) {
            result = "<div class=\"failed\">";
        } else if (status == Status.SKIPPED) {
            result = "<div class=\"skipped\">";
        }
        return result;
    }

    public static enum Status {
        PASSED, FAILED, SKIPPED
    }

    public static <T, R> List<R> collectScenarios(Element[] list, Closure<String, Element> clo) {
        List<R> res = new ArrayList<R>();
        for (final Element t : list) {
            res.add((R) clo.call(t));
        }
        return res;
    }

    public static <T, R> List<R> collectSteps(Step[] list, Closure<String, Step> clo) {
        List<R> res = new ArrayList<R>();
        for (final Step t : list) {
            res.add((R) clo.call(t));
        }
        return res;
    }

    public static <T, R> List<R> collectTags(Tag[] list, StringClosure<String, Tag> clo) {
        List<R> res = new ArrayList<R>();
        for (final Tag t : list) {
            res.add((R) clo.call(t));
        }
        return res;
    }

    public static boolean itemExists(String item) {
        return !(item.isEmpty() || item == null);
    }

    public static int findStatusCount(List<Util.Status> statuses, Status statusToFind) {
        int occurrence = 0;
        for(Util.Status status : statuses){
            if(status == statusToFind){
                occurrence++;
            }
        }
       return occurrence;
    }
}
