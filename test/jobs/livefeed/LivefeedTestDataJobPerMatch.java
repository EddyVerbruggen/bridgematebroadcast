package jobs.livefeed;

import models.*;
import models.livefeed.LivefeedHandrecord;
import models.livefeed.LivefeedPlay;
import models.livefeed.LivefeedResult;
import play.Logger;
import play.jobs.Every;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Every("5s")
public class LivefeedTestDataJobPerMatch extends LivefeedTestDataJob {

  private static final Long BOARDNR_FINISHED = 100L;

  private Map<MatchID, LivefeedTestDataMatchStatus> matchStatuses = new HashMap<MatchID, LivefeedTestDataMatchStatus>();

  @Override
  protected void doRunning() {
    Logger.info("doRunning");

    for (MatchID key : matchStatuses.keySet()) {
      insertNextRecordForMatch(key);
    }

    // Check if all matches are finished...
    boolean finished = true;
    for (MatchID key : matchStatuses.keySet()) {
      if (!BOARDNR_FINISHED.equals(matchStatuses.get(key).lastPublishedPlayBoardnumber)) {
        finished = false;
      }
    }

    if (finished) {
      status = Status.FINISHED;
    }
  }

  private void insertNextRecordForMatch(MatchID key) {
    LivefeedTestDataMatchStatus status = matchStatuses.get(key);

    Logger.info("Last published[board=" + status.lastPublishedPlayBoardnumber + ",iscard="+status.lastPublishedPlayIscard + ",externalid=" + status.lastPublishedPlayExternalID + "]");
    List<LivefeedPlay> nextPlays = LivefeedPlay.find(
        "match.id.sessionid = ? and match.id.matchid = ? and ((boardnumber = ? and (iscard >= ? and externalid > ?)) or boardnumber > ?) order by sessionid ASC, matchid ASC, boardnumber ASC, iscard ASC, externalid ASC",
        key.sessionid,
        key.matchid,
        status.lastPublishedPlayBoardnumber,
        status.lastPublishedPlayIscard,
        status.lastPublishedPlayExternalID,
        status.lastPublishedPlayBoardnumber
      ).fetch(2);

    if (nextPlays.size() > 0) {
      LivefeedPlay nextPlay = null;

      if (nextPlays.size() > 1) {
        nextPlay = nextPlays.get(1);
      }
      LivefeedPlay currentPlay = nextPlays.get(0);

      // First: check if we need to insert the handrecord
      insertHandrecordIfNecessary(status, currentPlay);

      // Second: result record should be inserted in the Play table
      Logger.info("Insert record into play with playid [" + currentPlay.playid + "] for [" +
          "sessionid ="+ currentPlay.match.id.sessionid +", " +
          "matchid ="+currentPlay.match.id.matchid + ", " +
          "boardnumber="+currentPlay.boardnumber +", " +
          "iscard="+currentPlay.iscard +", " +
          "externalid="+currentPlay.externalid +"]");

      PlayRecord play = createPlay(currentPlay);

      status.lastPublishedPlayExternalID = currentPlay.externalid;
      if (nextPlay!= null && nextPlay.iscard > currentPlay.iscard) {
        status.lastPublishedPlayExternalID = 0L;
        status.lastPublishedPlayIscard = nextPlay.iscard;
      }

      if (nextPlay == null || nextPlay.boardnumber > play.boardnumber) {
        // Board number will be changed after the current play, so there should be a result record and we should update the current boardnumber
        List<LivefeedResult> livefeedResultList = LivefeedResult.find("match.id.sessionid = ? and match.id.matchid = ? and boardnumber = ?", currentPlay.match.id.sessionid, currentPlay.match.id.matchid, play.boardnumber).fetch();
        for (LivefeedResult livefeedResult : livefeedResultList) {
          Logger.info("Insert record into result with resultid " + livefeedResult.resultid);
          Result result = createResult(livefeedResult);
          result.save();
        }
        status.lastPublishedPlayExternalID = 0L;
        status.lastPublishedPlayIscard = 0L;
        if (nextPlay != null) {
          status.lastPublishedPlayBoardnumber = nextPlay.boardnumber;
        } else {
          status.lastPublishedPlayBoardnumber = BOARDNR_FINISHED; // End of match...
        }
      }

      // Save the play record
      play.save();
    } else {
      status.lastPublishedPlayBoardnumber = BOARDNR_FINISHED; // No play records, end of match...
    }
  }

  private void insertHandrecordIfNecessary(LivefeedTestDataMatchStatus status, LivefeedPlay livefeedPlay) {

    // Check if Handrecord already exists
    List<Handrecord> handrecords = Handrecord.find("sessionid = ? and boardnumber = ?", livefeedPlay.match.id.sessionid, livefeedPlay.boardnumber).fetch();

    if (handrecords.isEmpty()) {
      Logger.info("Insert record into handrecord with boardnumber " + livefeedPlay.boardnumber);
      // Insert handrecord
      HandRecordID id = new HandRecordID();
      id.sessionID = livefeedPlay.match.id.sessionid;
      id.boardNumber = livefeedPlay.boardnumber;

      LivefeedHandrecord livefeedHandrecord = LivefeedHandrecord.findById(id);
      Handrecord handrecord = createHandrecord(livefeedHandrecord);
      handrecord.save();
    }
  }

  @Override
  protected void doAdditionalMatchStuff(Match match) {
    matchStatuses.put(match.id, new LivefeedTestDataMatchStatus());
    // Testing only 1 match
    matchStatuses.clear();
    matchStatuses.put(new MatchID(507301L, 10109L), new LivefeedTestDataMatchStatus());

  }
}
