package models.channel;

public class ChannelID {
  private Long tournamentID;
  private Long sessionID;
  private Long matchID;
  
  public ChannelID (Long tournamentID, Long sessionID, Long matchID) {
    this.tournamentID = tournamentID;
    this.sessionID = sessionID;
    this.matchID = matchID;
  }

  public Long getTournamentID() {
    return tournamentID;
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
    if (tournamentID != null ? !tournamentID.equals(channelID.tournamentID) : channelID.tournamentID != null)
      return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = tournamentID != null ? tournamentID.hashCode() : 0;
    result = 31 * result + (sessionID != null ? sessionID.hashCode() : 0);
    result = 31 * result + (matchID != null ? matchID.hashCode() : 0);
    return result;
  }
}
