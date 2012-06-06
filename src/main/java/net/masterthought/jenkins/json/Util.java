package net.masterthought.jenkins.json;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
        put("undefined", Util.Status.UNDEFINED);
        put("missing", Util.Status.MISSING);
    }};

    public static String result(Status status) {
        String result = "<div>";
        if (status == Status.PASSED) {
            result = "<div class=\"passed\">";
        } else if (status == Status.FAILED) {
            result = "<div class=\"failed\">";
        } else if (status == Status.SKIPPED) {
            result = "<div class=\"skipped\">";
        } else if (status == Status.UNDEFINED) {
            result = "<div class=\"undefined\">";
        } else if (status == Status.MISSING) {
            result = "<div class=\"missing\">";
        }
        return result;
    }

    public static enum Status {
        PASSED, FAILED, SKIPPED, UNDEFINED, MISSING
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
        for (Util.Status status : statuses) {
            if (status == statusToFind) {
                occurrence++;
            }
        }
        return occurrence;
    }

    public static String readFileAsString(String filePath) throws java.io.IOException {
        byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        } finally {
            if (f != null) try {
                f.close();
            } catch (IOException ignored) {
            }
        }
        return new String(buffer);
    }

    public static String formatDuration(Long duration) {
        PeriodFormatter formatter = new PeriodFormatterBuilder()
                .appendDays()
                .appendSuffix(" day", " days")
                .appendSeparator(" and ")
                .appendHours()
                .appendSuffix(" hour", " hours")
                .appendSeparator(" and ")
                .appendMinutes()
                .appendSuffix(" min", " mins")
                .appendSeparator(" and ")
                .appendSeconds()
                .appendSuffix(" sec", " secs")
                .appendSeparator(" and ")
                .appendMillis()
                .appendSuffix(" ms", " ms")
                .toFormatter();
        return formatter.print(new Period(0, duration / 1000000));


    }
}
