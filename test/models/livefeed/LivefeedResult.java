package models.livefeed;

import play.db.jpa.GenericModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="livefeedresult")
public class LivefeedResult extends GenericModel {

  @Id
  public Long resultid;

  @ManyToOne(optional = false)
  @JoinColumns({@JoinColumn(name = "matchid", referencedColumnName = "matchid"), @JoinColumn(name = "sessionid", referencedColumnName = "sessionid")})
  public LivefeedMatch match;

  public Long externalid;
  public Long roundnumber;
  public Long boardnumber;
  public Long pairns;
  public Long pairew;
  public String declarer;
  public String contract;
  public String result;
  public String lead;
  public Date timestamp;
  public Long iserased;
  public Long isintermediate;

}
