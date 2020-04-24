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
package cern.c2mon.daq.common.messaging.impl;

import java.sql.Timestamp;
import java.util.Collection;

import javax.jms.JMSException;

import org.springframework.jms.JmsException;

import lombok.extern.slf4j.Slf4j;

import cern.c2mon.daq.common.conf.core.ProcessConfigurationHolder;
import cern.c2mon.daq.common.messaging.IProcessMessageSender;
import cern.c2mon.daq.common.messaging.JmsSender;
import cern.c2mon.shared.common.datatag.*;
import cern.c2mon.shared.common.process.ProcessConfiguration;
import cern.c2mon.shared.util.buffer.PullEvent;
import cern.c2mon.shared.util.buffer.PullException;
import cern.c2mon.shared.util.buffer.SynchroBuffer;
import cern.c2mon.shared.util.buffer.SynchroBufferListener;

/**
 * The ProcessMessageSender class is responsible for sending JMS messages from
 * the DAQ to the application server. <p>
 *
 * This class supports sending the updates to
 * multiple JMS connections. The sending itself is performed by a JMSSender
 * class. Several of these can be specified in the jmsSenders collection field.
 * Notice that the calls to these senders are made on the same threads, so it is
 * up to the JMSSenders to release the threads, if for instance they are not
 * critical.
 *
 * For low priority messages, two synchrobuffer's are used (one for persistent,
 * the other for non-persistent messages).
 */
@Slf4j
public class ProcessMessageSender implements IProcessMessageSender {

  /**
   * The buffer for non-persistent SourceDataTags objects
   */
  private SynchroBuffer<SourceDataTagValue> dataTagsBuffer;

  /**
   * The buffer for persistent SourceDataTags objects
   */
  private SynchroBuffer<SourceDataTagValue> persistentTagsBuffer;

  /**
   * The reference for the AliveTimer object
   */
  private AliveTimer aliveTimer;

  /**
   * The collection of JMS senders (each responsible for sending updates to a
   * specific broker). Injected in Spring XML configuration file.
   */
  private Collection<JmsSender> jmsSenders;

  public void init() {
    aliveTimer = new AliveTimer(this);

    ProcessConfiguration processConfiguration = ProcessConfigurationHolder.getInstance();
    // TODO move the min window size to properties or database
    // create and initialize dataTagsBuffer for non-persistent tags
    dataTagsBuffer = new SynchroBuffer<>(200, processConfiguration.getMaxMessageDelay(), 100, SynchroBuffer.DUPLICATE_OK);
    // create and initialize dataTagsBuffer for persistent tags
    persistentTagsBuffer = new SynchroBuffer<>(200, processConfiguration.getMaxMessageDelay(), 100, SynchroBuffer.DUPLICATE_OK);

    dataTagsBuffer.setSynchroBufferListener(new SynchroBufferEventsListener());
    persistentTagsBuffer.setSynchroBufferListener(new SynchroBufferEventsListener());

    dataTagsBuffer.enable();
    persistentTagsBuffer.enable();
  }

  /**
   * This method initializes and starts the AliveTimer. Since it's initialized
   * it periodically takes action to send AliveTag to TIM server (using
   * ProcessMessageSender's JMS queue connection)
   */
  public final void startAliveTimer() {
    ProcessConfiguration processConfiguration = ProcessConfigurationHolder.getInstance();
    aliveTimer.setInterval(processConfiguration.getAliveInterval());
  }

  /**
   * Stops the Process alive timer. Used at final DAQ shutdown.
   */
  public final void stopAliveTimer() {
    if (aliveTimer != null) {
      aliveTimer.terminateTimer();
    }
  }

