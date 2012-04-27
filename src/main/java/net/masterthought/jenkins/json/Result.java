package net.masterthought.jenkins.json;

public class Result {

    private String status;
    private String error_message;
    private Long duration;

    public Result(String status, String error_message, Long duration){
        this.status = status;
        this.error_message = error_message;
        this.duration = duration;
    }

    public String getStatus(){
      return status;
    }

    public Long getDuration(){
        return duration == null ? 0L : duration;
    }
    
    public String getErrorMessage(){
        return error_message;
    }

}
