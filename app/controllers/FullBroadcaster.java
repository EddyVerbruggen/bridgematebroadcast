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

import java.util.List;

import static play.mvc.Http.WebSocketEvent.*;


public class FullBroadcaster {

  public static class WebSocket extends WebSocketController {

    private static Channel subscriptionChannel;

    public static void stream() {
      Logger.info("entered new fullbroadcaster");

      Subscriber subscriber = loginSubscriber();

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

    private static Subscriber loginSubscriber() {
      // TODO: Create login for subscriber
//      Subscriber subscriber = Subscriber.find("loginname = ? and active = '1'", "Test").first();

      Subscriber subscriber = new Subscriber();
      subscriber.id = 1L;
      subscriber.loginName = "test";
      subscriber.name = "test";
      return subscriber;
    }

    private static void handleChannelEvent(Object channelEvent) {
      Logger.info("handleChannelEvent " + channelEvent);

      //TODO: if channelEvent instanceof match => end of match, check if remove subscription in PublishJob works here
      outbound.sendJson(channelEvent);
    }

    private static void handleClientEvent(Subscriber subscriber, WebSocketEvent clientEvent) {
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

    private static void quit(Subscriber subscriber) {
      outbound.sendJson("Quitting...");
      quitAndUnsubscribe(subscriber);
    }

    /**
     * Subscribe subscriber to a match.
     * TODO FUTURE: We can check here if the subscriber is allowed to subscribe and register the subscription for charging purposes.
     * @param subscriber
     * @param parser
     */
    private static void subscribeToMatch(Subscriber subscriber, QueryStringParser parser) {
      if (parser.getParams().size() != 3) {
        outbound.sendJson("Invalid request");
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      final Long sessionID = Long.parseLong(parser.getParams().get(1));
      final Long matchID = Long.parseLong(parser.getParams().get(2));

      MatchID matchIDObj = new MatchID(matchID, sessionID);
      Match match = Match.findById(matchIDObj);

      if (match.status == 2) {
        // Match is finished, no subscription possible
        outbound.sendJson("Match is finished");
        return;
      }

      // First, send match record
      outbound.sendJson(match);

      // Second, send handrecord
      outbound.sendJson(Handrecord.findById(sessionID));

      // Third, subscribe to the match channel
      subscriptionChannel = ChannelManager.getInstance().subscribe(subscriber, sessionID, matchID);

      // Fourth, send play records... (Send all play records that will never be published anymore)
      List<Play> plays = Play.find("sessionid = ? and matchid = ? and playid <= ? order by playid ASC", sessionID, matchID, subscriptionChannel.lastPublishedPlayID).fetch();
      outbound.sendJson(plays);

      // Fifth, send result records... (Send all result records that will never be published anymore)
      List<Result> results = Result.find("sessionid = ? and matchid = ? and resultid <= ? order by resultid ASC", sessionID, matchID, subscriptionChannel.lastPublishedResultID).fetch();
      outbound.sendJson(results);

    }

    /**
     * Get all events (tournaments)
     */
    private static void getTournaments() {
      //TODO: Handle status (do not return status 2)
      outbound.sendJson(Tournament.findAll());
    }

    /**
     * Get event (tournament) details
     * @param parser
     */
    private static void getTournament(QueryStringParser parser) {
      if (parser.getParams().size() != 1) {
        outbound.sendJson("Invalid request");
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      outbound.sendJson(Tournament.findById(tournamentID));
    }

    /**
     * Get all sessions for the event (tournament)
     * @param parser
     */
    private static void getTournamentSessions(QueryStringParser parser) {
      if (parser.getParams().size() != 1) {
        outbound.sendJson("Invalid request");
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      outbound.sendJson(Session.find("tournament.tournamentid = ? and status <> ?", tournamentID, 2).fetch());
    }

    /**
     * Get session details
     * @param parser
     */
    private static void getSession(QueryStringParser parser) {
      if (parser.getParams().size() != 2) {
        outbound.sendJson("Invalid request");
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      final Long sessionID = Long.parseLong(parser.getParams().get(1));
      outbound.sendJson(Session.find("tournament.tournamentid = ? and sessionid = ?", tournamentID, sessionID).first());
    }

    /**
     * Get all matches for the session
     * @param parser
     */
    private static void getSessionMatches(QueryStringParser parser) {
      if (parser.getParams().size() != 2) {
        outbound.sendJson("Invalid request");
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      final Long sessionID = Long.parseLong(parser.getParams().get(1));
      outbound.sendJson(Match.find("sessionid = ? and status <> ?", sessionID, 2).fetch());
    }
    
    private static void quitAndUnsubscribe(Subscriber subscriber) {
      if (subscriptionChannel != null) {
        ChannelManager.getInstance().unsubscribe(subscriptionChannel, subscriber);
      }
      disconnect();
    }
  }
}
