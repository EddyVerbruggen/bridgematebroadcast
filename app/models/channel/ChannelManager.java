package models.channel;

import models.Subscriber;
import play.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton ChannelManager. Responsible for creating and managing channels.
 * Subscribing/unsubscribing to a channel happens here.
 */
public class ChannelManager {

  // All existing channels in the application
  private List<Channel> channels = new ArrayList<Channel>();

  private static ChannelManager instance;

  private ChannelManager(){}
  
  public static ChannelManager getInstance() {
    if (instance == null) {
      instance = new ChannelManager();
    }
    return instance;
  }

  /**
   * Subscribe to a channel with sessionid and (possibly) matchid
   * @param subscriber The subscriber to the channel
   * @param sessionID The session to subscribe to
   * @param matchID The match to subscribe to
   * @return
   *
   */
  public Channel subscribe(Subscriber subscriber, Long sessionID, Long matchID){
    Channel channel = findChannel(sessionID, matchID);
    
    if (channel == null) {
      // If the channel isn't there yet, create one
      channel = new Channel();
      channel.channelID = new ChannelID(sessionID, matchID);
      channel.channelType = ChannelType.MATCH;
      channels.add(channel);

      //TODO: Set lastPublishedPlayID?

      Logger.info("created channel for [sessionid = " + sessionID + ", matchid = " + matchID +"]");
    }
    
    channel.subscribe(subscriber);

    return channel;
  }

  public Channel findChannel(Long sessionID, Long matchID) {
    ChannelID channelID = new ChannelID(sessionID, matchID);
    
    for (Channel channel : channels) {
      if (channel.channelID.equals(channelID)) {
        return channel;
      }
    }

    return null;
  }

  public void unsubscribe(Channel subscriptionChannel, Subscriber subscriber) {
    subscriptionChannel.unsubscribe(subscriber);
    if (subscriptionChannel.hasNoSubscriptions()) {
      channels.remove(subscriptionChannel);
    }
  }

  public List<Channel> getChannels() {
    return channels;
  }
}
