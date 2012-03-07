package jobs;

import controllers.Broadcaster;
import models.channel.Channel;
import models.channel.ChannelManager;
import models.Play;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

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

    Play obj = Play.find("byMatchid", matchID).first();
    if (obj != null) {
      Logger.info("Publishing object " + obj);
      channel.publish(obj);
    } else {
      Logger.info("object is null");
    }
  }
}
