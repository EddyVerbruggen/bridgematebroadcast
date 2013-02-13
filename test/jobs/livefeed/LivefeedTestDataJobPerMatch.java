package jobs.livefeed;

import models.*;
import models.livefeed.LivefeedHandrecord;
import models.livefeed.LivefeedPlay;
import models.livefeed.LivefeedResult;
import play.Logger;
import play.jobs.Every;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Every("5s")
public class LivefeedTestDataJobPerMatch extends LivefeedTestDataJob {

  private Map<MatchID, LivefeedTestDataMatchStatus> matchStatuses = new HashMap<MatchID, LivefeedTestDataMatchStatus>();

  @Override
  protected void doRunning() {
    Logger.info("doRunning");

    for (MatchID key : matchStatuses.keySet()) {
      insertNextRecordForMatch(key);
    }
  }

  private void insertNextRecordForMatch(MatchID key) {
    LivefeedTestDataMatchStatus status = matchStatuses.get(key);

    List<LivefeedPlay> nextPlays = LivefeedPlay.find(
        "sessionid = ? and matchid = ? and boardnumber >= ? and ((iscard = ? and externalid > ?) or (iscard > ?)) order by boardnumber ASC, iscard ASC, externalid ASC",
        key.sessionid,
        key.matchid,
        status.lastPublishedPlayBoardnumber,
        status.lastPublishedPlayIscard,
        status.lastPublishedPlayExternalID,
        status.lastPublishedPlayIscard
    ).fetch(1);

    if (nextPlays.size() > 0) {
      LivefeedPlay livefeedPlay = nextPlays.get(0);

      Long currentBoardNumber = status.lastPublishedPlayBoardnumber;
      // First: check if we need to insert the handrecord
      insertHandrecordIfNecessary(status, livefeedPlay);

      // Second: result record should be inserted in the Play table
      Logger.info("Insert record into play with playid " + livefeedPlay.playid);
      PlayRecord play = createPlay(livefeedPlay);

      if (currentBoardNumber < play.boardnumber) {
        // Board number has changed since previous play, so there should be a result record and we should update the current boardnumber
        List<LivefeedResult> livefeedResultList = LivefeedResult.find("match.id.sessionid = ? and match.id.matchid = ? and boardnumber = ?", livefeedPlay.match.id.sessionid, livefeedPlay.match.id.matchid, currentBoardNumber).fetch();
        for (LivefeedResult livefeedResult : livefeedResultList) {
          Logger.info("Insert record into result with resultid " + livefeedResult.resultid);
          Result result = createResult(livefeedResult);
          result.save();
        }
      }

      // Save the play record
      play.save();
      status.lastPublishedPlayIscard = livefeedPlay.iscard;
      status.lastPublishedPlayExternalID = livefeedPlay.externalid;
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
    status.lastPublishedPlayBoardnumber = livefeedPlay.boardnumber;
  }

  @Override
  protected void doAdditionalMatchStuff(Match match) {
    matchStatuses.put(match.id, new LivefeedTestDataMatchStatus());
  }
}
