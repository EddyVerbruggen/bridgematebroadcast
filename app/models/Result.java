package models;

import play.db.jpa.GenericModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "result")
public class Result extends GenericModel {

  @Id
  public Long resultid;

  @ManyToOne(optional = false)
  @JoinColumns({@JoinColumn(name = "matchid", referencedColumnName = "matchid"), @JoinColumn(name = "sessionid", referencedColumnName = "sessionid")})
  public Match match;

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
