package models.channel;

import models.Play;
import models.Subscriber;
import play.libs.F;

import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper channel class around Play eventstream object
 */
public class Channel {
  
  public ChannelID channelID;
  public ChannelType channelType;

  public Long lastPublishedPlayID = 0L;
  public Long lastPublishedResultID = 0L;

  private List<Subscriber> subscribers = new ArrayList<Subscriber>();
  
  private F.EventStream liveStream = new F.EventStream();

  // Use ChannelManager.subscribe to subscribe to a channel
  void subscribe(Subscriber subscriber) {
    subscribers.add(subscriber);
  }
  
  // Use ChannelManager.subscribe to unsubscribe from a channel
  void unsubscribe(Subscriber subscriber) {
    subscribers.remove(subscriber);
  }

  void unsubscribeAll() {
    subscribers.clear();
  }

  public void publish(Object publishObject) {
    liveStream.publish(publishObject);
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
