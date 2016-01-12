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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.Timestamp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cern.c2mon.publisher.rdaAlarms.RdaAlarmsProperty.AlarmState;
import cern.c2mon.shared.client.alarm.AlarmValue;
import cern.c2mon.shared.client.alarm.AlarmValueImpl;
import cern.cmw.data.Data;
import cern.cmw.data.DataFactory;
import cern.cmw.data.Entry;
import cern.cmw.rda3.common.data.AcquiredData;

@RunWith(SpringJUnit4ClassRunner.class)
public class TestRdaAlarmsProperty extends TestBaseClass {

    private RdaAlarmsProperty prop;

    @Test
    public void testUpdateProp() {
        getLogger().info("Starting testUpdateProp() ----------------- ");

        prop = new RdaAlarmsProperty(TestBaseClass.SOURCE_ID);
        assertEquals(0, prop.get().getEntries().size());
        
        // update with AlarmValue object
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        AlarmValue av = new AlarmValueImpl(1L, 1, "FM", "FF", "Activation", 1L, ts, true);
        prop.onUpdate(av);
        av = new AlarmValueImpl(1L, 3, "FM", "FF", "Activation", 1L, ts, false);
        prop.onUpdate(av);
                        
        // check unfiltered value result, all alarms (i.e.) should be there
        assertEquals(2, prop.get().getEntries().size());
        assertEquals(AlarmState.ACTIVE.toString(), prop.get().getString(ALARM_ID));

        av = new AlarmValueImpl(1L, 1, "FM", "FF", "Activation", 1L, ts, false);
        prop.onUpdate(av);
        assertEquals(2, prop.get().getEntries().size());
        assertEquals(AlarmState.TERMINATE.toString(), prop.get().getString(ALARM_ID));
            
        // check filtered value result
        Data filter = DataFactory.createData();
        filter.append("F1", TestBaseClass.ALARM_ID);
        assertEquals(1, prop.getValue(filter).getData().getEntries().size());
        assertNotNull("F1", prop.getValue(filter).getData().getString("F1"));   // make sure the filter key is now the id!
        printProp(filter);
        getLogger().info("Completed testUpdateProp() ---------------- ");
    }
    
    private void printProp(Data filter) {
        AcquiredData data = prop.getValue(filter);
        for (Entry e : data.getData().getEntries()) {
            getLogger().info(e.getName() + " -> " + e.getString());
        }            
    }
        
}
