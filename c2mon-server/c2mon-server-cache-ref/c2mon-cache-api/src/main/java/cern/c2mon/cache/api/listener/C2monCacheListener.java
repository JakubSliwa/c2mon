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
package cern.c2mon.cache.api.listener;

import cern.c2mon.shared.common.Cacheable;


/**
 * Interface that must be implemented by modules that which to be registered as
 * cache listeners. The listener must then be registered with the cache using
 * one of the registration methods provided on the cache object.
 * 
 * @author Mark Brightwell
 * @param <T> the type the listener expects
 */
public interface C2monCacheListener<T extends Cacheable> {
  
  /**
   * Callback when a cache object is modified. The passed object
   * can be queried but should in general not be modified. 
   * 
   * @param cacheable the object in the cache that has been updated
   * 
   */
  void notifyElementUpdated(T cacheable);
  
  /**
   * Callback used for confirming the value of the cache object. This is
   * used in particular during a system recovery after a crash. Guaranteed
   * actions should be performed, however the passed cache object
   * will often be the same as the previous notifyElementUpdated call.
   * 
   * <p>WARNING: listeners that react to supervision changes should leave this
   * method empty, since the passed object will not contain the latest supervision
   * status, which would be overwritten by this call. After a server crash,
   * these listeners will get a callback on the supervision change interface
   * CacheSupervisionListener, which will allow them to refresh the tags *with*
   * the current status applied.
   * 
   * @param cacheable copy of cache object 
   */
  void confirmStatus(T cacheable);
  
}
