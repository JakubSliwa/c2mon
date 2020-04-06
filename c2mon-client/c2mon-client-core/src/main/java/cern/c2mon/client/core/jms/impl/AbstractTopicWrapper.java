package cern.c2mon.client.core.jms.impl;

import java.util.concurrent.ExecutorService;

import javax.jms.*;

import org.apache.activemq.command.ActiveMQTopic;

import lombok.Getter;

import cern.c2mon.client.core.jms.SupervisionListener;

abstract class AbstractTopicWrapper<T, U> {
  
  /**
   * Buffer before warnings for slow consumers are sent. Value for topics with
   * few updates (heartbeat, admin message)
   */
  static final int DEFAULT_LISTENER_QUEUE_SIZE = 100;
  
  /**
   * Buffer before warnings for slow consumers are sent, for tags, alarms (where
   * larger buffer is desirable).
   */
  static final int HIGH_LISTENER_QUEUE_SIZE = 10000;
  
  /**
   * Topic on which server heartbeat messages are arriving.
   */
  @Getter
  private final Destination topic;
  
  @Getter
  private final AbstractListenerWrapper<T, U> listenerWrapper;
  
  public AbstractTopicWrapper(final SlowConsumerListener slowConsumerListener,
                              final ExecutorService topicPollingExecutor,
                              String topicName) {
    this.topic = new ActiveMQTopic(topicName);
    listenerWrapper = createListenerWrapper(slowConsumerListener, topicPollingExecutor);
    listenerWrapper.start();
  }
  
  abstract AbstractListenerWrapper<T, U> createListenerWrapper(SlowConsumerListener slowConsumerListener, final ExecutorService topicPollingExecutor);
  
  /**
   * Subscribes to the topic. Called when refreshing all subscriptions.
   * @param connection The JMS connection
   * @throws JMSException if problem subscribing
   */
  public void subscribeToTopic(Connection connection) throws JMSException {
    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
    MessageConsumer consumer = session.createConsumer(topic);
    consumer.setMessageListener(listenerWrapper);
  }
  
  public void removeListener(T listener) {
    if (listener != null) {
      listenerWrapper.removeListener(listener);
    }
  }
  
  public void addListener(T listener) {
    if (listener != null) {
      listenerWrapper.addListener(listener);
    }
  }

  public int getQueueSize() {
    return listenerWrapper.getQueueSize();
  }
  
  public void stop() {
    listenerWrapper.stop();
  }
}
