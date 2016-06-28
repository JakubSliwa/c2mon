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
package cern.c2mon.server.cachepersistence;

import cern.c2mon.server.cache.dbaccess.RuleTagMapper;
import cern.c2mon.server.cache.rule.RuleTagCacheImpl;
import cern.c2mon.server.cachepersistence.junit.DatabasePopulationRule;
import cern.c2mon.server.common.rule.RuleTag;
import cern.c2mon.server.common.rule.RuleTagCacheObject;
import cern.c2mon.server.test.CacheObjectCreation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration test of the cache-persistence and cache
 * modules. Test the correct persistence of updates to
 * the RuleTag cache.
 * 
 * @author Mark Brightwell
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({
    "classpath:config/server-cache.xml",
    "classpath:config/server-cachedbaccess.xml",
    "classpath:config/server-cachepersistence.xml",
    "classpath:test-config/server-test-properties.xml"
})
@TestPropertySource("classpath:c2mon-server-default.properties")
public class RuleTagCachePersistenceTest implements ApplicationContextAware {

  @Rule
  @Autowired
  public DatabasePopulationRule databasePopulationRule;
  
  /**
   * Need context to explicitly start it (for cache listener lifecycle).
   */
  private ApplicationContext context;
  
  @Autowired
  private RuleTagMapper ruleTagMapper;
  
  @Autowired
  private RuleTagCacheImpl ruleTagCache;
  
  private RuleTag originalObject;

  @Before
  public void insertTestTag() throws IOException {
    originalObject = CacheObjectCreation.createTestRuleTag();
    ruleTagMapper.insertRuleTag((RuleTagCacheObject) originalObject);
    startContext();
  }
  
  public void startContext() {
    ((AbstractApplicationContext) context).start();
  }
  
  /**
   * Tests the functionality: put value in cache -> persist to DB.
   */
  @Test
  public void testRuleTagPersistence() {
    
    ruleTagCache.put(originalObject.getId(), originalObject);
    
    //check it is in cache (only values so far...)
    
    RuleTagCacheObject cacheObject = (RuleTagCacheObject) ruleTagCache.get(originalObject.getId());
    assertEquals(((RuleTag) ruleTagCache.get(originalObject.getId())).getValue(), originalObject.getValue());    
    //check it is in database (only values so far...)
    RuleTagCacheObject objectInDB = (RuleTagCacheObject) ruleTagMapper.getItem(originalObject.getId());
    assertNotNull(objectInDB);
    assertEquals(objectInDB.getValue(), originalObject.getValue());
    assertEquals(new Integer(1000), objectInDB.getValue()); //value is 1000 for test rule tag
    
    //now update the cache object to new value
    cacheObject.setValue(new Integer(2000));
    //notify the listeners
    ruleTagCache.notifyListenersOfUpdate(cacheObject);
    
    //...and check the DB was updated after the buffer has time to fire
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    objectInDB = (RuleTagCacheObject) ruleTagMapper.getItem(originalObject.getId());
    assertNotNull(objectInDB);    
    assertEquals(Integer.valueOf(2000), objectInDB.getValue());
    
    //clean up...
    //remove from cache
    ruleTagCache.remove(originalObject.getId());
  
  }
  
  /**
   * Set the application context. Used for explicit start.
   */
  @Override
  public void setApplicationContext(ApplicationContext arg0) throws BeansException {
    context = arg0;
  }
  
}