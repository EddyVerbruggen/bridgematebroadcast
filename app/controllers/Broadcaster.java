package controllers;

import play.Logger;
import play.libs.F;
import play.mvc.WebSocketController;

public class Broadcaster {
  public static F.EventStream liveStream = new F.EventStream();

  public static class WebSocket extends WebSocketController {

    public static boolean hasConnection;

    public static void stream() {
      if (true) {



        outbound.sendJson("history (not using stream)");
      }
      Logger.info("request URL: " + request.url);
      Logger.info("request querystring: " + request.querystring);
      Logger.info("request routed id: " + request.routeArgs.get("id"));
      hasConnection = false;
      while (inbound.isOpen()) {
        hasConnection = true;
        try {
          Object obj = await(liveStream.nextEvent()); // using object, so we can publish anything.. although it's most likely a List<Model>
          if (obj != null) {
//            Logger.info("Publishing " + obj + " to subscribers", obj);
            outbound.sendJson(obj);
          }
        } catch (Throwable t) {
          Logger.error(t, t.getMessage());
          hasConnection = outbound.isOpen();
        }
      }
    }
  }
}
