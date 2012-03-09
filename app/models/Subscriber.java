package models;

import play.db.jpa.GenericModel;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "subscriber")
public class Subscriber extends GenericModel {
  
  @Id
  public Long id;
  public String name;
  public String loginName;
  public String password;
  public String active;

}
