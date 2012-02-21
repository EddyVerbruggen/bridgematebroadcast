package jobs;

import models.channel.Channel;
import models.channel.ChannelManager;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart(async = true)
@Every("10s")
public class ChannelRegistrationsJob extends Job {

  @Override
  public void doJob() throws Exception {
    for (Channel channel : ChannelManager.getInstance().getChannels()) {
      Logger.info("Registered Channel " + channel);
    }
    if (ChannelManager.getInstance().getChannels().isEmpty()) {
      Logger.info("No Registered Channels yet... ");
    }
  }
}
