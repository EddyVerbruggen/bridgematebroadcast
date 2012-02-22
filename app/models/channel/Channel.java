package models.channel;

import models.Play;
import play.libs.F;

import java.util.ArrayList;
import java.util.List;

public class Channel {
  
  public ChannelID channelID;
  public ChannelType channelType;

  // TODO: Replace with list of subscriber objects
  private List<String> subscribers = new ArrayList<String>();
  
  private F.EventStream liveStream = new F.EventStream();

  public void subscribe(String subscriber) {
    subscribers.add(subscriber);
  }
  
  public void unsubscribe(String subscriber) {
    subscribers.remove(subscriber);
  }

  public void publish(Play play) {
    liveStream.publish(play);
  }
  
  public F.Promise nextEvent() {
    return liveStream.nextEvent();
  }

  @Override
  public String toString() {
    return "Channel[" + channelID + "]";
  }

  public boolean hasNoSubscriptions() {
    return subscribers.isEmpty();
  }
}
