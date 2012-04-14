package net.masterthought.jenkins.json;

public class Result {

    private String status;
    private String error_message;
    private int duration;

    public Result(String status, String error_message, int duration){
        this.status = status;
        this.error_message = error_message;
        this.duration = duration;
    }

    public String getStatus(){
      return status;
    }

    public String getErrorMessage(){
        return error_message;
    }

}