  /**
   * This method is responsible for creating a JMS XML message containing alive
   * tag and putting it to the TIM JMS queue
   */
  @Override
  public final void sendAlive() {
    ProcessConfiguration processConfiguration = ProcessConfigurationHolder.getInstance();
    log.debug("sending AliveTag. tag id : " + processConfiguration.getAliveTagID());

    long timestamp = System.currentTimeMillis();
    SourceDataTagValue aliveTagValue = SourceDataTagValue.builder()
        .id(Long.valueOf(processConfiguration.getAliveTagID()))
        .name(processConfiguration.getProcessName() + "::AliveTag")
        .controlTag(true)
        .value(Long.valueOf(timestamp))
        .quality(new SourceDataTagQuality())
        .timestamp(new Timestamp(timestamp))
        .daqTimestamp(new Timestamp(timestamp))
        .priority(DataTagAddress.PRIORITY_HIGHEST)
        .guaranteedDelivery(false)
        .valueDescription("")
         // we keep the message on the broker max twice as long as the configured alive interval
        .timeToLive(processConfiguration.getAliveInterval() * 2)
        .build();

    distributeValue(aliveTagValue);
  }

  @Override
  public void sendCommfaultTag(long tagID, String tagName, boolean value, String pDescription) {
    log.debug("Sending CommfaultTag tag {} (#{})", tagName, tagID);

    long timestamp = System.currentTimeMillis();
    SourceDataTagValue commfaultTagValue = SourceDataTagValue.builder()
        .id(tagID)
        .name(tagName)
        .controlTag(true)
        .value(value)
        .quality(new SourceDataTagQuality())
        .timestamp(new Timestamp(timestamp))
        .daqTimestamp(new Timestamp(timestamp))
        .priority(DataTagAddress.PRIORITY_HIGHEST)
        .timeToLive(DataTagConstants.TTL_FOREVER)
        .valueDescription(pDescription)
        .build();
    
    distributeValue(commfaultTagValue);
  }

  @Override
  public final void addValue(final SourceDataTagValue dataTagValue) {
    log.debug("adding data tag " + dataTagValue.getId() + " to a sending buffer");
    if (dataTagValue.getPriority() == DataTagAddress.PRIORITY_HIGH) {
      log.debug("\t sourceDataTagValue priority is HIGH");
      this.distributeValue(dataTagValue);
    } else {
      log.debug("\t sourceDataTagValue priority is LOW");
      // check whether it's message with guaranteed delivery or not
      if (dataTagValue.isGuaranteedDelivery()) {
        log.debug("\t guaranteedDelivery is TRUE");

        // note : synchrobuffer's push method is thread-safety,
        // so no external synchronization is needed
        this.persistentTagsBuffer.push(dataTagValue);
      } else {
        log.debug("\t guaranteedDelivery is FALSE");

        // note : synchrobuffer's push method is thread-safety,
        // so no external synchronization is needed
        this.dataTagsBuffer.push(dataTagValue);
      }
    }
  }

  /**
   * Connects to all the registered brokers (individual JMSSenders should
   * implement this on separate threads if the connection is unessential).
   */
  public final void connect() {
    for (JmsSender jmsSender : jmsSenders) {
      // Connection
      jmsSender.connect();
    }
  }

  /**
   * This methods gently closes and disables ProcessMessageSender's
   * synchrobuffers.
   */
  public final void closeSourceDataTagsBuffers() {
    dataTagsBuffer.disable();
    dataTagsBuffer.close();
    persistentTagsBuffer.disable();
    persistentTagsBuffer.close();
  }

  /**
   * Forwards the value to all the JMS senders.
   *
   * @param sourceDataTagValue the value to send
   * @throws JMSException if one of the senders fails
   */
  private void distributeValue(final SourceDataTagValue sourceDataTagValue) {
    for (JmsSender jmsSender : jmsSenders) {
      try {
        jmsSender.processValue(sourceDataTagValue);
      } catch (Exception e) {
        log.error("Unhandled exception caught while sending a source value (tag id " + sourceDataTagValue.getId() + ") - the value update will be lost.", e);
      }
    }
    // log value in appropriate log file
    sourceDataTagValue.log();
  }

