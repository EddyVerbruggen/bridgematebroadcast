package jobs.livefeed;

import models.*;
import models.livefeed.*;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Every("1s")
public class LivefeedTestDataJob extends Job {

  static final Long TOURNAMENTID = 54253876L;
  static final Long SESSIONID = 56066789L;
  
  static final Long LAST_PLAYID = 5906L;
  static final Long LAST_RESULTID = 142L;

  //"2007-10-11 03:17:30"
  static Date startOfTestData;

  static {
    Calendar cal = Calendar.getInstance();
    cal.set(2007, 9, 11, 3, 17, 30);
    startOfTestData = cal.getTime();
  }

  enum Status {
    START,
    RUNNING,
    FINISHED
  }

  //Start in START mode
  private Status status = Status.START;

  // Timestamp of start
  private long nrOfSecondsSimulating = 0;
  private long nrOfSecondsMatchIsFinished = 0;

  private Long lastInsertedPlayID = 0L;
  private Long lastInsertedResultID = 0L;
  private Long lastInsertedBoardNumber = 0L;


  @Override
  public void doJob() throws Exception {
    nrOfSecondsSimulating++;
    if (Status.START.equals(status)) {
      doStart();
    }

    if (Status.RUNNING.equals(status)) {
      doRunning();
    }

    if (Status.FINISHED.equals(status)) {
      doFinished();
    }
  }

  private void doRunning() {

    Logger.info("doRunning for " + nrOfSecondsSimulating + " seconds ");
    // obsolete
    Date systemDate = new Date(startOfTestData.getTime() + nrOfSecondsSimulating * 1000);
    Logger.info("System time is " + systemDate);

    //Insert play records created since previous insert
    List<LivefeedPlay> livefeedPlayList = LivefeedPlay.find("playid > ? and playid <= ?", lastInsertedPlayID, lastInsertedPlayID + 1).fetch();
    for (LivefeedPlay livefeedPlay : livefeedPlayList) {
      // First, check if the handrecord should be inserted
      if (livefeedPlay.boardnumber > lastInsertedBoardNumber) {
        Logger.info("Insert record into handrecord with boardnumber " + livefeedPlay.boardnumber);
        // Insert handrecord
        HandRecordID id = new HandRecordID();
        id.sessionID = SESSIONID;
        id.boardNumber = livefeedPlay.boardnumber;

        LivefeedHandrecord livefeedHandrecord = LivefeedHandrecord.findById(id);
        Handrecord handrecord = createHandrecord(livefeedHandrecord);
        handrecord.save();
        lastInsertedBoardNumber = livefeedPlay.boardnumber;
      }

      Logger.info("Insert record into play with playid " + livefeedPlay.playid);
      Play play = createPlay(livefeedPlay);
      play.save();
      lastInsertedPlayID = play.playid;
    }

    //Insert result records created since previous insert
    List<LivefeedResult> livefeedResultList = LivefeedResult.find("resultid > ? and resultid <= ?", lastInsertedResultID, lastInsertedResultID + 1).fetch();
    for (LivefeedResult livefeedResult : livefeedResultList) {
      Logger.info("Insert record into result with resultid " + livefeedResult.resultid);
      Result result = createResult(livefeedResult);
      result.save();
      lastInsertedResultID = result.resultid;
    }

    // Let's see if we are done inserting all play and result records
    if (LAST_PLAYID.compareTo(lastInsertedPlayID) < 1 && LAST_RESULTID.compareTo(lastInsertedResultID) < 1) {
      status = Status.FINISHED;
    }
  }

  private void doFinished() {
    Logger.info("doFinished");

    if (nrOfSecondsMatchIsFinished == 0) {
      List<Match> matches = Match.findAll();
      for (Match match : matches) {
        match.status = 2L;
        match.save();
      }
    } else if (nrOfSecondsMatchIsFinished > 10) {
      status = Status.START;
    }

    nrOfSecondsMatchIsFinished++;
  }

  private void doStart() {
    Logger.info("doStart");

    resetStatus();

    // Deletes all stuff from the actual tables
    deleteOldData();

    // Insert the tournament record on Application Startup using livefeedTournament
    LivefeedTournament livefeedTournament = LivefeedTournament.findById(TOURNAMENTID);
    Tournament tournament = createTournament(livefeedTournament);
    tournament.save();

    // Insert the session record(s) on Application Startup using livefeedSession
    List<LivefeedSession> livefeedSessionList = LivefeedSession.find("tournamentid = ?", TOURNAMENTID).fetch();
    for (LivefeedSession livefeedSession : livefeedSessionList) {
      Session session = createSession(livefeedSession);
      session.save();
    }

    // When the session starts, insert the matches from the match table
    List<LivefeedMatch> livefeedMatchList = LivefeedMatch.find("sessionid = ?", SESSIONID).fetch();
    for (LivefeedMatch livefeedMatch : livefeedMatchList) {
      Match match = createMatch(livefeedMatch);
      match.save();
    }

    status = Status.RUNNING;
  }

