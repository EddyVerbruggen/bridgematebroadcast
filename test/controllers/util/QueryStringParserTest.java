package controllers.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import play.test.UnitTest;

//public class QueryStringParserTest extends UnitTest {
public class QueryStringParserTest {

  private QueryStringParser parser;
  
  @Test
  public void testGetTournaments() {
    parser = new QueryStringParser("getTournaments");
    Assert.assertEquals("getTournaments", parser.getCommand());
  }

  @Test
  public void testGetSingleTournament() {
    parser = new QueryStringParser("getTournament(100)");
    Assert.assertEquals("getTournament", parser.getCommand());
    Assert.assertFalse(parser.getParams().isEmpty());
    Assert.assertEquals("100", parser.getParams().get(0));
  }

  @Test
  public void testGetTournamentSessions() {
    parser = new QueryStringParser("getTournamentSessions(100)");
    Assert.assertEquals("getTournamentSessions", parser.getCommand());
    Assert.assertEquals("100", parser.getParams().get(0));
  }

  @Test
  public void testGetSessionData() {
    parser = new QueryStringParser("getSessionData(100,10008)");
    Assert.assertEquals("getSessionData", parser.getCommand());
    Assert.assertEquals(2, parser.getParams().size());
    Assert.assertEquals("100", parser.getParams().get(0));
    Assert.assertEquals("10008", parser.getParams().get(1));
  }
  
}
