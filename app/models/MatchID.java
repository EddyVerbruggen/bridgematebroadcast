package models;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class MatchID implements Serializable {
  public Long matchid;
  public Long sessionid;
  public Long tablenumber;
  public String section;

  public MatchID(Long matchid, Long sessionid, Long tablenumber, String section) {
    this.matchid = matchid;
    this.sessionid = sessionid;
    this.tablenumber = tablenumber;
    this.section = section;
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
    if (!section.equals(matchID.section)) {
      return false;
    }
    if (!sessionid.equals(matchID.sessionid)) {
      return false;
    }
    if (!tablenumber.equals(matchID.tablenumber)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = matchid.hashCode();
    result = 31 * result + sessionid.hashCode();
    result = 31 * result + tablenumber.hashCode();
    result = 31 * result + section.hashCode();
    return result;
  }
}
