package controllers.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Parses queryString entered during the Websocket connection
 */
public class QueryStringParser {
  
  private String queryString;
  
  private String command;
  private List<String> params = new ArrayList<String>();
  
  public QueryStringParser(String queryString) {
    this.queryString = queryString;
    parse();
  }
  
  private void parse() {
    int indexOfFirstBracket = queryString.indexOf("(");

    if (indexOfFirstBracket == -1) {
      command = queryString;
    } else {
      this.command = queryString.substring(0, indexOfFirstBracket);
      String paramString = queryString.substring(indexOfFirstBracket + 1, queryString.indexOf(")"));
      StringTokenizer tokenizer = new StringTokenizer(paramString, ",");
      while (tokenizer.hasMoreTokens()) {
        params.add(tokenizer.nextToken());
      }
    }
  }

  public String getCommand() {
    return command;
  }

  public List<String> getParams() {
    return params;
  }
}
