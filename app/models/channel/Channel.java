package models.channel;

import controllers.model.WebsocketSubscriber;
import models.Subscriber;
import play.libs.F;

import java.util.*;

/**
 * Wrapper channel class around Play eventstream object
 */
public class Channel {
  
  public ChannelID channelID;
  public ChannelType channelType;

  // Play publish status data within Match
  public Long lastPublishedPlayBoardnumber = 0L;
  public Long lastPublishedPlayIscard = 0L;
  public Long lastPublishedPlayExternalID = 0L;

  // Result publish status data within Match
  public Long lastPublishedResultExternalID = 0L;
  public List<Long> publishedBoardNumbers = new ArrayList<Long>();

  private List<WebsocketSubscriber> subscribers = new ArrayList<WebsocketSubscriber>();

  private Map<WebsocketSubscriber, F.EventStream> streams = new HashMap<WebsocketSubscriber, F.EventStream>();

  // Use ChannelManager.subscribe to subscribe to a channel
  void subscribe(WebsocketSubscriber subscriber) {
    subscribers.add(subscriber);
    streams.put(subscriber, new F.EventStream());
  }
  
  // Use ChannelManager.subscribe to unsubscribe from a channel
  void unsubscribe(WebsocketSubscriber subscriber) {
    subscribers.remove(subscriber);
    streams.remove(subscriber);
  }

  void unsubscribeAll() {
    subscribers.clear();
    streams.clear();
  }

  public synchronized void publish(Object publishObject) {
    Set subscribers = streams.keySet() ;

    Iterator itr = subscribers.iterator();
    while (itr.hasNext()) {
      WebsocketSubscriber subscriber = (WebsocketSubscriber) itr.next();
      F.EventStream stream = streams.get(subscriber);
      if (stream != null) {
        stream.publish(publishObject);
      }
    }
  }

  public synchronized F.Promise nextEvent(WebsocketSubscriber subscriber) {
    F.EventStream stream = streams.get(subscriber);
    return stream.nextEvent();
  }

  @Override
  public String toString() {
    return "Channel[" + channelID + "]";
  }

  public boolean hasNoSubscriptions() {
    return subscribers.isEmpty();
  }
}

