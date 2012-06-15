package jobs.testdatacreator;

import models.Match;
import models.MatchID;
import models.Play;
import models.Result;
import play.jobs.Job;

import java.util.Calendar;

//@Every("20s")
public class TestDataCreatorJob extends Job {

  Match testMatch;  
  int i = 0;
  boolean finished;

  @Override
  public void doJob() throws Exception {

    if (!finished) {
      loadMatch();

      createPlayRecord();
      i++;

      if (i % 5 == 0) {
        createResultRecord();
      }

      if (i % 15 == 0) {
        //After third result record, finish match so broadcasting stops ...
        MatchID matchID = new MatchID(
          TestDataCreateSessionAndMatchOnStartupJob.MATCHID,
          TestDataCreateSessionAndMatchOnStartupJob.SESSIONID);
        testMatch = Match.findById(matchID);
        testMatch.status = 2L;
        testMatch.save();

        // This job is finished
        finished = true;
      }
    }
  }

  private void loadMatch() {
    if (testMatch == null) {
      MatchID matchID = new MatchID(
        TestDataCreateSessionAndMatchOnStartupJob.MATCHID,
        TestDataCreateSessionAndMatchOnStartupJob.SESSIONID);
      testMatch = Match.findById(matchID);
    }
  }

  private void createPlayRecord() {
    Play lastPlay = Play.find("order by playid desc").first();

    Play play = new Play();
    play.playid = lastPlay.playid + 1;
    play.match = testMatch;
    play.externalid = 1L;
    play.roundnumber = 1L;
    play.boardnumber = 1L;
    play.counter = 1L;
    play.direction = "N";
    play.iscard = 0L;
    play.action = "PASS";
    play.timestamp = Calendar.getInstance().getTime();
    play.iserased = 0L;

    play.save();
  }

  private void createResultRecord() {
    Result lastResult = Result.find("order by resultid desc").first();

    //Create result record
    Result result = new Result();
    result.resultid = lastResult.resultid + 1;
    result.match = testMatch;
    result.externalid = 1L;
    result.roundnumber = 1L;
    result.boardnumber = 1L;
    result.pairns = 7L;
    result.pairew = 11L;
    result.declarer = "N";
    result.contract = "1 H";
    result.result = "-1";
    result.lead = "HA";
    result.timestamp = Calendar.getInstance().getTime();
    result.iserased = 0L;
    result.isintermediate = 1L;
    result.save();
  }
}
