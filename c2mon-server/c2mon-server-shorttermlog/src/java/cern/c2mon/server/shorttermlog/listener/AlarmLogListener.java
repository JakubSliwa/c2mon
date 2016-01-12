/******************************************************************************
 * Copyright (C) 2010-2016 CERN. All rights not expressly granted are reserved.
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
package cern.c2mon.server.shorttermlog.listener;

import java.util.ArrayList;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import cern.c2mon.server.cache.C2monCacheListener;
import cern.c2mon.server.cache.CacheRegistrationService;
import cern.c2mon.server.common.alarm.Alarm;
import cern.c2mon.server.common.component.Lifecycle;
import cern.c2mon.server.common.config.ServerConstants;
import cern.c2mon.server.shorttermlog.logger.BatchLogger;

/**
 * Listens to updates in the Alarm cache and calls the DAO
 * for logging these to the database (STL account).
 * 
 * @author Felix Ehm
 *
 */
@Service
public class AlarmLogListener implements C2monCacheListener<Alarm>, SmartLifecycle {

  /**
   * Class logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(AlarmLogListener.class);
  
  /**
   * Reference to registration service.
   */
  private CacheRegistrationService cacheRegistrationService;
  
  /**
   * Bean that logs Tags into the STL.
   */
  private BatchLogger<Alarm> tagLogger;
  
  /**
   * Listener container lifecycle hook.
   */
  private Lifecycle listenerContainer;
  
  /**
   * Lifecycle flag.
   */
  private volatile boolean running = false;
  
  /**
   * Autowired constructor.
   * 
   * @param cacheRegistrationService for registering cache listeners
   * @param tagLogger for logging cache objects to the STL
   */
  @Autowired
  public AlarmLogListener(final CacheRegistrationService cacheRegistrationService, @Qualifier("alarmLogger") final BatchLogger<Alarm> tagLogger) {
    super();
    this.cacheRegistrationService = cacheRegistrationService;
    this.tagLogger = tagLogger;
  }

  /**
   * Registers to be notified of all Tag updates (data, rule and control tags).
   */
  @PostConstruct
  public void init() {
    listenerContainer = cacheRegistrationService.registerToAlarms(this);
  }

  @Override
  public void notifyElementUpdated(Alarm cacheable) {
    ArrayList<Alarm> toSave = new ArrayList<Alarm>();
    toSave.add(cacheable);
    tagLogger.log(toSave);
  }

  @Override
  public void confirmStatus(Alarm cacheable) {
      // no confirmation required
  }
  
  @Override
  public boolean isAutoStartup() {   
    return false;
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
    LOGGER.debug("Starting Alarm logger (short-term-log)");
    running = true;
    listenerContainer.start();
  }

  @Override
  public void stop() {
    LOGGER.debug("Stopping Alarm logger (short-term-log)");
    listenerContainer.stop();
    running = false;    
  }

  @Override
  public int getPhase() {
    return ServerConstants.PHASE_STOP_LAST - 1;    
  }


 

}
