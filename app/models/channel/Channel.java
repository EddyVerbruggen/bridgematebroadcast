package models.channel;

import models.Play;
import play.libs.F;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper channel class around Play eventstream object
 */
public class Channel {
  
  public ChannelID channelID;
  public ChannelType channelType;

  public Long lastPublishedPlayID;

  // TODO: Replace with list of subscriber objects
  private List<String> subscribers = new ArrayList<String>();
  
  private F.EventStream liveStream = new F.EventStream();

  // Use ChannelManager.subscribe to subscribe to a channel
  void subscribe(String subscriber) {
    subscribers.add(subscriber);
  }
  
  // Use ChannelManager.subscribe to unsubscribe from a channel
  void unsubscribe(String subscriber) {
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
