package models.livefeed;


import play.db.jpa.GenericModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "livefeedevent")
public class LivefeedTournament extends GenericModel {

  @Id
  public Long tournamentid;
  public String name;
  public String location;
  public Date startdate;
  public Date enddate;
  public String timezone;

}
