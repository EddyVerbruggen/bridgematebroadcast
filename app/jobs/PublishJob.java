package jobs;

import models.Match;
import models.MatchID;
import models.Play;
import models.Result;
import models.channel.Channel;
import models.channel.ChannelID;
import models.channel.ChannelManager;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

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
@Every("10s")
public class PublishJob extends Job {

  private int i;
  
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
          // Kill all subscriptions on match here...
          ChannelManager.getInstance().unsubscribeAll(channel);
        }
      } else {
        // Session subscription
        // TODO: Implement
      }
    }
  }

  private boolean handleMatchSubscription(Channel channel, Long sessionID, Long matchID) {
    boolean isMatchFinished = false;

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
      // Publish match object, since match is finished
      channel.publish(match);
      isMatchFinished = true;
    } else {
      Logger.info("Match not finished yet");
    }

    return isMatchFinished;
  }
}
