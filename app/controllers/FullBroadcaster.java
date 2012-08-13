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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static play.mvc.Http.WebSocketEvent.*;


public class FullBroadcaster {

  public static class WebSocket extends WebSocketController {

    private static HashMap<String, Channel> subscriptionChannels = new HashMap<String, Channel>();

    private static HashMap<String, Subscriber> subscribers = new HashMap<String, Subscriber>();

    public static void stream(String username, String password) {
      Logger.info("entered new fullbroadcaster for outbound: " + WebSocket.outbound.toString());

      String websocketIdentifier = WebSocket.outbound.toString();

      loginSubscriber(username, password, websocketIdentifier);

      Subscriber subscriber = subscribers.get(websocketIdentifier);
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

        Channel subscriptionChannel = subscriptionChannels.get(websocketIdentifier);
        
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
              subscriptionChannel.nextEvent(subscriber)));

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
          handleClientEvent(subscriber, clientEvent, subscriptionChannel, websocketIdentifier);
        }
      }
    }

    private static void loginSubscriber(String username, String password, String websocketIdentifier) {
      Logger.info("logging in subscriber [username = " + username + ", password = " + password + "]");

      List<Subscriber> registeredSubscribers = Subscriber.find("loginname = ? and password = ? and active = '1'", username, password).fetch();

      if (registeredSubscribers.size() == 1) {
        subscribers.put(websocketIdentifier, registeredSubscribers.get(0));
      }
    }

    private static void handleChannelEvent(Object channelEvent) {
      Logger.info("handleChannelEvent " + channelEvent);
      outbound.sendJson(channelEvent);
    }

    private static void handleClientEvent(Subscriber subscriber, WebSocketEvent clientEvent, Channel subscriptionChannel, String websocketIdentifier) {
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
          } else if ("isSubscriptionAlive".equals(parser.getCommand())) {
            isSubscriptionAlive(parser, subscriptionChannel);
          } else if ("unsubscribe".equals(parser.getCommand())) {
            unsubscribe(parser, subscriptionChannel, subscriber);
          } else if ("subscribeToMatch".equals(parser.getCommand())) {
            subscribeToMatch(subscriber, parser, websocketIdentifier);
          } else if ("quit".equals(parser.getCommand())) {
            quit(parser, subscriber, subscriptionChannel);
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
        quitAndUnsubscribe(subscriber, subscriptionChannel);
      }
    }

    private static void unsubscribe(QueryStringParser parser, Channel subscriptionChannel, Subscriber subscriber) {
      Long matchID = null;
      if (subscriptionChannel != null) {
        matchID = subscriptionChannel.channelID.getMatchID();
        ChannelManager.getInstance().unsubscribe(subscriptionChannel, subscriber);
        subscriptionChannel = null;
      }
      outbound.sendJson(ResponseBuilder.createDataResponse(parser, "System", "Unsubscribed from match " + (matchID != null ? matchID : "")));
    }

    private static void isSubscriptionAlive(QueryStringParser parser, Channel subscriptionChannel) {
      if (subscriptionChannel != null) {
        MatchID matchID = new MatchID(subscriptionChannel.channelID.getMatchID(), subscriptionChannel.channelID.getSessionID());
        Match match = Match.findById(matchID);
        outbound.sendJson(ResponseBuilder.createDataResponse(parser, "Match", match));
      } else {
        outbound.sendJson(ResponseBuilder.createDataResponse(parser, "System", "No match subscribed"));
      }
    }
    private static void quit(QueryStringParser parser, Subscriber subscriber, Channel subscriptionChannel) {
      outbound.sendJson(ResponseBuilder.createDataResponse(parser, "", "Quitting..."));
      quitAndUnsubscribe(subscriber, subscriptionChannel);
    }

    /**
     * Subscribe subscriber to a match.
     * TODO FUTURE: We can check here if the subscriber is allowed to subscribe and register the subscription for charging purposes.
     * @param subscriber
     * @param parser
     */
    private static void subscribeToMatch(Subscriber subscriber, QueryStringParser parser, String websocketIdentifier) {
      if (parser.getParams().size() != 3) {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_INVALID_REQUEST)));
        return;
      }
      final Long tournamentID = Long.parseLong(parser.getParams().get(0));
      final Long sessionID = Long.parseLong(parser.getParams().get(1));
      final Long matchID = Long.parseLong(parser.getParams().get(2));

      MatchID matchIDObj = new MatchID(matchID, sessionID);
      Match match = Match.findById(matchIDObj);

      if (match == null) {
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_NO_DATA_AVAILABLE)));
        return;
      }

      if (match.status == 2) {
        // Match is finished, no subscription possible
        outbound.sendJson(ResponseBuilder.createErrorResponse(parser, new Error(Error.ERROR_MATCH_FINISHED)));
        return;
      }

      // First, send match record
      outbound.sendJson(ResponseBuilder.createDataResponse(parser.getQueryString(), "Match", match));

      // Second, subscribe to the match channel
      Channel subscriptionChannel = ChannelManager.getInstance().subscribe(subscriber, sessionID, matchID);
      subscriptionChannels.put(websocketIdentifier, subscriptionChannel);

      // Find all plays for previous boards
      List<PlayRecord> plays =
            PlayRecord.find("sessionid = ? and matchid = ? and boardnumber < ? ORDER by boardnumber ASC, iscard ASC, externalid ASC",
                    sessionID,
                    matchID,
                    subscriptionChannel.lastPublishedPlayBoardnumber).fetch();

      // Find all plays for current board
      List<PlayRecord> currentPlays = PlayRecord.find("sessionid = ? and matchid = ? and boardnumber = ? and ((iscard = ? and externalid <= ?) or (iscard < ?)) order by iscard ASC, externalid ASC",
              sessionID,
              matchID,
              subscriptionChannel.lastPublishedPlayBoardnumber,
              subscriptionChannel.lastPublishedPlayIscard,
              subscriptionChannel.lastPublishedPlayExternalID,
              subscriptionChannel.lastPublishedPlayIscard
      ).fetch();

      // Combine both lists
      plays.addAll(currentPlays);

      // Third, send handrecords for boardnumbers that already have play records
      List<Long> publishedBoardNumbers = new ArrayList<Long>();
      for (PlayRecord play : plays) {
        if (!publishedBoardNumbers.contains(play.boardnumber)) {
          HandRecordID id = new HandRecordID();
          id.sessionID = sessionID;
          id.boardNumber = play.boardnumber;
          outbound.sendJson(ResponseBuilder.createDataResponse(parser.getQueryString(), "Handrecord", Handrecord.findById(id)));
          publishedBoardNumbers.add(play.boardnumber);
          subscriptionChannel.publishedBoardNumbers.add(play.boardnumber);
        }
      }

      // Fourth, send play records... (Send all play records that will never be published anymore)
      outbound.sendJson(ResponseBuilder.createDataResponse(parser.getQueryString(), "Play", plays));

      // Fifth, send result records... (Send all result records that will never be published anymore)
      List<Result> results = Result.find("sessionid = ? and matchid = ? and externalid <= ? order by externalid ASC", sessionID, matchID, subscriptionChannel.lastPublishedResultExternalID).fetch();
      outbound.sendJson(ResponseBuilder.createDataResponse(parser.getQueryString(), "Result", results));

      // Send a history finished response
      outbound.sendJson(ResponseBuilder.createDataResponse(parser.getQueryString(), "System", "Sending history records finished"));
    }

    /**
     * Get all events (tournaments)
     */
    private static void getTournaments(QueryStringParser parser) {
      List<Tournament> tournaments = Tournament.find("status <> ?", 2L).fetch();
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
    
    private static void quitAndUnsubscribe(Subscriber subscriber, Channel subscriptionChannel) {
      if (subscriptionChannel != null) {
        ChannelManager.getInstance().unsubscribe(subscriptionChannel, subscriber);
      }
      disconnect();
    }
  }
}
