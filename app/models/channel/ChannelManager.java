package models.channel;

import models.PlayRecord;
import models.Result;
import models.Subscriber;
import play.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Singleton ChannelManager. Responsible for creating and managing channels.
 * Subscribing/unsubscribing to a channel happens here.
 */
public class ChannelManager {

  // All existing channels in the application
  private ConcurrentMap<ChannelID, Channel> channelMap = new ConcurrentHashMap<ChannelID, Channel>();
  
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
      channelMap.put(channel.channelID, channel);

      PlayRecord play = PlayRecord.find("sessionid = ? and matchid = ? order by playid DESC", sessionID, matchID).first();
      if (play != null) {
        channel.lastPublishedPlayID = play.playid;
      }

      Result result = Result.find("sessionid = ? and matchid = ? order by resultid DESC", sessionID, matchID).first();
      if (result != null) {
        channel.lastPublishedResultID = result.resultid;
      }

      Logger.info("created channel for [sessionid = " + sessionID + ", matchid = " + matchID +", lastPublishedPlayID = " + channel.lastPublishedPlayID + ", lastPublishedResultID = " + channel.lastPublishedResultID + "]");
    }
    
    channel.subscribe(subscriber);

    return channel;
  }

  public Channel findChannel(Long sessionID, Long matchID) {
    return channelMap.get(new ChannelID(sessionID, matchID));
  }

  public void unsubscribeAll(Channel subscriptionChannel) {
    subscriptionChannel.unsubscribeAll();
    channelMap.remove(subscriptionChannel.channelID);
  }
  
  public void unsubscribe(Channel subscriptionChannel, Subscriber subscriber) {
    subscriptionChannel.unsubscribe(subscriber);
    if (subscriptionChannel.hasNoSubscriptions()) {
      channelMap.remove(subscriptionChannel.channelID);
    }
  }

  public ConcurrentMap<ChannelID, Channel> getChannelMap() {
    return channelMap;
  }
}
