package models.livefeed;

import models.MatchID;
import play.db.jpa.GenericModel;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
public class LivefeedMatch extends GenericModel {

  @EmbeddedId
  public MatchID id;
//  @ManyToOne(optional = false)
//  @JoinColumn(name = "sessionid", insertable = false, updatable = false)
//  public Session session;

  public Long tablenumber;
  public String section;
  public String room;
  public Long othertablematchid; // OneToOne or kiss?

  public String playernorth;
  public String playereast;
  public String playersouth;
  public String playerwest;

  public String playercountrynorth;
  public String playercountryeast;
  public String playercountrysouth;
  public String playercountrywest;

  public Double carryoverns;
  public Double carryoverew;

  public Long status;

  public String teamnamens;
  public String teamnameew;

  public boolean isFinished() {
    return 2 == status;
  }
}
