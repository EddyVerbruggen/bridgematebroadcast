package models;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class HandRecordID implements Serializable {

  public Long sessionID;
  public Long boardNumber;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HandRecordID that = (HandRecordID) o;

    if (boardNumber != null ? !boardNumber.equals(that.boardNumber) : that.boardNumber != null) return false;
    if (sessionID != null ? !sessionID.equals(that.sessionID) : that.sessionID != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = sessionID != null ? sessionID.hashCode() : 0;
    result = 31 * result + (boardNumber != null ? boardNumber.hashCode() : 0);
    return result;
  }
}
