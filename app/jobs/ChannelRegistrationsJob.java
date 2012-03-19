package jobs;

import models.channel.Channel;
import models.channel.ChannelID;
import models.channel.ChannelManager;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import java.util.Set;

@OnApplicationStart(async = true)
@Every("10s")
public class ChannelRegistrationsJob extends Job {

  @Override
  public void doJob() throws Exception {
    Set<ChannelID> keySet = ChannelManager.getInstance().getChannelMap().keySet();
    for (ChannelID key : keySet) {
      Logger.info("Registered Channel " + ChannelManager.getInstance().getChannelMap().get(key));
    }

    if (ChannelManager.getInstance().getChannelMap().isEmpty()) {
      Logger.info("No Registered Channels yet... ");
    }
  }
}
