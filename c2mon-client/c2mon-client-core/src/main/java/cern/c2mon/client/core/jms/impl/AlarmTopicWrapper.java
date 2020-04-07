package cern.c2mon.client.core.jms.impl;

import java.util.concurrent.ExecutorService;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import lombok.extern.slf4j.Slf4j;

import cern.c2mon.client.core.config.C2monClientProperties;
import cern.c2mon.client.core.jms.AlarmListener;
import cern.c2mon.shared.client.tag.TagUpdate;

@Slf4j
public class AlarmTopicWrapper extends AbstractTopicWrapper<AlarmListener, TagUpdate> {
  
  /**
   * Alarm Session.
   */
  private Session alarmSession;

  /**
   * Alarm Consumer.
   */
  private MessageConsumer alarmConsumer;

  public AlarmTopicWrapper(final SlowConsumerListener slowConsumerListener,
                              final ExecutorService topicPollingExecutor,
                              final C2monClientProperties properties) {
    super(slowConsumerListener, topicPollingExecutor, properties.getJms().getAlarmTopic());
  }
  
  @Override
  protected AbstractListenerWrapper<AlarmListener, TagUpdate> createListenerWrapper(SlowConsumerListener slowConsumerListener, final ExecutorService topicPollingExecutor) {
    return new AlarmListenerWrapper(HIGH_LISTENER_QUEUE_SIZE, slowConsumerListener, topicPollingExecutor);
  }
  
  /**
   * Unsubscribes from the alarm topic.
   * @throws JMSException if problem subscribing
   */
  protected void unsubscribeFromAlarmTopic() throws JMSException {
    alarmSession.close();
    alarmSession = null;
    alarmConsumer.close();
    alarmConsumer = null;
    log.debug("Successfully unsubscribed from alarm topic");
  }
}
