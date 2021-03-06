package models.livefeed;

import play.db.jpa.GenericModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="livefeedplay")
public class LivefeedPlay extends GenericModel {

  @Id
  public Long playid;

  @ManyToOne(optional = false)
  @JoinColumns({@JoinColumn(name = "matchid", referencedColumnName = "matchid"), @JoinColumn(name = "sessionid", referencedColumnName = "sessionid")})
  public LivefeedMatch match;
  public Long externalid;
  public Long roundnumber;
  public Long boardnumber;
  public Long counter;
  public String direction;
  public Long iscard;
  public String action;
  public Date timestamp;
  public Long iserased;

  @Override
  public String toString() {
    return "Play{" +
      "playid=" + playid +
      ", match=" + match.id +
      "}";
  }
}
