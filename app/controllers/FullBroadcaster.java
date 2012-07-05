package controllers;

import controllers.model.Error;
import controllers.model.Response;
import controllers.util.QueryStringParser;
import controllers.util.QueryStringParserException;
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

    private static Subscriber subscriber;

    public static void stream(String username, String password) {
      Logger.info("entered new fullbroadcaster");

      loginSubscriber(username, password);

      if (inbound.isOpen()) {
        if (subscriber == null) {
          Logger.info("Invalid username/password combination, disconnecting...");
          outbound.sendJson(ResponseBuilder.createErrorResponse("", new Error(Error.ERROR_INCORRECT_CREDENTIALS)));
          return;
        } else {
          Logger.info("Successfully logged on " + username);
          outbound.sendJson(ResponseBuilder.createDataResponse("", "", "Successfully logged on " + username));
        }
      }

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

    private static void loginSubscriber(String username, String password) {
      Logger.info("logging in subscriber [username = " + username + ", password = " + password + "]");

      List<Subscriber> subscribers = Subscriber.find("loginname = ? and password = ? and active = '1'", username, password).fetch();

      if (subscribers.size() == 1) {
        subscriber = subscribers.get(0);
      }
    }

    private static void handleChannelEvent(Object channelEvent) {
      Logger.info("handleChannelEvent " + channelEvent);
      outbound.sendJson(channelEvent);
    }

    private static void handleClientEvent(Subscriber subscriber, WebSocketEvent clientEvent) {
      Logger.info("handleClientEvent " + clientEvent);

      for(String userMessage: TextFrame.match(clientEvent)) {
        try {
          QueryStringParser parser = new QueryStringParser(userMessage);

          if ("getTournaments".equals(parser.getCommand())) {
            getTournaments(parser);
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
            quit(parser, subscriber);
          } else {
            // Unknown command, just echoing user input
            Logger.info("echo user input " + userMessage);
            outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_UNKNOWN_REQUEST)));
          }
        } catch (QueryStringParserException e) {
          outbound.sendJson(ResponseBuilder.createErrorResponse(userMessage, new Error(Error.ERROR_INVALID_REQUEST)));
        }
      }
      
      // Case: The socket has been closed
      for(WebSocketClose closed: SocketClosed.match(clientEvent)) {
        quitAndUnsubscribe(subscriber);
      }
    }

    private static void quit(QueryStringParser parser, Subscriber subscriber) {
      outbound.sendJson(ResponseBuilder.createDataResponse(parser, "", "Quitting..."));
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
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_INVALID_REQUEST)));
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      final Long sessionID = Long.parseLong(parser.getParams().get(1));
      final Long matchID = Long.parseLong(parser.getParams().get(2));

      MatchID matchIDObj = new MatchID(matchID, sessionID);
      Match match = Match.findById(matchIDObj);

      if (match.status == 2) {
        // Match is finished, no subscription possible
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_MATCH_FINISHED)));
        return;
      }

      // First, send match record
      outbound.sendJson(ResponseBuilder.createDataResponse(parser.getQueryString(), "Match", match));

      // Second, subscribe to the match channel
      subscriptionChannel = ChannelManager.getInstance().subscribe(subscriber, sessionID, matchID);

      List<Play> plays = Play.find("sessionid = ? and matchid = ? and playid <= ? order by playid ASC", sessionID, matchID, subscriptionChannel.lastPublishedPlayID).fetch();

      // Third, send handrecords for boardnumbers that already have play records
      for (Play play : plays) {
        if (!subscriptionChannel.publishedBoardNumbers.contains(play.boardnumber)) {
          HandRecordID id = new HandRecordID();
          id.sessionID = sessionID;
          id.boardNumber = play.boardnumber;
          outbound.sendJson(ResponseBuilder.createDataResponse(parser.getQueryString(), "Handrecord", Handrecord.findById(id)));
          subscriptionChannel.publishedBoardNumbers.add(play.boardnumber);
        }
      }
      
      // Fourth, send play records... (Send all play records that will never be published anymore)
      outbound.sendJson(ResponseBuilder.createDataResponse(parser.getQueryString(), "Play", plays));

      // Fifth, send result records... (Send all result records that will never be published anymore)
      List<Result> results = Result.find("sessionid = ? and matchid = ? and resultid <= ? order by resultid ASC", sessionID, matchID, subscriptionChannel.lastPublishedResultID).fetch();
      outbound.sendJson(ResponseBuilder.createDataResponse(parser.getQueryString(), "Result", results));

      // Send a history finished response
      outbound.sendJson(ResponseBuilder.createDataResponse(parser.getQueryString(), "System", "Sending history records finished"));
    }

    /**
     * Get all events (tournaments)
     */
    private static void getTournaments(QueryStringParser parser) {
      //TODO: Handle status (do not return status 2)
      List<Tournament> tournaments = Tournament.findAll();
      if (!tournaments.isEmpty()) {
        outbound.sendJson(ResponseBuilder.createDataResponse(parser, "Tournaments", tournaments));
      } else {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_NO_DATA_AVAILABLE)));
      }
    }

    /**
     * Get event (tournament) details
     * @param parser
     */
    private static void getTournament(QueryStringParser parser) {
      Response response = new Response();
      response.setRequest(parser.getQueryString());
      if (parser.getParams().size() != 1 || !parser.assertParamsNumeric()) {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_INVALID_REQUEST)));
        return;
      }

      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      Tournament tournament = Tournament.findById(tournamentID);
      if (tournament != null) {
        outbound.sendJson(ResponseBuilder.createDataResponse(parser, "Tournament", tournament));
      } else {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_NO_DATA_AVAILABLE)));
      }
    }

    /**
     * Get all sessions for the event (tournament)
     * @param parser contains the query string
     */
    private static void getTournamentSessions(QueryStringParser parser) {
      if (parser.getParams().size() != 1 || !parser.assertParamsNumeric()) {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_INVALID_REQUEST)));
        return;
      }

      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      List<Session> sessions = Session.find("tournament.tournamentid = ? and status <> ?", tournamentID, 2L).fetch();
      if (!sessions.isEmpty()) {
        outbound.sendJson(ResponseBuilder.createDataResponse(parser, "Sessions", sessions));
      } else {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_NO_DATA_AVAILABLE)));
      }
    }

    /**
     * Get session details
     * @param parser contains the query string
     */
    private static void getSession(QueryStringParser parser) {
      Response response = new Response();
      response.setRequest(parser.getQueryString());
      if (parser.getParams().size() != 2 || !parser.assertParamsNumeric()) {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_INVALID_REQUEST)));
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      final Long sessionID = Long.parseLong(parser.getParams().get(1));

      Session session = Session.find("tournament.tournamentid = ? and sessionid = ?", tournamentID, sessionID).first();
      if (session != null) {
        outbound.sendJson(ResponseBuilder.createDataResponse(parser, "Session", session));
      } else {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_NO_DATA_AVAILABLE)));
      }
    }

    /**
     * Get all matches for the session
     * @param parser contains the query string
     */
    private static void getSessionMatches(QueryStringParser parser) {
      if (parser.getParams().size() != 2 || !parser.assertParamsNumeric()) {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_INVALID_REQUEST)));
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      final Long sessionID = Long.parseLong(parser.getParams().get(1));
      List<Match> matches = Match.find("sessionid = ? and status <> ?", sessionID, 2L).fetch();
      if (!matches.isEmpty()) {
        outbound.sendJson(ResponseBuilder.createDataResponse(parser, "Matches", matches));
      } else {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_NO_DATA_AVAILABLE)));
      }
    }
    
    private static void quitAndUnsubscribe(Subscriber subscriber) {
      if (subscriptionChannel != null) {
        ChannelManager.getInstance().unsubscribe(subscriptionChannel, subscriber);
      }
      disconnect();
    }
  }
}
