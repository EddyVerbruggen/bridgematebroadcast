package models;

import play.db.jpa.GenericModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "`session`")
public class Session extends GenericModel {
  
  @Id
  public Long sessionid;
  @ManyToOne
  @JoinColumn(name = "tournamentid")
  public Tournament tournament;
  public String name;
  public Date date;
  public Long status;
}
