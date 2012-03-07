package models;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class MatchID implements Serializable {
  public Long matchid;
  public Long sessionid;

  public MatchID(Long matchid, Long sessionid) {
    this.matchid = matchid;
    this.sessionid = sessionid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MatchID matchID = (MatchID) o;

    if (!matchid.equals(matchID.matchid)) {
      return false;
    }
    if (!sessionid.equals(matchID.sessionid)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = matchid.hashCode();
    result = 31 * result + sessionid.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "{ matchid = " + matchid + ", sessionid = "+ sessionid + "}";
  }
}
