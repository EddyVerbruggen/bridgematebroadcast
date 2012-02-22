package controllers;

import models.channel.Channel;
import models.channel.ChannelManager;
import play.Logger;
import play.libs.F;
import play.mvc.WebSocketController;

public class Broadcaster {
  public static F.EventStream liveStream = new F.EventStream();

  public static class WebSocket extends WebSocketController {

    public static boolean hasConnection;

    public static void stream() {
      if (request.querystring.contains("history=true")) {
        outbound.sendJson("pushing entire history as requested... (not implemented yet)");
      }
      Logger.info("request URL: " + request.url);
      Logger.info("request querystring: " + request.querystring);
      Logger.info("request routed id: " + request.routeArgs.get("id"));
      Long matchID = Long.parseLong(request.routeArgs.get("id"));

      String subscriber = "TEST";
      Channel channel = ChannelManager.getInstance().subscribe(subscriber, matchID, matchID, matchID);

      hasConnection = false;
      while (inbound.isOpen()) {
        hasConnection = true;
        try {
          Object obj = await(channel.nextEvent()); // using object, so we can publish anything.. although it's most likely a List<Model>
          if (obj != null) {
            outbound.sendJson(obj);
            Logger.info("enjoying sending a JSON object");
          }
        } catch (Throwable t) {
          Logger.error(t, t.getMessage());
          hasConnection = outbound.isOpen();
        }
      }
    }
  }
}
