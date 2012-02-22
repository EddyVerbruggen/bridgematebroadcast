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

  public Channel subscribe(String subscriber, Long tournamentID, Long sessionID, Long matchID){
    Channel channel = findChannel(tournamentID, sessionID, matchID);
    
    if (channel == null) {
      channel = new Channel();
      channel.channelID = new ChannelID(tournamentID, sessionID, matchID);
      channel.channelType = ChannelType.MATCH;
      channels.add(channel);

      Logger.info("created channel");
    }
    
    channel.subscribe(subscriber);

    return channel;
  }

  public Channel findChannel(Long tournamentID, Long sessionID, Long matchID) {
    ChannelID channelID = new ChannelID(tournamentID, sessionID, matchID);
    
    for (Channel channel : channels) {
      if (channel.channelID.equals(channelID)) {
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
