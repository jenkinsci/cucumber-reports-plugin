package net.masterthought.jenkins.json;

public class Step {

    private String name;
    private String keyword;
    private Result result;

    public Step(String name, String keyword) {
        this.name = name;
        this.keyword = keyword;

    }

    public Util.Status getStatus() {
        return Util.resultMap.get(result.getStatus());
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