  private void resetStatus() {
    nrOfSecondsSimulating = 0;
    nrOfSecondsMatchIsFinished = 0;

    lastInsertedPlayID = 0L;
    lastInsertedResultID = 0L;
    lastInsertedBoardNumber = 0L;
  }

  private Result createResult(LivefeedResult livefeedResult) {
    Result result = new Result();
    result.resultid = livefeedResult.resultid;
    result.match = createMatch(livefeedResult.match);
    result.externalid = livefeedResult.externalid;
    result.roundnumber = livefeedResult.roundnumber;
    result.boardnumber = livefeedResult.boardnumber;
    result.pairns = livefeedResult.pairns;
    result.pairew = livefeedResult.pairew;
    result.declarer = livefeedResult.declarer;
    result.contract = livefeedResult.contract;
    result.result = livefeedResult.result;
    result.lead = livefeedResult.lead;
    result.timestamp = livefeedResult.timestamp;
    result.iserased = livefeedResult.iserased;
    result.isintermediate = livefeedResult.isintermediate;
    return result;
  }

  private Play createPlay(LivefeedPlay livefeedPlay) {
    Play play = new Play();
    play.playid = livefeedPlay.playid;
    play.match = createMatch(livefeedPlay.match);
    play.externalid = livefeedPlay.externalid;
    play.roundnumber = livefeedPlay.roundnumber;
    play.boardnumber = livefeedPlay.boardnumber;
    play.counter = livefeedPlay.counter;
    play.direction = livefeedPlay.direction;
    play.iscard = livefeedPlay.iscard;
    play.action = livefeedPlay.action;
    play.timestamp = livefeedPlay.timestamp;
    play.iserased = livefeedPlay.iserased;
    return play;
  }

  private Handrecord createHandrecord(LivefeedHandrecord livefeedHandrecord) {
    Handrecord handrecord = new Handrecord();
    handrecord.handRecordID = livefeedHandrecord.handRecordID;

    handrecord.north = livefeedHandrecord.north;
    handrecord.south = livefeedHandrecord.south;
    handrecord.east = livefeedHandrecord.east;
    handrecord.west = livefeedHandrecord.west;

    return handrecord;
  }

  private Match createMatch(LivefeedMatch livefeedMatch) {
    Match match = new Match();
    match.id = livefeedMatch.id;
    match.tablenumber = livefeedMatch.tablenumber;
    match.section = livefeedMatch.section;
    match.room = livefeedMatch.room;
    match.othertablematchid = livefeedMatch.othertablematchid;
    match.playernorth = livefeedMatch.playernorth;
    match.playereast = livefeedMatch.playereast;
    match.playersouth = livefeedMatch.playersouth;
    match.playerwest = livefeedMatch.playerwest;
    match.playercountrynorth = livefeedMatch.playercountrynorth;
    match.playercountryeast = livefeedMatch.playercountryeast;
    match.playercountrysouth = livefeedMatch.playercountrysouth;
    match.playercountrywest = livefeedMatch.playercountrywest;
    match.carryoverns = livefeedMatch.carryoverns;
    match.carryoverew = livefeedMatch.carryoverew;
    match.status = 1L;
    match.teamnamens = livefeedMatch.teamnamens;
    match.teamnameew = livefeedMatch.teamnameew;
    return match;
  }

  private Tournament createTournament(LivefeedTournament livefeedTournament) {
    Tournament tournament = new Tournament();

    tournament.tournamentid = livefeedTournament.tournamentid;
    tournament.name = livefeedTournament.name;
    tournament.location = livefeedTournament.location;
    tournament.startdate = livefeedTournament.startdate;
    tournament.enddate = livefeedTournament.enddate;
    tournament.timezone = livefeedTournament.timezone;

    return tournament;
  }

  private Session createSession(LivefeedSession livefeedSession) {
    Session session = new Session();
    session.sessionid = livefeedSession.sessionid;
    session.tournament = Tournament.findById(livefeedSession.tournament.tournamentid);
    session.name = livefeedSession.name;
    session.date = livefeedSession.date;
    session.status = 1L;
    return session;
  }

  private void deleteOldData() {
    Play.deleteAll();
    Result.deleteAll();
    Match.deleteAll();
    Handrecord.deleteAll();
    Session.deleteAll();
    Tournament.deleteAll();
  }
}
