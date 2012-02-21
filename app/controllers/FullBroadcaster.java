package controllers;

import models.channel.Channel;
import models.channel.ChannelManager;
import play.Logger;
import play.libs.F;
import play.mvc.Http;
import play.mvc.WebSocketController;

import play.*;
import play.mvc.*;
import play.libs.*;
import play.libs.F.*;
import play.mvc.Http.*;

import static play.libs.F.*;
import static play.libs.F.Matcher.*;
import static play.mvc.Http.WebSocketEvent.*;


public class FullBroadcaster {

  public static class WebSocket extends WebSocketController {

    private static Channel subscriptionChannel;

    public static void stream() {

      String subscriber = "TEST";

      Logger.info("entered new fullbroadcaster");

//      channel = ChannelManager.getInstance().subscribe(matchID);

      while (inbound.isOpen()) {
        Logger.info("awaiting input...");
        WebSocketEvent clientEvent = null;

        if (subscriptionChannel == null) {
          Logger.info("awaiting input only from client...");
          // Not yet subscribed, so no subscriptionChannel. Waiting for client event.
          clientEvent = await(inbound.nextEvent());
        } else {
          Logger.info("awaiting input from both client and subscriptionChannel = " + subscriptionChannel.toString());
          // Wait for an event (either something coming on the inbound socket channel, or subscriptionChannel messages)
          F.Either<WebSocketEvent, F.Promise> either = (Either<WebSocketEvent, Promise>)
            await(Promise.waitEither(
              inbound.nextEvent(),
              subscriptionChannel.nextEvent()));

          if (either._1.isDefined()) {
            // inbound socket channel event received
            clientEvent = either._1.get();
          }
          if (either._2.isDefined()) {
            // subscriptionChannel event received, send to client
            outbound.sendJson(either._2.get());
            Logger.info("enjoying sending a JSON object");
          }
        }

        if (clientEvent != null) {
          Logger.info("received clientEvent " + clientEvent);
          // handle clientEvent
          handleClientEvent(subscriber, clientEvent);
        }
      }
    }

    private static void handleClientEvent(String subscriber, WebSocketEvent clientEvent) {
      Logger.info("handleClientEvent " + clientEvent);

      for(String userMessage: TextFrame.match(clientEvent)) {

        if ("subscribe101".equals(userMessage)) {
          Logger.info("Subscribing to channel 101");
          subscriptionChannel = ChannelManager.getInstance().subscribe(subscriber, 101L);
        } else {
          handleUserInput(subscriber, userMessage);
        }
      }
      
      // Case: The socket has been closed
      for(WebSocketClose closed: SocketClosed.match(clientEvent)) {
        quitAndUnsubscribe(subscriber);
      }
    }

    private static void quitAndUnsubscribe(String subscriber) {
      if (subscriptionChannel != null) {
        ChannelManager.getInstance().unsubscribe(subscriptionChannel, subscriber);
      }
      disconnect();
    }

    private static void handleUserInput(String subscriber, String userMessage) {
      Logger.info("handleUserInput " + userMessage);
      if ("quit".equals(userMessage)) {
        outbound.sendJson("Quitting....");
        quitAndUnsubscribe(subscriber);
      }
      outbound.sendJson(userMessage);
    }
  }
}
