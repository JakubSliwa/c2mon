package cern.c2mon.cache.commfault;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import cern.c2mon.cache.AbstractCacheLoaderTest;
import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.server.cache.dbaccess.CommFaultTagMapper;
import cern.c2mon.server.common.commfault.CommFaultTag;
import cern.c2mon.server.common.commfault.CommFaultTagCacheObject;

import static org.junit.Assert.*;

/**
 * @author Szymon Halastra
 */
public class CommfaultCacheLoaderTest extends AbstractCacheLoaderTest {

  @Autowired
  private C2monCache<Long, CommFaultTag> commFaultTagCacheRef;

  @Autowired
  private CommFaultTagMapper commFaultTagMapper;

  @Before
  public void init() {
    commFaultTagCacheRef.init();
  }

  @Test
  public void preloadCache() {
    assertNotNull("Commfault Cache should not be null", commFaultTagCacheRef);

    List<CommFaultTag> commFaultList = commFaultTagMapper.getAll();

    Set<Long> keySet = commFaultList.stream().map(CommFaultTag::getId).collect(Collectors.toSet());
    assertTrue("List of alarms should not be empty", commFaultList.size() > 0);

    assertEquals("Size of cache and DB mapping should be equal", commFaultList.size(), commFaultTagCacheRef.getKeys().size());
    //compare all the objects from the cache and buffer
    Iterator<CommFaultTag> it = commFaultList.iterator();
    while (it.hasNext()) {
      CommFaultTagCacheObject currentTag = (CommFaultTagCacheObject) it.next();
      //equality of DataTagCacheObjects => currently only compares names
      assertEquals("Cached CommfaultTag should have the same name as in DB",
              currentTag.getEquipmentId(), ((commFaultTagCacheRef.get(currentTag.getId())).getEquipmentId()));
    }
  }
}