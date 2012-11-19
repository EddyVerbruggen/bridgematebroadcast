package controllers;

import models.Session;
import models.Tournament;
import play.data.validation.*;
import play.data.validation.Error;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.With;

import java.util.ArrayList;
import java.util.List;

@With(Secure.class)
public class AdminController extends Controller {

  public static void admin() {
    List<Tournament> tournaments = getActiveTournamentsAndAddEmptyLines();

    List<Session> sessions = getActiveSessions();
    for (int i = 0; i < 5; i++) {
      sessions.add(new Session());
    }

    List<Tournament> tournamentDropdown = getTournamentDropdown();

    render(tournaments, sessions, tournamentDropdown);
  }

  private static List<Tournament> getTournamentDropdown() {
    List<Tournament> tournamentDropdown = new ArrayList<Tournament>();
    Tournament t = new Tournament();
    t.name = "";
    tournamentDropdown.add(t);
    tournamentDropdown.addAll(getActiveTournaments());
    return tournamentDropdown;
  }

  private static List<Session> getActiveSessions() {
    return Session.find("status <> ?", 2L).fetch();
  }

  private static List<Tournament> getActiveTournaments() {
    return Tournament.find("status <> ?", 2L).fetch();
  }

  private static List<Tournament> getActiveTournamentsAndAddEmptyLines() {
    List<Tournament> tournaments = getActiveTournaments();
    for (int i = 0; i < 5; i++) {
      tournaments.add(new Tournament());
    }
    return tournaments;
  }

  public static void saveTournaments(List<Tournament> tournaments) {

    for (Tournament t : tournaments) {
      if (!t.isEmpty()) {
        Validation.ValidationResult result = validation.valid(t);
        System.out.println(result);
      }
    }

    if (validation.hasErrors()) {
      List<Session> sessions = getActiveSessions();
      render("@admin", tournaments, sessions);
    }

    for (Tournament t : tournaments) {
      if (t.tournamentid != null) {
        // Update the entity that is already in the database
        Tournament dbTournament = Tournament.findById(t.tournamentid);
        dbTournament.name = t.name;
        dbTournament.location = t.location;
        dbTournament.startdate = t.startdate;
        dbTournament.enddate = t.enddate;
        dbTournament.timezone = t.timezone;
        dbTournament.save();
      } else {
        // No id there, so create the new provided tournament and set its default values
        if (!t.isEmpty()) {
          Tournament lastTournament = Tournament.find("order by tournamentid desc").first();
          t.tournamentid = lastTournament.tournamentid + 1;
          t.status = 0L;
          t.save();
        }
      }
    }

    // Reload the admin page
    flash.success("Events zijn opgeslagen");
    admin();
  }

  /**
   * Restructure the array of params provided by the html so that we receive an array of tournaments in the method saveTournaments
   */
  @Before(only = {"saveTournaments"})
  static void parseTournamentParams() {
    String[] tournamentIds = params.getAll("tournaments.tournamentid");
    String[] tournamentNames = params.getAll("tournaments.name");
    String[] tournamentLocations = params.getAll("tournaments.location");
    String[] tournamentStartdates = params.getAll("tournaments.startdate");
    String[] tournamentEnddates = params.getAll("tournaments.enddate");
    String[] tournamentTimezones = params.getAll("tournaments.timezone");

    params.remove("tournaments.tournamentid");
    params.remove("tournaments.name");
    params.remove("tournaments.location");
    params.remove("tournaments.startdate");
    params.remove("tournaments.enddate");
    params.remove("tournaments.timezone");

    for (int i = 0; i < tournamentIds.length; i++) {
      params.put("tournaments[" + i + "].tournamentid", tournamentIds[i]);
      params.put("tournaments[" + i + "].name", tournamentNames[i]);
      params.put("tournaments[" + i + "].location", tournamentLocations[i]);
      params.put("tournaments[" + i + "].startdate", tournamentStartdates[i]);
      params.put("tournaments[" + i + "].enddate", tournamentEnddates[i]);
      params.put("tournaments[" + i + "].timezone", tournamentTimezones[i]);
    }
  }

  public static void saveSessions(List<Session> sessions) {

    for (Session s : sessions) {
      if (!s.isEmpty()) {
        // if tournament is empty, set to null so required annotation works
        if (s.tournament.isEmpty()) {
          s.tournament = null;
        }
        Validation.ValidationResult result = validation.valid(s);
      }
    }

    if (validation.hasErrors()) {
      List<Tournament> tournaments = getActiveTournamentsAndAddEmptyLines();
      List<Tournament> tournamentDropdown = getTournamentDropdown();
      render("@admin", tournaments, sessions, tournamentDropdown);
    }

    for (Session s : sessions) {
      if (s.sessionid != null) {
        // Update the entity that is already in the database
        Session dbSession = Session.findById(s.sessionid);
        dbSession.name = s.name;
        dbSession.tournament = Tournament.findById(s.tournament.tournamentid);
        dbSession.date = s.date;
        dbSession.save();
      } else {
        // No id there, so create the new provided tournament and set its default values
        if (!s.isEmpty()) {
          Session lastSession = Session.find("order by sessionid desc").first();
          s.sessionid = lastSession.sessionid + 1;
          s.tournament = Tournament.findById(s.tournament.tournamentid);
          s.status = 0L;
          s.save();
        }
      }
    }

    // Reload the admin page
    flash.success("Sessions zijn opgeslagen");
    admin();
  }


  @Before(only = {"saveSessions"})
  static void parseSessionParams() {
    String[] sessionIds = params.getAll("sessions.sessionid");
    String[] sessionNames = params.getAll("sessions.name");
    String[] sessionTournamentIds = params.getAll("sessions.tournamentid[]");
    String[] tournamentStartdates = params.getAll("sessions.date");

    params.remove("sessions.sessionid");
    params.remove("sessions.name");
    params.remove("sessions.tournamentid[]");
    params.remove("sessions.tournamentid");
    params.remove("sessions.date");

    for (int i = 0; i < sessionIds.length; i++) {
      params.put("sessions[" + i + "].sessionid", sessionIds[i]);
      params.put("sessions[" + i + "].name", sessionNames[i]);
      params.put("sessions[" + i + "].tournament.tournamentid", sessionTournamentIds[i]);
      params.put("sessions[" + i + "].date", tournamentStartdates[i]);
    }

    System.out.println(params.all());
  }
}
