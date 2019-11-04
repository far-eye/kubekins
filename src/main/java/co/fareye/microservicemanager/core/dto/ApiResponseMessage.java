package co.fareye.microservicemanager.core.dto;

public class ApiResponseMessage {

    private String message;

    public ApiResponseMessage()
    {
        message = "Not Found";
    }

    public ApiResponseMessage(String message)
    {
        this.message = message;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

