package controllers;

import play.mvc.With;

@With(Secure.class)
public class Sessions extends CRUD {
}
