package jobs.livefeed;

import models.*;
import models.livefeed.*;
import play.Logger;
import play.jobs.Every;
import play.jobs.Job;

import java.util.*;

@Every("1s")
public class LivefeedTestDataJob extends Job {

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

  private Long playIDToUpdateTo = 0L;
  private Long lastInsertedPlayID = 29235L;
  private Map<MatchID, Long> currentBoardNumberPerMatch = new HashMap<MatchID, Long>();
  private List<Long> insertedBoardNumbers = new ArrayList<Long>();

  @Override
  public void doJob() throws Exception {
    nrOfSecondsSimulating++;
    // Only run the livefeed testdata job in testmode
    if (play.Play.runingInTestMode()) {
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
  }

  private void doRunning() {

    Logger.info("doRunning for " + lastInsertedPlayID);

    //Insert play records created since previous insert
    playIDToUpdateTo = 0L;
    List<LivefeedPlay> livefeedPlayList = LivefeedPlay.find("playid > ? and playid <= ?", lastInsertedPlayID, lastInsertedPlayID + 1).fetch();
    for (LivefeedPlay livefeedPlay : livefeedPlayList) {
      // First, check if the handrecord should be inserted
      if (!insertedBoardNumbers.contains(livefeedPlay.boardnumber)) {
        Logger.info("Insert record into handrecord with boardnumber " + livefeedPlay.boardnumber);
        // Insert handrecord
        HandRecordID id = new HandRecordID();
        id.sessionID = livefeedPlay.match.id.sessionid;
        id.boardNumber = livefeedPlay.boardnumber;

        LivefeedHandrecord livefeedHandrecord = LivefeedHandrecord.findById(id);
        Handrecord handrecord = createHandrecord(livefeedHandrecord);
        handrecord.save();
        insertedBoardNumbers.add(livefeedPlay.boardnumber);
      }

      // Create the play record (save it after result record of previous board is inserted)
      Logger.info("Insert record into play with playid " + livefeedPlay.playid);
      PlayRecord play = createPlay(livefeedPlay);

      // Set the current board number of the match
      Long currentMatchBoardNumber = currentBoardNumberPerMatch.get(play.match.id);

      if (currentMatchBoardNumber == null) {
        // No current board number yet, set it
        currentBoardNumberPerMatch.put(play.match.id, play.boardnumber);  
        Logger.info("Setting current board number for match " + play.match.id + " to " + play.boardnumber);
      } else if (currentMatchBoardNumber != play.boardnumber) {
        // Board number has changed since previous play, so there should be a result record and we should update the current boardnumber
        List<LivefeedResult> livefeedResultList = LivefeedResult.find("match.id.sessionid = ? and match.id.matchid = ? and boardnumber = ?", livefeedPlay.match.id.sessionid, livefeedPlay.match.id.matchid, currentMatchBoardNumber).fetch();
        for (LivefeedResult livefeedResult : livefeedResultList) {
          Logger.info("Insert record into result with resultid " + livefeedResult.resultid);
          Result result = createResult(livefeedResult);
          result.save();
        }

        currentBoardNumberPerMatch.put(play.match.id, play.boardnumber);
        Logger.info("Setting current board number for match " + play.match.id + " to " + play.boardnumber);
      }

      // Save the play record
      play.save();
      playIDToUpdateTo = play.playid;
    }

    // Let's see if we are done inserting all play records, find all plays still to be inserted
    List<LivefeedPlay> playsToBeInserted = LivefeedPlay.find("playid > ?", lastInsertedPlayID).fetch();

    if (playIDToUpdateTo == 0L) {
      // find the next play id to insert
      if (!playsToBeInserted.isEmpty()) {
        playIDToUpdateTo = playsToBeInserted.get(0).playid - 1;
      }
    }

    lastInsertedPlayID = playIDToUpdateTo;

    if (playsToBeInserted.isEmpty()) {
      List<Match> matches = Match.findAll();
      for (Match match : matches) {
        // Last play is published, let's see if we still have a result to publish
        List<LivefeedResult> livefeedResultList = LivefeedResult.find("match.id.sessionid = ? and match.id.matchid = ? and boardnumber = ?", match.id.sessionid, match.id.matchid, currentBoardNumberPerMatch.get(match.id)).fetch();
        for (LivefeedResult livefeedResult : livefeedResultList) {
          Logger.info("Insert record into result with resultid " + livefeedResult.resultid);
          Result result = createResult(livefeedResult);
          result.save();
        }
      }

      status = Status.FINISHED;
    }
  }

  private void doFinished() {
    Logger.info("doFinished");

    if (nrOfSecondsMatchIsFinished == 0) {
      List<Match> matches = Match.findAll();
      for (Match match : matches) {
        Logger.info("Updating status of match " + match.id.matchid);
        match.status = 2L;
        match.save();
      }
    } else if (nrOfSecondsMatchIsFinished > 60) {
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
    List<LivefeedTournament> livefeedTournamentList = LivefeedTournament.findAll();
    for (LivefeedTournament livefeedTournament : livefeedTournamentList) {
      Tournament tournament = createTournament(livefeedTournament);
      tournament.save();
    }

    // Insert the session record(s) on Application Startup using livefeedSession
    List<LivefeedSession> livefeedSessionList = LivefeedSession.findAll();
    for (LivefeedSession livefeedSession : livefeedSessionList) {
      Session session = createSession(livefeedSession);
      session.save();
    }

    // When the session starts, insert the matches from the match table
    List<LivefeedMatch> livefeedMatchList = LivefeedMatch.findAll();
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
//    lastInsertedResultID = 0L;
    insertedBoardNumbers = new ArrayList<Long>();
    currentBoardNumberPerMatch = new HashMap<MatchID, Long>();
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

  private PlayRecord createPlay(LivefeedPlay livefeedPlay) {
    PlayRecord play = new PlayRecord();
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
    tournament.status = 1L;

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
    PlayRecord.deleteAll();
    Result.deleteAll();
    Match.deleteAll();
    Handrecord.deleteAll();
    Session.deleteAll();
    Tournament.deleteAll();
  }
}
