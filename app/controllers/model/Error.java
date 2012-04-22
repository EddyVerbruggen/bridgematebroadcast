package controllers.model;

public class Error {

  public static final String ERROR_INVALID_REQUEST = "1";
  public static final String ERROR_NO_DATA_AVAILABLE = "2";
  public static final String ERROR_MATCH_FINISHED = "3";
  public static final String ERROR_UNKNOWN_REQUEST = "4";
  public static final String ERROR_INCORRECT_CREDENTIALS = "5";

  private String code;
  private String description;

  public Error(String code) {
    this.code = code;
    this.description = fillDescription(code);
  }
  
  private String fillDescription(String code) {
    if (ERROR_INVALID_REQUEST.equals(code)) {
      return "Invalid request";
    } else if (ERROR_NO_DATA_AVAILABLE.equals(code)) {
      return "No data available";
    } else if (ERROR_MATCH_FINISHED.equals(code)) {
      return "Match is finished";
    } else if (ERROR_UNKNOWN_REQUEST.equals(code)) {
      return "Unknown request";
    } else if (ERROR_INCORRECT_CREDENTIALS.equals(code)) {
      return "Credentials are incorrect, login failed";
    } else {
      return "";
    }
  }
}
