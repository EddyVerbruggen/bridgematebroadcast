package models;

import play.data.binding.As;
import play.data.validation.MaxSize;
import play.data.validation.Required;
import play.db.jpa.GenericModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "event")
public class Tournament extends GenericModel {

  @Id
//  @GeneratedValue(strategy = GenerationType.AUTO)
  public Long tournamentid;

  @Required
  @MaxSize(255)
  public String name;

  @Required
  @MaxSize(255)
  public String location;

  @Required
  @As(lang={"*"}, value={"dd/MM/yyyy"})
  public Date startdate;

  @Required
  @As(lang={"*"}, value={"dd/MM/yyyy"})
  public Date enddate;

  public String timezone;

  public Long status;

  @Override
  public String toString() {
    return name + " (Lokatie: " + location + ")";
  }

  public boolean isEmpty() {
    return tournamentid == null &&
        (name == null || "".equals(name)) &&
        (location == null || "".equals(location)) &&
        startdate == null &&
        enddate == null;
  }
}
