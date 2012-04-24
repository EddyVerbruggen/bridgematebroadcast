package controllers.model;

public class Response {

  private String request;
  private boolean success;
  private String responseType = "";
  private Error error;
  private Object response;

  public String getRequest() {
    return request;
  }

  public void setRequest(String request) {
    this.request = request;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public Error getError() {
    return error;
  }

  public void setError(Error error) {
    this.error = error;
  }

  public Object getResponse() {
    return response;
  }

  public void setResponse(Object response) {
    this.response = response;
  }

  public String getResponseType() {
    return responseType;
  }

  public void setResponseType(String responseType) {
    this.responseType = responseType;
  }
}
