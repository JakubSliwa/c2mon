package cern.c2mon.server.cache.alarm.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import cern.c2mon.server.cache.alarm.AlarmAggregatorRegistration;
import cern.c2mon.server.cache.alarm.AlarmAggregatorListener;
import cern.c2mon.server.common.alarm.Alarm;
import cern.c2mon.server.common.tag.Tag;

@Slf4j
@Component
public class AlarmAggregatorNotifier implements AlarmAggregatorRegistration {
  /**
   * List of registered listeners.
   */
  private final List<AlarmAggregatorListener> listeners = new ArrayList<>();;

  
  public void registerForTagUpdates(final AlarmAggregatorListener aggregatorListener) {
    listeners.add(aggregatorListener);
  }
  
  /**
   * Notify the listeners of a tag update with associated alarms.
   * 
   * @param tag
   *          the Tag that has been updated
   * @param alarms
   *          the associated list of evaluated alarms
   */
  public void notifyOnUpdate(final Tag tag, final List<Alarm> alarms) {
    for (AlarmAggregatorListener listener : listeners) {
      try {
        listener.notifyOnUpdate((Tag) tag.clone(), alarms);
      } catch (CloneNotSupportedException e) {
        log.error("Unexpected exception caught: clone should be implemented for this class! Alarm & tag listener was not notified: {}", listener.getClass().getSimpleName(), e);
      }
    }
  }
  
  public void notifyOnSupervisionChange(final Tag tag, final List<Alarm> alarms) {
    for (AlarmAggregatorListener listener : listeners) {
      try {
        listener.notifyOnSupervisionChange((Tag) tag.clone(), alarms);
      } catch (CloneNotSupportedException e) {
        log.error("Unexpected exception caught: clone should be implemented for this class! Alarm & tag listener was not notified: {}", listener.getClass().getSimpleName(), e);
      }
    }
  }
}