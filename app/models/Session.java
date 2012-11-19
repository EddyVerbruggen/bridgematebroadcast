package models;

import play.data.binding.As;
import play.data.validation.Required;
import play.db.jpa.GenericModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "`session`")
public class Session extends GenericModel {
  
  @Id
//  @GeneratedValue(strategy = GenerationType.AUTO)
  public Long sessionid;

  @ManyToOne
  @JoinColumn(name = "tournamentid")
  @Required
  public Tournament tournament;

  @Required
  public String name;

  @Required
  @As(lang={"*"}, value={"dd/MM/yyyy"})
  public Date date;

  public Long status;

  public boolean isEmpty() {
    return sessionid == null &&
        (tournament == null || tournament.tournamentid == null) &&
        (name == null || "".equals(name)) &&
        date == null &&
        status == null;
  }

  public String toString() {
    return name + " (Event: " + (tournament != null ? tournament.name : "geen") + ")";
  }
}
