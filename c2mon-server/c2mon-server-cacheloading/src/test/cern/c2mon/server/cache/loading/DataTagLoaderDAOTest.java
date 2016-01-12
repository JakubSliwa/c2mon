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
package cern.c2mon.server.cache.loading;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cern.c2mon.server.common.datatag.DataTag;

@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
@ContextConfiguration({"classpath:cern/c2mon/server/cache/loading/config/server-cacheloading-datatag-test.xml"})
public class DataTagLoaderDAOTest {

  @Autowired
  private DataTagLoaderDAO dataTagLoaderDAO;

  @Test
  public void testGetItem() {
    assertNotNull(dataTagLoaderDAO.getItem(200000L));
  }

  /**
   * Check the default property is picked up (should override that set in the
   * cache object itself).
   */
  @Test
  public void testGetItemDoPostDbLoading() {
    DataTag tag = dataTagLoaderDAO.getItem(200010L);
    assertNotNull(tag);
    assertTopicSetCorrectly(tag);
  }

  @Test
  public void testGetBatch() {
    assertNotNull(dataTagLoaderDAO.getBatchAsMap(0L, 500000L));
    assertTrue(dataTagLoaderDAO.getBatchAsMap(200000L, 200010L).size() > 6);
  }

  @Test
  public void testGetBatchDoPostDbLoading() {
    for (Map.Entry<Object, DataTag> entry : dataTagLoaderDAO.getBatchAsMap(0L, 500000L).entrySet()) {
      assertTopicSetCorrectly(entry.getValue());
    }
  }

  private void assertTopicSetCorrectly(DataTag tag) {
    assertEquals("c2mon.client.tag" + "." + tag.getProcessId(), tag.getTopic());
  }

}
