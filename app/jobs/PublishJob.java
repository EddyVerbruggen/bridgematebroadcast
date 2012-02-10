package jobs;

import controllers.Broadcaster;
import models.Match;
import models.Play;
import models.Tournament;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import java.util.List;

@OnApplicationStart(async = true)
@Every("10s")
public class PublishJob extends Job {

  private int i;
  
  @Override
  public void doJob() throws Exception {
    if (Broadcaster.WebSocket.hasConnection) {
      Logger.info("There is a connection, so looking for stuff to publish...");

      // query db here and put the list of stuff in the publish method below
      // a few examples:

//      Tournament obj = Tournament.findById(100L);

//      Match obj = Match.find("byMatchid", 4500L).first();

//      List<Play> obj = Play.find("byMatchid", 102383L).fetch();

      System.err.println("i: " + i++);

      Play obj = Play.find("byMatchid", 102383L).first();

      Broadcaster.liveStream.publish(obj);
    } else {
//      Logger.info("No cnx, no query.");
    }
  }
}
