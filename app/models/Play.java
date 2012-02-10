package models;

import play.db.jpa.GenericModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "play")
public class Play extends GenericModel {
  
  @Id
  public Long playid;
  public Long matchid;
  public Long sessionid;
  public Long externalid;
  public Long roundnumber;
  public Long boardnumber;
  public Long counter;
  public String direction;
  public Long iscard;
  public String action;
  public Date timestamp;
  public Long iserased;
}
