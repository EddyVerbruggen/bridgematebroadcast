package controllers;

import models.Tournament;
import play.mvc.With;

@With(Secure.class)
@CRUD.For(Tournament.class)
public class Events extends CRUD {
}
