package it.icron.icronium.connector.rr.model;

public class LoginResponse {

    private boolean success;
    private String mode;
    private String userId;
    private String message;

    public LoginResponse() {
    }

    public LoginResponse(boolean success, String mode, String userId, String message) {
        this.success = success;
        this.mode = mode;
        this.userId = userId;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
