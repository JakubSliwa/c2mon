/******************************************************************************
 * Copyright (C) 2010-2020 CERN. All rights not expressly granted are reserved.
 *
 * This file is part of the CERN Control and Monitoring Platform 'C2MON'.
 * C2MON is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the license.
 *
 * C2MON is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with C2MON. If not, see <http://www.gnu.org/licenses/>.
 *****************************************************************************/
package cern.c2mon.server.client.publish;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.jms.JmsException;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import cern.c2mon.server.cache.alarm.AlarmAggregator;
import cern.c2mon.server.cache.alarm.AlarmAggregatorListener;
import cern.c2mon.server.client.config.ClientProperties;
import cern.c2mon.server.client.util.TransferObjectFactory;
import cern.c2mon.server.common.alarm.Alarm;
import cern.c2mon.server.common.alarm.TagWithAlarms;
import cern.c2mon.server.common.alarm.TagWithAlarmsImpl;
import cern.c2mon.server.common.config.ServerConstants;
import cern.c2mon.server.common.republisher.Publisher;
import cern.c2mon.server.common.republisher.Republisher;
import cern.c2mon.server.common.republisher.RepublisherFactory;
import cern.c2mon.server.common.tag.Tag;
import cern.c2mon.shared.client.serializer.TransferTagSerializer;
import cern.c2mon.shared.client.tag.TransferTagValueImpl;
import cern.c2mon.shared.util.jms.JmsSender;

/**
 * Publishes active alarms and the corresponding tag value to the C2MON client applications on the
 * alarm publication topic.
 *
 * <p>Will attempt re-publication of alarms if JMS connection fails.
 *
 * @author Matthias Braeger
 */
@Slf4j
@Service
@ManagedResource(description = "Bean publishing Alarm updates (TagWithAlarms) to the clients")
public class AlarmPublisherTagWithAlarms implements SmartLifecycle, AlarmAggregatorListener, Publisher<TagWithAlarms> {

  /** Bean providing for sending JMS messages and waiting for a response */
  private final JmsSender jmsSender;
  
  /** The configured JMS alarm topic, extracted from {@link ClientProperties} */
  private final String tagWithAlarmsTopic;
  
  /** Listens for Tag updates, evaluates all associated alarms and passes the result */
  private final AlarmAggregator alarmAggregator;

  /** Contains re-publication logic */
  private final Republisher<TagWithAlarms> republisher;

  /** Lifecycle flag */
  private volatile boolean running = false;

  /**
   * Default Constructor
   * @param jmsSender Used for sending JMS messages and waiting for a response.
   * @param alarmAggregator Required to receive notifications when a tag has changed.
   * @param properties The configured {@link ClientProperties}. Required to determine the JMS alarm topic.
   */
  @Autowired
  public AlarmPublisherTagWithAlarms(@Qualifier("alarmTopicPublisher") final JmsSender jmsSender, 
      final AlarmAggregator alarmAggregator,
      final ClientProperties properties) {

    this.jmsSender = jmsSender;
    this.alarmAggregator = alarmAggregator;
    this.tagWithAlarmsTopic = properties.getJms().getTagWithAlarmsTopic();
    
    republisher = RepublisherFactory.createRepublisher(this, "TagWithAlarms");
  }

  /**
   * Registering this listener to alarms.
   */
  @PostConstruct
  void init() {
    this.alarmAggregator.registerForTagUpdates(this);
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public void stop(Runnable runnable) {
    stop();
    runnable.run();
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  public void start() {
    log.debug("Starting Alarm publisher for TagWithAlarms");
    running = true;
    republisher.start();
  }

  @Override
  public void stop() {
    log.debug("Stopping Alarm publisher for TagWithAlarms");
    republisher.stop();
    running = false;
  }

  @Override
  public int getPhase() {
    return ServerConstants.PHASE_STOP_LAST - 1;
  }

  /**
   * @return the total number of failed publications since the publisher start
   */
  @ManagedOperation(description = "Returns the total number of failed alarm publication attempts since the application started")
  public long getNumberFailedPublications() {
    return republisher.getNumberFailedPublications();
  }

  /**
   * @return the number of current tag updates awaiting publication to the clients
   */
  @ManagedOperation(description = "Returns the current number of alarms awaiting re-publication (should be 0 in normal operation)")
  public int getSizeUnpublishedList() {
    return republisher.getSizeUnpublishedList();
  }

  /**
   * Send update for all alarms that changed
   */
  @Override
  public void notifyOnUpdate(Tag tag, List<Alarm> alarms) {
    List<Alarm> changedAlarms = alarms.stream()
        .filter(a -> a.getSourceTimestamp().equals(tag.getTimestamp())).collect(Collectors.toList());
    
    if (!changedAlarms.isEmpty()) {
      publish(new TagWithAlarmsImpl(tag, changedAlarms));
    }
  }
  
  @Override
  public void publish(final TagWithAlarms tagWithAlarms) {
    TransferTagValueImpl tagValue = TransferObjectFactory.createTransferTagValue(tagWithAlarms);
    
    if (log.isTraceEnabled()) {
      log.trace("Publishing alarm(s) with full tag object for tag id #{} to topic {}", tagWithAlarms.getTag().getId(), tagWithAlarmsTopic);
    }

    try {
      jmsSender.sendToTopic(TransferTagSerializer.toJson(tagValue), tagWithAlarmsTopic);
    } catch (JmsException e) {
      log.error("Error publishing alarm(s) with full tag object to clients - submitting for republication for tag #{} + alarms", tagWithAlarms.getTag().getId(), e);
      republisher.publicationFailed(tagWithAlarms);
    }
    
  }
}
