package models.livefeed;

import play.db.jpa.GenericModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="livefeedsession")
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
