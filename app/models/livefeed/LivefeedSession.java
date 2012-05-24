package models.livefeed;

import play.db.jpa.GenericModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Date;

@Entity
public class LivefeedSession extends GenericModel {

  @Id
  public Long sessionid;
  @ManyToOne
  @JoinColumn(name = "tournamentid")
  public LivefeedTournament tournament;
  public String name;
  public Date date;
  public Long status;
}
