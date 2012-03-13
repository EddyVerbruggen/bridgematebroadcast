package jobs;

import models.*;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

import java.util.Calendar;
import java.util.Date;

@OnApplicationStart
public class TestDataCreateSessionAndMatchOnStartupJob extends Job {

  static final Long TOURNAMENTID = 1L;
  static final Long SESSIONID = 1L;
  static final Long MATCHID = 1L;

  @Override
  public void doJob() throws Exception {

    deleteOldData();

    // Create Event
    Tournament tournament = new Tournament();
    tournament.tournamentid = TOURNAMENTID;
    tournament.name = "Test data tournament";
    tournament.location = "Test";
    tournament.startdate = Calendar.getInstance().getTime();
    tournament.enddate = new Date(Calendar.getInstance().getTime().getTime() + 1000*60*60*24*7); //One week from now
    tournament.save();
    
    // Create Session
    Session session = new Session();
    session.sessionid = SESSIONID;
    session.tournament = tournament;
    session.name = "Test data session";
    session.date = Calendar.getInstance().getTime();
    session.save();
    
    // Create match
    Match match = new Match();
    match.id = new MatchID(MATCHID, SESSIONID);
    match.tablenumber = 1L;
    match.section = "A";
    match.room = "Open";
    match.othertablematchid = 2L;
    match.playernorth = "Test North";
    match.playereast = "Test East";
    match.playersouth = "Test South";
    match.playerwest = "Test West";
    match.playercountryeast = "NL";
    match.playercountrywest = "NL";
    match.playercountrynorth = "NL";
    match.playercountrysouth = "NL";
    match.carryoverew = 0.00;
    match.carryoverns = 0.00;
    match.status = 0L;
    match.teamnameew = "Test Data Team EW";
    match.teamnamens = "Test Data Team NS";
    match.save();
  }

  private void deleteOldData() {
    Play.delete("sessionid = ? and matchid = ? ", SESSIONID, MATCHID);
    Result.delete("sessionid = ? and matchid = ? ", SESSIONID, MATCHID);
    Match.delete("sessionid = ? and matchid = ? ", SESSIONID, MATCHID);
    Session.delete("sessionid = ?", SESSIONID);
    Tournament.delete("tournamentid = ?", TOURNAMENTID);
  }
}
