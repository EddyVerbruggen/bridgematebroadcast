package jobs;

import controllers.Broadcaster;
import models.channel.Channel;
import models.channel.ChannelManager;
import models.Play;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart(async = true)
@Every("10s")
public class PublishJob extends Job {

  private int i;
  
  @Override
  public void doJob() throws Exception {
    Logger.info("Publishing....");

    for (Channel channel : ChannelManager.getInstance().getChannels()) {
      Logger.info("Publishing to channel " + channel);
      Play obj = Play.find("byMatchid", channel.channelID.getMatchID()).first();
      if (obj != null) {
        Logger.info("Publishing object " + obj);
        channel.publish(obj);
      } else {
        Logger.info("object is null");
      }
    }
    
    if (ChannelManager.getInstance().getChannels().isEmpty()) {
      Logger.info("No channels to publish");
    }
      // query db here and put the list of stuff in the publish method below
      // a few examples:

//      Tournament obj = Tournament.findById(100L);
//      Match obj = Match.find("byMatchid", 4500L).first();
//      List<Play> obj = Play.find("byMatchid", 102383L).fetch();
//      Play obj = Play.find("byMatchid", 102383L).first();

  }
}
