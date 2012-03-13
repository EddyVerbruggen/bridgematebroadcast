package models.channel;

public class ChannelID {
  private Long sessionID;
  private Long matchID;
  
  public ChannelID (Long sessionID, Long matchID) {
    this.sessionID = sessionID;
    this.matchID = matchID;
  }

  public Long getSessionID() {
    return sessionID;
  }

  public Long getMatchID() {
    return matchID;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ChannelID channelID = (ChannelID) o;

    if (matchID != null ? !matchID.equals(channelID.matchID) : channelID.matchID != null) return false;
    if (sessionID != null ? !sessionID.equals(channelID.sessionID) : channelID.sessionID != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = sessionID != null ? sessionID.hashCode() : 0;
    result = 31 * result + (matchID != null ? matchID.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "[sessionid = " + sessionID +", matchid = " + matchID + "]";
  }
}
