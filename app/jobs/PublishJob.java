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

      // query db here and put the list of stuff in the publish method below
      // a few examples:

//      Tournament obj = Tournament.findById(100L);

//      Match obj = Match.find("byMatchid", 4500L).first();

//      List<Play> obj = Play.find("byMatchid", 102383L).fetch();

      Play obj = Play.find("byMatchid", 102383L).first();

      // TODO base query on matchid
      Channel channel = ChannelManager.getInstance().findChannel(101L);

      if (channel != null) {
        Logger.info("Publishing to channel " + channel);
        channel.publish(obj);
      } else {
        Logger.info("No one is published");
      }

//      Broadcaster.liveStream.publish(obj);
//      Logger.info("No cnx, no query.");
  }
}
