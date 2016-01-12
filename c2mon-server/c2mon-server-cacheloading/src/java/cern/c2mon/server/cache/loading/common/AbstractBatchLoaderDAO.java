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
package cern.c2mon.server.cache.loading.common;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.c2mon.server.cache.dbaccess.BatchLoaderMapper;
import cern.c2mon.server.cache.dbaccess.structure.DBBatch;
import cern.c2mon.server.cache.loading.BatchCacheLoaderDAO;
import cern.c2mon.shared.common.Cacheable;

/**
 * Abstract class implementing {@link BatchCacheLoaderDAO}, providing common DAO methods
 * for loading caches on many threads.
 *  
 * @author Mark Brightwell
 * @param <T> the type of the cache object
 *
 */
public abstract class AbstractBatchLoaderDAO<T extends Cacheable> extends AbstractSimpleLoaderDAO<T> implements BatchCacheLoaderDAO<T> {

  private static Logger LOGGER = LoggerFactory.getLogger(AbstractBatchLoaderDAO.class);
  
  /**
   * Mapper required for batch loading.
   */
  private BatchLoaderMapper<T> batchLoaderMapper;
  
  /**
   * Constructor.
   * @param batchLoaderMapper required mapper
   */
  public AbstractBatchLoaderDAO(final BatchLoaderMapper<T> batchLoaderMapper) {
    super(batchLoaderMapper);
    this.batchLoaderMapper = batchLoaderMapper;
  }
  
  @Override
  public Long getMinId() {
    Long minId = batchLoaderMapper.getMinId();
    if (minId != null) {
      return minId;
    } else {
      return 0L;
    }    
  }
  
  @Override
  public Long getMaxId() {
    Long maxId = batchLoaderMapper.getMaxId();
    if (maxId != null) {
      return maxId;
    } else {
      return 0L;
    }
  }

  @Override
  //@Transactional("cacheTransactionManager")
  public Map<Object, T> getBatchAsMap(Long firstId, Long lastId) {
    DBBatch dbBatch = new DBBatch(firstId, lastId);
    List<T> cacheableList = batchLoaderMapper.getRowBatch(dbBatch);
    Map<Object, T> returnMap = new ConcurrentHashMap<Object, T>((int) (lastId - firstId));
    Iterator<T> it = cacheableList.iterator();
    T current;
    while (it.hasNext()) {
      current = it.next();
      if (current != null){
        returnMap.put(current.getId(), doPostDbLoading(current));
      } else {
        LOGGER.warn("Null value retrieved from DB by Mapper " + batchLoaderMapper.getClass().getSimpleName());
      }      
    }
    return returnMap;
    
  }
  
}
