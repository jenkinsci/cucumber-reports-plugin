package net.masterthought.jenkins.json;

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
    
    public Row[] getRows(){
        return rows;
    }

    public boolean hasRows(){
      boolean result = false;
      if(rows != null){
          if(rows.length > 0){
              result = true;
          }
      }
        return result;
    }
    
    public Long getDuration(){
        return result.getDuration();
    }
    
    public Util.Status getStatus() {
        return Util.resultMap.get(result.getStatus());
    }
    
    public String getDataTableClass(){
        String content = "";
        Util.Status status = getStatus();
        if(status == Util.Status.FAILED){
            content = "failed";
        } else if(status == Util.Status.PASSED){
            content = "passed";
        } else if(status == Util.Status.SKIPPED){
            content = "skipped";
        } else {
            content = ""; 
        }
        return content;   
    }

    public String getName(){
        String content = "";
        if(getStatus() == Util.Status.FAILED){
            content = Util.result(getStatus()) + "<span class=\"step-keyword\">" + keyword + " </span><span class=\"step-name\">" + name + "</span>" + "<div class=\"step-error-message\"><pre>" + result.getErrorMessage() + "</pre></div>" +  Util.closeDiv();
        } else {
            content = Util.result(getStatus()) + "<span class=\"step-keyword\">" + keyword + " </span><span class=\"step-name\">" + name + "</span>" + Util.closeDiv();
        }
        return content;
    }

}