  /**
   * Forwards the list of values to all the JMS senders.
   *
   * @throws JMSException if one of the senders throws one (individual senders
   *           should also listen to these locally to take any necessary action)
   * @param dataTagValueUpdate the values to send
   */
  private void distributeValues(final DataTagValueUpdate dataTagValueUpdate) {
    for (JmsSender jmsSender : jmsSenders) {
      try {
        jmsSender.processValues(dataTagValueUpdate);
      } catch (Exception e) {
        log.error("Unhandled exception caught while sending a collection of source values - the updates will be lost.", e);
      }
    }
    // log value in appropriate log file
    dataTagValueUpdate.log();
  }

  /**
   * Setter method.
   *
   * @param jmsSenders the jmsSenders to set
   */
  public final void setJmsSenders(final Collection<JmsSender> jmsSenders) {
    this.jmsSenders = jmsSenders;
  }

  /**
   * This class implements SynchroBuffer's SychroBufferListener, so that both
   * ProcessMessageSender's tag buffers (for persistent and non-persistent) tags
   * are able to handle Pull events.
   */
  class SynchroBufferEventsListener implements SynchroBufferListener<SourceDataTagValue> {
    
    ProcessConfiguration processConfiguration = ProcessConfigurationHolder.getInstance();

    /**
     * This method is called by SynchroBuffer, each time a PullEvent occurs.
     *
     * @param event the pull event, containing the collection of objects to be
     *          sent
     * @throws cern.c2mon.shared.util.buffer.PullException
     */
    @Override
    public void pull(PullEvent<SourceDataTagValue> event) throws PullException {
      log.trace("entering pull()..");
      log.debug("\t Number of pulled objects : {}", event.getPulled().size());

      // We add the PIK to our communication process
      DataTagValueUpdate dataTagValueUpdate = createNewDataTagValueUpdate();

      for (SourceDataTagValue sdtValue : event.getPulled()) {
        if (isMessageExpired(sdtValue)) {
          log.debug("\t pull : Discarded value update for tag id #{}, because TTL was exceeded", sdtValue.getId());
        } else {
          dataTagValueUpdate.addValue(sdtValue);
        }
        
        // check if the maximum allowed message size has been reached
        if (dataTagValueUpdate.getValues().size() >= processConfiguration.getMaxMessageSize()) {
          sendMessage(dataTagValueUpdate);
          // create new dataTagValueUpdate object
          dataTagValueUpdate = createNewDataTagValueUpdate();
        }
      }

      sendMessage(dataTagValueUpdate);

      log.trace("leaving pull method");
    }
    
    private DataTagValueUpdate createNewDataTagValueUpdate() {
      return new DataTagValueUpdate(processConfiguration.getProcessID(), processConfiguration.getprocessPIK());
    }
    
    private void sendMessage(DataTagValueUpdate dataTagValueUpdate) {
      if (!dataTagValueUpdate.getValues().isEmpty()) {
        try {
          distributeValues(dataTagValueUpdate);
          log.debug("\t sent " + dataTagValueUpdate.getValues().size() + " SourceDataTagValue objects");
        } catch (JmsException ex) {
          log.error("\t pull : JMSException caught while invoking processValues methods :" + ex.getMessage());
        }
      }
    }

    /**
     * @param sdtValue the message value that shall be checked
     * @return <code>true</code>, if the message has expired
     */
    private boolean isMessageExpired(final SourceDataTagValue sdtValue) {
      return sdtValue.getTimeToLive() != DataTagAddress.TTL_FOREVER 
          && (sdtValue.getDaqTimestamp().getTime() + sdtValue.getTimeToLive()) < System.currentTimeMillis();
    }
  }

  /**
   * Shuts down all JmsSenders.
   */
  public void shutdown() {
    jmsSenders.stream().forEach(JmsSender::shutdown);
  }
}
