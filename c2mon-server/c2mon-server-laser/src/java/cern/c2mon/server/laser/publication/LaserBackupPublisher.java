package cern.c2mon.server.laser.publication;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.SmartLifecycle;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Service;

import cern.laser.source.alarmsysteminterface.ASIException;
import cern.laser.source.alarmsysteminterface.AlarmSystemInterface;
import cern.laser.source.alarmsysteminterface.AlarmSystemInterfaceFactory;
import cern.laser.source.alarmsysteminterface.FaultState;
import cern.tim.server.cache.AlarmCache;
import cern.tim.server.cache.exception.CacheElementNotFoundException;
import cern.tim.server.common.alarm.Alarm;
import cern.tim.server.common.config.ServerConstants;

/**
 * Sends regular backups of all active alarms to LASER.
 * 
 * @author Mark Brightwell
 * 
 */
@Service
@ManagedResource(objectName = "cern.c2mon:type=LaserPublisher,name=LaserBackupPublisher")
public class LaserBackupPublisher extends TimerTask implements SmartLifecycle {

  /**
   * Class logger.
   */
  private static final Logger LOGGER = Logger.getLogger(LaserBackupPublisher.class);

  /**
   * Time (ms) between backups.
   */
  private static final int BACKUP_INTERVAL = 60000;

  /**
   * Initial delay before sending backups (ms).
   */
  private static final int INITIAL_BACKUP_DELAY = BACKUP_INTERVAL;

  /**
   * Lock used to only allow one backup to run at any time across a server cluster.
   */
  private ReentrantReadWriteLock backupLock = new ReentrantReadWriteLock();
  
  /**
   * Flag for lifecycle calls.
   */
  private volatile boolean running = false;

  /**
   * Timer scheduling publication.
   */
  private Timer timer;

  /**
   * Ref to alarm cache.
   */
  private AlarmCache alarmCache;
  
  /**
   * Our reference to the {@link LaserPublisher} as we need it to 
   * use the {@link LaserPublisher#getSourceName()} method.<br>
   * <br>
   * This is because we want to be aligned (sourcename-wise) with the LaserPublisher instance. 
   * Otherwise we may end up sending backups with a different sourcename. 
   */
  private LaserPublisher publisher = null;
  
  /** Reference to the LASER alarm system interface. */
  private AlarmSystemInterface asi = null;
  
  /**
   * Constructor.
   * 
   * @param alarmCache ref to Alarm cache bean
   */
  @Autowired
  public LaserBackupPublisher(AlarmCache alarmCache, LaserPublisher publisher) {
    super();
    this.alarmCache = alarmCache;
    this.publisher = publisher;
  }

  @Override
  public void run() {
    //lock to only allow a single backup at a time
    backupLock.writeLock().lock();
    try {
      LOGGER.debug("Sending LASER active alarm backup.");
      List<Alarm> alarmList = new ArrayList<Alarm>();
      for (Long alarmId : alarmCache.getKeys()) {
        try {
          Alarm alarm = alarmCache.getCopy(alarmId);
          if (alarm.isActive()) {
            alarmList.add(alarm);
          }
        } catch (CacheElementNotFoundException e) {
          // should only happen if concurrent re-configuration of the server
          LOGGER.warn("Unable to locate alarm " + alarmId + " in cache during LASER backup: not included in backup.", e);
        }
      }
      LOGGER.debug("Sending LASER active alarm backup.");
      if (!alarmList.isEmpty()) {
          publishAlarmBackUp(alarmList);
      }
      LOGGER.debug("Finished sending LASER active alarm backup.");
    } catch (Exception e) {
      LOGGER.error("Exception caught while publishing active Alarm backup list", e);
    } finally {
      backupLock.writeLock().unlock();
    }
  }

  /**
   * Publishes the alarm list as backup to LASER
   * 
   * @param alarmList list of active alarms
   */
  private void publishAlarmBackUp(List<Alarm> alarmList) {
	  ArrayList<FaultState> toSend = new ArrayList<FaultState>();
	  
	  // iterate over list and transform them into Laser fault states 
	  for(Alarm timAlarm : alarmList){
		FaultState fs = null;
		
		fs = AlarmSystemInterfaceFactory.createFaultState(timAlarm.getFaultFamily(), timAlarm.getFaultMember(), timAlarm.getFaultCode());
		fs.setUserTimestamp(timAlarm.getTimestamp());
		fs.setDescriptor(timAlarm.getState());
		if (timAlarm.getInfo() != null) {
			Properties prop = null;
	        prop = fs.getUserProperties();
	        prop.put(FaultState.ASI_PREFIX_PROPERTY, timAlarm.getInfo());
	        fs.setUserProperties(prop);
	    }
		
		toSend.add(fs);
	  }
	  try {
		asi.pushActiveList(toSend);
	} catch (ASIException e) {
		LOGGER.error("Cannot create backup list : " + e.getMessage());
		e.printStackTrace();
	}
  }

  @Override
  public boolean isAutoStartup() {
    return true;
  }

  @Override
  public void stop(Runnable callback) {
    stop();
    callback.run();
  }

  @Override
  public boolean isRunning() {
    return running;
  }

  @Override
  @ManagedOperation(description="starts the backups publisher.")
  public void start() {
    LOGGER.info("Starting LASER backup mechanism.");
    try {
		asi = AlarmSystemInterfaceFactory.createSource(publisher.getSourceName());
		timer = new Timer();
	    timer.scheduleAtFixedRate(this, INITIAL_BACKUP_DELAY, BACKUP_INTERVAL);
	    running = true;
	} catch (ASIException e) {
		stop();
	}
  }

  @Override
  @ManagedOperation(description="Stops the backups publisher.")
  public void stop() {
    LOGGER.info("Stopping LASER backup mechanism.");
    timer.cancel();
    if (asi != null) { 
    	asi.close();
    }
    running = false;
  }

  @Override
  public int getPhase() {
    return ServerConstants.PHASE_START_LAST;
  }

}
