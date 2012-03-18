package jobs;

import controllers.Broadcaster;
import models.Match;
import models.MatchID;
import models.Result;
import models.channel.Channel;
import models.channel.ChannelManager;
import models.Play;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import java.util.List;

/*
      // query db here and put the list of stuff in the publish method below
      // a few examples:

//      Tournament obj = Tournament.findById(100L);
//      Match obj = Match.find("byMatchid", 4500L).first();
//      List<Play> obj = Play.find("byMatchid", 102383L).fetch();
//      Play obj = Play.find("byMatchid", 102383L).first();
 */

@OnApplicationStart(async = true)
@Every("10s")
public class PublishJob extends Job {

  private int i;
  
  @Override
  public void doJob() throws Exception {
    Logger.info("Publishing....");

    if (ChannelManager.getInstance().getChannels().isEmpty()) {
      Logger.info("No channels to publish");
    }

    // Handle subscriptions
    for (Channel channel : ChannelManager.getInstance().getChannels()) {

      Logger.info("Publishing to channel " + channel);

      if (channel.channelID.getMatchID() != null) {
        // Match subscription
        Long sessionID = channel.channelID.getSessionID();
        Long matchID = channel.channelID.getMatchID();
        handleMatchSubscription(channel, sessionID, matchID);
      } else {
        // Session subscription
        // TODO: Implement
      }
    }
  }

  private void handleMatchSubscription(Channel channel, Long sessionID, Long matchID) {
    // 1. Check on and send new Play records
    List<Play> plays = Play.find("sessionid = ? and matchid = ? and playid > ? order by playid ASC", sessionID, matchID, channel.lastPublishedPlayID).fetch();
    if (plays != null && plays.size() > 0) {
      channel.publish(plays);
      Play lastPlayRecord = plays.get(plays.size() - 1);
      channel.lastPublishedPlayID = lastPlayRecord.playid;
    } else {
      Logger.info("no play records to publish");
    }

    // 2. Check on and send new Result records
    List<Result> results = Result.find("sessionid = ? and matchid = ? and resultid > ? order by resultid ASC", sessionID, matchID, channel.lastPublishedResultID).fetch();
    if (results != null && results.size() > 0) {
      channel.publish(results);
      Result lastResultRecord = results.get(results.size() - 1);
      channel.lastPublishedResultID = lastResultRecord.resultid;
    } else {
      Logger.info("no result records to publish");
    }
    
    // 3. Check if match is finished yet
    MatchID matchIDObj = new MatchID(matchID, sessionID); 
    Match match = Match.findById(matchIDObj);
    if (match.isFinished()) {
      channel.publish(match);
      // Publish match object, since match is finished
      // TODO: Kill all subscriptions on match here... (and see if that works or if that has to be done in the FullBroadcaster)
      ChannelManager.getInstance().unsubscribeAll(channel);
    } else {
      Logger.info("Match not finished yet");
    }
  }
}
