package models;

import play.db.jpa.GenericModel;

import javax.persistence.*;

@Entity
@Table(name = "handrecord")
public class Handrecord extends GenericModel {
  
  @Id
  public Long sessionID;
  @OneToOne
  @JoinColumn(name = "sessionid", referencedColumnName = "sessionid")
  public Session session;
  public Long boardNumber;
  
  public String north;
  public String south;
  public String east;
  public String west;

}
