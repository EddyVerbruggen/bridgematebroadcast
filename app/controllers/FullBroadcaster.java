package controllers;

import controllers.util.QueryStringParser;
import models.*;
import models.channel.Channel;
import models.channel.ChannelManager;
import play.Logger;
import play.libs.F;
import play.mvc.WebSocketController;

import play.libs.F.*;
import play.mvc.Http.*;

import static play.mvc.Http.WebSocketEvent.*;


public class FullBroadcaster {

  public static class WebSocket extends WebSocketController {

    private static Channel subscriptionChannel;

    public static void stream() {
      Logger.info("entered new fullbroadcaster");

      String subscriber = "TEST";

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
            handleChannelEvent(either._2.get());
          }
        }

        if (clientEvent != null) {
          Logger.info("received clientEvent " + clientEvent);
          // handle clientEvent
          handleClientEvent(subscriber, clientEvent);
        }
      }
    }

    private static void handleChannelEvent(Object channelEvent) {
      Logger.info("handleChannelEvent " + channelEvent);

      //TODO: Check if match status has become 2 --> end of match, remove subscription

      outbound.sendJson(channelEvent);
    }

    private static void handleClientEvent(String subscriber, WebSocketEvent clientEvent) {
      Logger.info("handleClientEvent " + clientEvent);

      for(String userMessage: TextFrame.match(clientEvent)) {
        QueryStringParser parser = new QueryStringParser(userMessage);

        if ("getTournaments".equals(parser.getCommand())) {
          getTournaments();
        } else if ("getTournament".equals(parser.getCommand())) {
          getTournament(parser);
        } else if ("getTournamentSessions".equals(parser.getCommand())) {
          getTournamentSessions(parser);
        } else if ("getSession".equals(parser.getCommand())) {
          getSession(parser);
        } else if ("getSessionMatches".equals(parser.getCommand())) {
          getSessionMatches(parser);
        } else if ("subscribeToMatch".equals(parser.getCommand())) {
          subscribeToMatch(subscriber, parser);
        } else if ("quit".equals(parser.getCommand())) {
          quit(subscriber);
        } else {
          // Unknown command, just echoing user input
          Logger.info("echo user input " + userMessage);
          outbound.sendJson(userMessage);
        }
      }
      
      // Case: The socket has been closed
      for(WebSocketClose closed: SocketClosed.match(clientEvent)) {
        quitAndUnsubscribe(subscriber);
      }
    }

    private static void quit(String subscriber) {
      outbound.sendJson("Quitting...");
      quitAndUnsubscribe(subscriber);
    }

    private static void subscribeToMatch(String subscriber, QueryStringParser parser) {
      if (parser.getParams().size() != 3) {
        outbound.sendJson("Invalid request");
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      final Long sessionID = Long.parseLong(parser.getParams().get(1));
      final Long matchID = Long.parseLong(parser.getParams().get(2));

      // First, send match record
      MatchID matchIDObj = new MatchID(matchID, sessionID);
      outbound.sendJson(Match.findById(matchIDObj));
      // Second, send handrecord
      outbound.sendJson(Handrecord.findById(sessionID));

      // Third, send play records... (Do we need to?)
      // Fourth, send result record



      subscriptionChannel = ChannelManager.getInstance().subscribe(subscriber, sessionID, matchID);
    }

    private static void getTournaments() {
      //TODO: Handle status (do not return status 2)
      outbound.sendJson(Tournament.findAll());
    }

    private static void getTournament(QueryStringParser parser) {
      if (parser.getParams().size() != 1) {
        outbound.sendJson("Invalid request");
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      outbound.sendJson(Tournament.findById(tournamentID));
    }

    private static void getTournamentSessions(QueryStringParser parser) {
      if (parser.getParams().size() != 1) {
        outbound.sendJson("Invalid request");
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      //TODO: Handle status (do not return status 2)
      outbound.sendJson(Session.find("byTournamentid", tournamentID).fetch());
    }

    private static void getSession(QueryStringParser parser) {
      if (parser.getParams().size() != 2) {
        outbound.sendJson("Invalid request");
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      final Long sessionID = Long.parseLong(parser.getParams().get(1));
      outbound.sendJson(Session.find("tournament.tournamentid = ? and sessionid = ?", tournamentID, sessionID).first());
    }

    private static void getSessionMatches(QueryStringParser parser) {
      if (parser.getParams().size() != 2) {
        outbound.sendJson("Invalid request");
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      final Long sessionID = Long.parseLong(parser.getParams().get(1));
      //TODO: Handle status (do not return status 2)
      outbound.sendJson(Match.find("bySessionid", sessionID).fetch());
    }
    
    private static void quitAndUnsubscribe(String subscriber) {
      if (subscriptionChannel != null) {
        ChannelManager.getInstance().unsubscribe(subscriptionChannel, subscriber);
      }
      disconnect();
    }
  }
}
