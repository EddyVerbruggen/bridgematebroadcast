package controllers;

import controllers.model.Response;
import controllers.model.Error;
import controllers.util.QueryStringParser;

public class ResponseBuilder {
  
  public static Response createErrorResponse(QueryStringParser parser, Error error) {
    Response response = new Response();
    response.setRequest(parser.getQueryString());
    response.setSuccess(false);
    response.setError(error);
    return response;
  }

  public static Response createErrorResponse(String queryString, Error error) {
    Response response = new Response();
    response.setRequest(queryString);
    response.setSuccess(false);
    response.setError(error);
    return response;
  }

  public static Response createDataResponse(String request, String responseType, Object data) {
    Response response = new Response();
    response.setRequest(request);
    response.setSuccess(true);
    response.setResponseType(responseType);
    response.setResponse(data);
    return response;
  }

  public static Response createDataResponse(QueryStringParser parser, String responseType, Object data) {
    Response response = new Response();
    response.setRequest(parser.getQueryString());
    response.setSuccess(true);
    response.setResponseType(responseType);
    response.setResponse(data);
    return response;
  }
}
