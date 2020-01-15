package cern.c2mon.server.supervision;

import cern.c2mon.cache.actions.process.ProcessService;
import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.server.common.alive.AliveTag;
import cern.c2mon.server.common.process.Process;
import cern.c2mon.shared.common.CacheEvent;
import cern.c2mon.shared.common.datatag.SourceDataTagQuality;
import cern.c2mon.shared.common.datatag.SourceDataTagValue;
import cern.c2mon.shared.common.supervision.SupervisionStatus;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import javax.inject.Inject;
import java.sql.Timestamp;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Tests for supervision logic related to alive tags and caching
 *
 * @author Alexandros Papageorgiou
 */
public class AliveTagSupervisionTest extends SupervisionCacheTest {

  @Inject
  private C2monCache<AliveTag> aliveTimerCache;

  @Inject
  private C2monCache<Process> processCache;

  @Inject
  private ProcessService processService;

  @Before
  public void initialStatusIsCorrect() {
    AliveTag aliveTimer = aliveTimerCache.get(1221L);
    assertNotNull(aliveTimer);
    assertEquals(0, aliveTimer.getLastUpdate());

    Process process = processCache.get(aliveTimer.getSupervisedId());
    assertEquals(SupervisionStatus.DOWN, process.getSupervisionStatus());
    assertNull(process.getStatusTime());
    assertNull(process.getStatusDescription());
  }

  /**
   * Tests a process alive tag is correctly processed by the SupervisionManager
   * (alive timer updated; supervision listeners notified, etc).
   *
   * Process is down at start of test, then alive is received.
   *
   * @throws InterruptedException
   */
  @Test
  @DirtiesContext
  public void testProcessAliveTag() throws InterruptedException {
    CountDownLatch latch = new CountDownLatch(1);
    supervisionNotifier.registerAsListener(supervisionEvent -> latch.countDown());

    long updateTime = System.currentTimeMillis();
    SourceDataTagValue aliveTag = createSampleAliveTag();
    aliveTag.setTimestamp(new Timestamp(updateTime));
    //process control tag
    supervisionManager.processControlTag(aliveTag);

    //check alive is updated
    AliveTag aliveTimer = aliveTimerCache.get(1221L);
    assertTrue(aliveTimer.getLastUpdate() > System.currentTimeMillis() - 10000); //account for non-synchronized

    //check process status is changed
    Process process = processCache.get(aliveTimer.getSupervisedId());
    assertEquals(SupervisionStatus.RUNNING, process.getSupervisionStatus());
    Timestamp processTime = process.getStatusTime();
    assertTrue(processTime.after(new Timestamp(updateTime - 1)));
    assertNotNull(process.getStatusDescription());

    assertTrue(latch.await(1, TimeUnit.SECONDS)); //wait for notification on listener thread
  }

  @Test
  @DirtiesContext
  public void timerCascadesProperly() {
    AliveTag aliveTimer = aliveTimerCache.get(1221L);

    long startTimer = 1;

    // Start the process
    processService.start(aliveTimer.getSupervisedId(), new Timestamp(startTimer));
    assertEquals(startTimer, aliveTimerCache.get(1221L).getLastUpdate());
  }

  /**
   * Alives older than 2 minutes are rejected.
   */
  @Test
  @DirtiesContext
  public void testRejectOldAlive() {
    //check alive timer is defined & set last update
    AliveTag aliveTimer = aliveTimerCache.get(1221L);
    aliveTimer.setLastUpdate(System.currentTimeMillis()-1000);
    long aliveTime = aliveTimer.getLastUpdate();
    aliveTimerCache.put(aliveTimer.getId(), aliveTimer);

    //send alive 2 minutes old (should be rejected)
    SourceDataTagValue value = createSampleAliveTag();
    value.setDaqTimestamp(new Timestamp(System.currentTimeMillis() - 130000));

    supervisionManager.processControlTag(value);

    //no update
    assertEquals(aliveTime, aliveTimer.getLastUpdate());
  }

  /**
   * Checks a new process alive has no effect on the state tag or on the process
   * status, since it is already up as running. Only the alive is updated.
   */
  @Test
  @DirtiesContext
  public void testProcessAliveNoEffect() throws InterruptedException {
    AliveTag aliveTimer = aliveTimerCache.get(1221L);

    long startTimer = System.currentTimeMillis();

    // Start the process
    processService.start(aliveTimer.getSupervisedId(), new Timestamp(startTimer));
    // Resume so that the status goes to RUNNING
    Process process = processService.resume(aliveTimer.getSupervisedId(), new Timestamp(System.currentTimeMillis()), "");
    assertEquals(SupervisionStatus.RUNNING, process.getSupervisionStatus());
    Timestamp originalProcessTime = process.getStatusTime();
    assertNotNull(originalProcessTime);

    // Create the next alive tag
    SourceDataTagValue newerAliveTag = createSampleAliveTag();
    long updatedTimer = startTimer + 1;
    newerAliveTag.setDaqTimestamp(new Timestamp(updatedTimer));

    // Set up a latch
    CountDownLatch latch = new CountDownLatch(1);
    aliveTimerCache.getCacheListenerManager().registerListener(at -> latch.countDown(), CacheEvent.UPDATE_ACCEPTED);

    supervisionManager.processControlTag(newerAliveTag);

    assertTrue("Alive tag cache value should be updated", latch.await(100, TimeUnit.MILLISECONDS));

    // check alive is updated with the correct timestamp
    assertEquals(updatedTimer, aliveTimerCache.get(1221L).getLastUpdate());

    //check process status is not changed & time also
    assertEquals(SupervisionStatus.RUNNING, process.getSupervisionStatus());
    assertEquals(originalProcessTime, process.getStatusTime());
  }

  private SourceDataTagValue createSampleAliveTag() {
    return new SourceDataTagValue(1221L,
      "test alive",
      true,
      0L,
      new SourceDataTagQuality(),
      new Timestamp(System.currentTimeMillis()),
      4,
      false,
      "description",
      10000);
  }
}
