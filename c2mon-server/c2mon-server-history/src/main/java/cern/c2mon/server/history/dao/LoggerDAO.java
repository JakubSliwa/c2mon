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
package cern.c2mon.server.history.dao;

import cern.c2mon.pmanager.IDBPersistenceHandler;
import cern.c2mon.pmanager.IFallback;
import cern.c2mon.pmanager.persistence.exception.IDBPersistenceException;
import cern.c2mon.server.history.mapper.LoggerMapper;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Common DAO implementation for objects that need storing in a history table using
 * the fallback mechanism.
 *
 * @author Mark Brightwell
 *
 * @param <T>
 *          the object that is being logged in the history table
 */
public class LoggerDAO<T extends IFallback> implements IDBPersistenceHandler<T> {

  /**
   * Private class logger.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggerDAO.class);

  /**
   * Maximum number of statements that will be executed in each SQL batch
   **/
  private static final int RECORDS_PER_BATCH = 500;

  /**
   * The iBatis factory used to acquire database sessions.
   */
  private SqlSessionFactory sqlSessionFactory;

  /**
   * Auto-commit mapper for single queries.
   */
  // private LoggerMapper<T> loggerMapper;

  /**
   * The mapper class name used for creating the batch logger mapper from the
   * session.
   */
  private Class<? extends LoggerMapper<T>> mapperInterface;

  /**
   * Information in order to identify the DB.
   */
  private String dbUrl;

  /**
   *
   * @param sqlSessionFactory
   *          the auto-commit mapper for single queries/inserts
   * @param dbUrl
   *          only used for logging error messages
   * @throws ClassNotFoundException
   */
  public LoggerDAO(SqlSessionFactory sqlSessionFactory, String mapperInterface, String dbUrl) throws ClassNotFoundException {
    super();
    this.sqlSessionFactory = sqlSessionFactory;
    Class<?> tmpInterface = Class.forName(mapperInterface);
    if (LoggerMapper.class.isAssignableFrom(tmpInterface)) {
      this.mapperInterface = (Class<? extends LoggerMapper<T>>) tmpInterface;
    } else {
      throw new IllegalArgumentException("Unexpected class name passed to LoggerDAO constructor - unable to instantiate.");
    }
    this.dbUrl = dbUrl;
    // if (tmpInterface instanceof LoggerMapper) {
    // Class<? extends LoggerMapper> mappper = tmpInterface;
    // }
  }

  /**
   * Inserts into the database a set of rows containing the data coming in
   * several IFallback objects
   *
   * @param data
   *          List of IFallback object whose data has to be inserted in the DB
   * @throws IDBPersistenceException
   *           An exception is thrown in case an error occurs during the data
   *           insertion. The exception provides also the number of already
   *           committed objects
   */
  @SuppressWarnings("unchecked")
  // add generics to persistence manager
  public final void storeData(final List data) throws IDBPersistenceException {
    SqlSession session = null;
    int size = data.size();
    int commited = 0;
    T tag;

    try {
      // We use batch set of statements to improve performance
      session = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Obtained batch transacted SQL session (session: " + session.toString() + ")");
      }
      LoggerMapper<T> persistenceMapper = session.getMapper(mapperInterface);

      // Iterate through the list of DataTagCacheObjects to insert
      // them one by one
      for (int i = 0; i != size; i++) {
        if ((0 == i % RECORDS_PER_BATCH) && i > 0) {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("storeData([Collection]) : Commiting rows for i=" + i);
          }
          session.commit();
          commited = i;
        }

        if (data.get(i) != null) {
          tag = (T) data.get(i);
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Logging object with ID: " + tag.getId());
          }
          persistenceMapper.insertLog(tag);
        }
      }
      // Commit the transaction
      session.commit();
      commited = size;
    } catch (PersistenceException e) {
      LOGGER.error("storeData([Collection]) : Error executing/closing prepared statement for " + data.size() + " dataTags", e);
      try {
        if (session != null) {
          session.rollback();
        }
      } catch (Exception sql) {
        LOGGER.error("storeData([Collection]) : Error rolling back transaction.", sql);
      }
      throw new IDBPersistenceException(e.getMessage(), commited);
    } finally {
      try {
        if (session != null) {
          session.close();
        }
      } catch (Exception e) {
        LOGGER.error("storeData([Collection]) : Error closing session.", e);
      }
    }
  }

  @Override
  public String getDBInfo() {
    return "C2MON history account on DB with URL: " + dbUrl;
  }

  @Override
  public void storeData(IFallback object) throws IDBPersistenceException {
    SqlSession session = null;
    try {
      session = sqlSessionFactory.openSession();
      LoggerMapper<T> loggerMapper = session.getMapper(mapperInterface);
      loggerMapper.insertLog((T) object);
      session.commit();
    } catch (PersistenceException ex1) {
      String message = "Exception caught while persisting an object to the history";
      LOGGER.error(message, ex1);
      if (session != null)
        session.rollback();
      throw new IDBPersistenceException(message, ex1);
    } finally {
      if (session != null)
        session.close();
    }
  }

}
