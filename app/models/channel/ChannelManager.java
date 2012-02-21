package models.channel;

import play.Logger;

import java.util.ArrayList;
import java.util.List;

public class ChannelManager {

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
   *
   */
  public Channel subscribe(String subscriber, Long matchID){
    Channel channel = findChannel(matchID);
    
    if (channel == null) {
      channel = new Channel();
      channel.id = matchID;
      channel.channelType = ChannelType.MATCH;
      channels.add(channel);

      Logger.info("created channel");
    }
    
    channel.subscribe(subscriber);

    return channel;
  }

  public Channel findChannel(Long matchID) {
    for (Channel channel : channels) {
      if (channel.id.equals(matchID)) {
        return channel;
      }
    }

    return null;
  }

  public void unsubscribe(Channel subscriptionChannel, String subscriber) {
    subscriptionChannel.unsubscribe(subscriber);
    if (subscriptionChannel.hasNoSubscriptions()) {
      channels.remove(subscriptionChannel);
    }
  }

  public List<Channel> getChannels() {
    return channels;
  }
}
