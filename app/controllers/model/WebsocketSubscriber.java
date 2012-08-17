package controllers.model;

import models.Subscriber;

/**
 * Decorator around subscriber to support multiple websocket connections per subscriber
 */
public class WebsocketSubscriber {

  public Subscriber subscriber;
  public String websocketIdentifier;

  public WebsocketSubscriber(Subscriber subscriber, String websocketIdentifier) {
    this.subscriber = subscriber;
    this.websocketIdentifier = websocketIdentifier;
  }
}
