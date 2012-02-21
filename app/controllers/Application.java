package controllers;

import play.mvc.Controller;

public class Application extends Controller {

  public static void index() {
    render();
  }

    public static void test() {
        render();
    }

    public static void noop() {
    }

}