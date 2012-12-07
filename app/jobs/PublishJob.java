package jobs;

import controllers.ResponseBuilder;
import models.*;
import models.channel.Channel;
import models.channel.ChannelID;
import models.channel.ChannelManager;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/*
      // query db here and put the list of stuff in the publish method below
      // a few examples:

//      Tournament obj = Tournament.findById(100L);
//      Match obj = Match.find("byMatchid", 4500L).first();
//      List<Play> obj = Play.find("byMatchid", 102383L).fetch();
//      Play obj = Play.find("byMatchid", 102383L).first();
 */

@OnApplicationStart(async = true)
@Every("3s")
public class PublishJob extends Job {

  @Override
  public void doJob() throws Exception {
    Logger.info("Publishing....");

    if (ChannelManager.getInstance().getChannelMap().isEmpty()) {
      Logger.info("No channels to publish to");
    }

    // Handle subscriptions
    Set<ChannelID> keys = ChannelManager.getInstance().getChannelMap().keySet();

    for (ChannelID key : keys) {
      Channel channel = ChannelManager.getInstance().getChannelMap().get(key);
      Logger.info("Publishing to channel " + channel);

      if (channel.channelID.getMatchID() != null) {
        // Match subscription
        Long sessionID = channel.channelID.getSessionID();
        Long matchID = channel.channelID.getMatchID();
        boolean isMatchFinished = handleMatchSubscription(channel, sessionID, matchID);
        if (isMatchFinished) {
          // TODO: Apparently this does not work (some timing issues with the match record being published)
          // Kill all subscriptions on match here...
          // ChannelManager.getInstance().unsubscribeAll(channel);
        }
      } else {
        // Session subscription
        // TODO: Implement
      }
    }
  }

  private boolean handleMatchSubscription(Channel channel, Long sessionID, Long matchID) {
    boolean isMatchFinished = false;

    // 1. Check if new handrecords need to be sent (loop through all play records and find out if Handrecord has been sent already)
    //List<PlayRecord> plays = PlayRecord.find("sessionid = ? and matchid = ? and playid > ? order by playid ASC", sessionID, matchID, channel.lastPublishedPlayExternalID).fetch();
    List<PlayRecord> playsCurrentBoard = PlayRecord.find("sessionid = ? and matchid = ? and boardnumber = ? and ((iscard = ? and externalid > ?) or (iscard > ?)) order by iscard ASC, externalid ASC",
            sessionID,
            matchID,
            channel.lastPublishedPlayBoardnumber,
            channel.lastPublishedPlayIscard,
            channel.lastPublishedPlayExternalID,
            channel.lastPublishedPlayIscard
      ).fetch();

    if (playsCurrentBoard.size() > 0) {
      channel.publish(ResponseBuilder.createDataResponse("Data pushed by Bridgemate Broadcast server", "Play", playsCurrentBoard));
      PlayRecord lastPlayRecord = playsCurrentBoard.get(playsCurrentBoard.size() - 1);
      channel.lastPublishedPlayBoardnumber = lastPlayRecord.boardnumber;
      channel.lastPublishedPlayIscard = lastPlayRecord.iscard;
      channel.lastPublishedPlayExternalID = lastPlayRecord.externalid;
    } else {
      Logger.info("no play records to publish");
    }


    // Check for play records of not yet published boards
    List<PlayRecord> plays = new ArrayList<PlayRecord>();
    if (channel.publishedBoardNumbers != null && !channel.publishedBoardNumbers.isEmpty()) {
      List<PlayRecord> playsNextBoards = PlayRecord.find("sessionid = ? and matchid = ? and boardnumber not in :publishedBoardnumbers order by boardnumber ASC, iscard ASC, externalid ASC",
          sessionID,
          matchID
      )
          .bind("publishedBoardnumbers", channel.publishedBoardNumbers)
          .fetch();

      plays.addAll(playsNextBoards);
    } else {
      List<PlayRecord> playsNextBoards = PlayRecord.find("sessionid = ? and matchid = ? order by boardnumber ASC, iscard ASC, externalid ASC", sessionID, matchID).fetch();
      plays.addAll(playsNextBoards);
    }


    if (plays.size() > 0) {
      for (PlayRecord play : plays) {
        if (!channel.publishedBoardNumbers.contains(play.boardnumber)) {
          HandRecordID id = new HandRecordID();
          id.boardNumber = play.boardnumber;
          id.sessionID = sessionID;
          Handrecord handrecord = Handrecord.findById(id);
          channel.publish(ResponseBuilder.createDataResponse("Data pushed by Bridgemante Broadcast server", "Handrecord", handrecord));
          channel.publishedBoardNumbers.add(play.boardnumber);
        }
      }
    }

    // 2. Check on and send new Result records
    List<Result> results = Result.find("sessionid = ? and matchid = ? and externalid > ? order by externalid ASC", sessionID, matchID, channel.lastPublishedResultExternalID).fetch();
    if (results != null && results.size() > 0) {
      channel.publish(ResponseBuilder.createDataResponse("Data pushed by Bridgemate Broadcast server", "Result", results));
      Result lastResultRecord = results.get(results.size() - 1);
      channel.lastPublishedResultExternalID = lastResultRecord.externalid;
    } else {
      Logger.info("no result records to publish");
    }

    // 3. Check on and send new Play records
    // List<Play> plays = Play.find("sessionid = ? and matchid = ? and playid > ? order by playid ASC", sessionID, matchID, channel.lastPublishedPlayExternalID).fetch();
    if (plays.size() > 0) {
      channel.publish(ResponseBuilder.createDataResponse("Data pushed by Bridgemate Broadcast server", "Play", plays));
      PlayRecord lastPlayRecord = plays.get(plays.size() - 1);
      channel.lastPublishedPlayBoardnumber = lastPlayRecord.boardnumber;
      channel.lastPublishedPlayIscard = lastPlayRecord.iscard;
      channel.lastPublishedPlayExternalID = lastPlayRecord.externalid;
    } else {
      Logger.info("no play records to publish");
    }

    // 4. Check if match is finished yet
    MatchID matchIDObj = new MatchID(matchID, sessionID); 
    Match match = Match.findById(matchIDObj);
    if (match == null) {
      // Match could not be found anymore, which means it is deleted so we should kill the channel and its subscriptionss
      channel.publish(ResponseBuilder.createDataResponse("Data pushed by Bridgemate Broadcast server", "Match", match));
      isMatchFinished = true;
      Logger.info("Match could not be found");
    } else if (match.isFinished()) {
      // Publish match object, since match is finished
      Logger.info("Publishing the Match record: " + match);
      channel.publish(ResponseBuilder.createDataResponse("Data pushed by Bridgemate Broadcast server", "Match", match));
      isMatchFinished = true;
    } else {
      Logger.info("Match not finished yet");
    }

    return isMatchFinished;
  }
}
