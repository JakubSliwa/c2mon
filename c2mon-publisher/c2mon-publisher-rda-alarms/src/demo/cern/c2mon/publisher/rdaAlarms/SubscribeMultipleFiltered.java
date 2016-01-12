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
package cern.c2mon.publisher.rdaAlarms;
/**
 * Copyright (c) 2014 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */


import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;

import cern.japc.AcquiredParameterValue;
import cern.japc.MapParameterValue;
import cern.japc.Parameter;
import cern.japc.ParameterException;
import cern.japc.ParameterValueListener;
import cern.japc.Selector;
import cern.japc.SimpleParameterValue;
import cern.japc.SubscriptionHandle;
import cern.japc.factory.MapParameterValueFactory;
import cern.japc.factory.ParameterFactory;
import cern.japc.factory.SelectorFactory;
import cern.japc.factory.SimpleParameterValueFactory;


/**
 * Demonstrates how to listen to alarms coming from different alarm sources.
 *
 * @author mbuttner 
 */
public class SubscribeMultipleFiltered implements ParameterValueListener
{    
    private static Log log;
    
    private static String alarmId_1 = "PSBS:DE1.STP26:1000";
    private static String sourceId_1 = "CPS";

    //
    // --- MAIN -----------------------------------------------------------------------------------
    //
    public static void main(String[] args)
    {        
        System.setProperty("app.name", "japc-ext-laser DemoMonitor");
        System.setProperty("app.version", "0.0.1");
        
        // standard logging setup stuff
        String log4jConfigFile = System.getProperty("log4j.configuration", "log4j.properties");
        PropertyConfigurator.configureAndWatch(log4jConfigFile, 60 * 1000);   
        log = LogFactory.getLog(SubscribeMultipleFiltered.class);
        log.info("Starting " + SubscribeMultipleFiltered.class.getName() + " ...");
        
        // create the filter parameter: This is a list of pairs internal alarm id / alarm sys alarm id
        Map<String, SimpleParameterValue> psbFilterParams = new HashMap<String, SimpleParameterValue>();        
        psbFilterParams.put(sourceId_1, SimpleParameterValueFactory.newValue(alarmId_1));        
        MapParameterValue psbFilter = SimpleParameterValueFactory.newValue(psbFilterParams);
        Selector psbSelector = SelectorFactory.newSelector("5000", psbFilter /*, false*/);           

        Map<String, SimpleParameterValue> leiFilterParams = new HashMap<String, SimpleParameterValue>();        
        leiFilterParams.put("LEI", SimpleParameterValueFactory.newValue("XENERICSAMPLER_2106:ER.SCBSYNTH:4000"));        
        MapParameterValue leiFilter = MapParameterValueFactory.newValue(leiFilterParams);
        Selector leiSelector = SelectorFactory.newSelector("5000", leiFilter /*, false*/);           
        
        try
        {
            SubscribeMultipleFiltered mon = new SubscribeMultipleFiltered();
            Parameter p1 = ParameterFactory.newInstance().newParameter("rda3:///DMN.RDA.ALARMS/" + sourceId_1);
            SubscriptionHandle sh1 = p1.createSubscription(psbSelector, mon);            
            sh1.startMonitoring();            

            Parameter p2 = ParameterFactory.newInstance().newParameter("rda3:///DMN.RDA.ALARMS/LEI");
            SubscriptionHandle sh2 = p2.createSubscription(leiSelector, mon);            
            sh2.startMonitoring();            
                        
            Thread.sleep(2 * 60 * 1000); // let's run it for 2 minutes.

            sh1.stopMonitoring();
            sh2.stopMonitoring();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        log.info(SubscribeMultipleFiltered.class.getName()  + " completed");
        System.exit(0);
    }

    @Override
    public void exceptionOccured(String parameterName, String description, ParameterException pe)
    {
        pe.printStackTrace();
    }

    @Override
    public void valueReceived(String parameterName, AcquiredParameterValue avalue)
    {
        try
        {
            String value = avalue.getValue().toString();
            log.info("Value received for " + parameterName + ": " + value.trim());
        }
        catch (Exception e)
        {
            log.error("Failed to retrieve value for " + parameterName, e);            
        }
    }


}
