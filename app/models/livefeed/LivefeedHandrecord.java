package models.livefeed;

import models.HandRecordID;
import play.db.jpa.GenericModel;

import javax.persistence.*;

@Entity
@Table(name="livefeedhandrecord")
public class LivefeedHandrecord extends GenericModel {

  @EmbeddedId
  public HandRecordID handRecordID;

//  @Id
//  public Long sessionID;
//  @OneToOne
//  @JoinColumn(name = "sessionid", referencedColumnName = "sessionid")
//  public LivefeedSession session;
//  public Long boardNumber;

  public String north;
  public String south;
  public String east;
  public String west;

}
