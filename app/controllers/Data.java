package controllers;

import models.Session;
import models.Tournament;
import play.mvc.Controller;

import java.util.List;

public class Data extends Controller {

  public static void tournaments() {
    final List<Tournament> tournaments = Tournament.findAll();
    renderJSON(tournaments);
  }

  public static void tournament(Long tournamentid) {
    final Tournament tournament = Tournament.findById(tournamentid);
    renderJSON(tournament);
  }

  public static void sessions(Long tournamentid) {
    final List<Session> sessions = Session.find("byTournamentid", tournamentid).fetch();
    renderJSON(sessions);
  }

  public static void session(Long tournamentid, Long sessionid) {
    final Session session = Session.find("tournament.tournamentid = ? and sessionid = ?", tournamentid, sessionid).first();
    renderJSON(session);
  }
}