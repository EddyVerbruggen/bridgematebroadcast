package jobs;

import controllers.Broadcaster;
import models.Match;
import models.MatchID;
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
    // TODO: Order by playid ASC
    List<Play> obj = Play.find("sessionid = ? and matchid = ? and playid > ?", sessionID, matchID, channel.lastPublishedPlayID).fetch();
    if (obj != null && obj.size() > 0) {
      Logger.info("Publishing object " + obj);
      channel.publish(obj);
      Play lastPlayRecord = obj.get(obj.size());
      channel.lastPublishedPlayID = lastPlayRecord.playid;
    } else {
      Logger.info("object is null");
    }

    // 2. Check if match is finished yet
    MatchID matchIDObj = new MatchID(matchID, sessionID); 
    Match match = Match.findById(matchIDObj);
    if (match.isFinished()) {
      channel.publish(match);
      // Publish match object, since match is finished
      // TODO: Kill all subscriptions on match here... (and see if that works or if that has to be done in the FullBroadcaster)
      //ChannelManager.getInstance().
    }
  }
}
