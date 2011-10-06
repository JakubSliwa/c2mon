package ch.cern.tim.driver.jec;

import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;
import static junit.framework.Assert.*;

import cern.tim.driver.common.EquipmentLogger;
import ch.cern.tim.driver.jec.config.PLCConfiguration;
import ch.cern.tim.jec.StdConstants;
import ch.cern.tim.jec.TestPLCDriver;

public class SynchronizationTimerTest {

    private SynchronizationTimer synchronizationTimer;
    private PLCObjectFactory plcFactory;
    
    @Before
    public void setUp() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        EquipmentLogger equipmentLogger =  new EquipmentLogger("asd", "asd", "asd");
        PLCConfiguration plcConfiguration = new PLCConfiguration();
        plcConfiguration.setProtocol("TestPLCDriver");
        plcFactory = new PLCObjectFactory(plcConfiguration);
        synchronizationTimer = new SynchronizationTimer(equipmentLogger, plcFactory);
    }
    
    @Test
    public void testAdjustment() {
        if (new GregorianCalendar().getTimeZone().inDaylightTime(new Date())) {
            GregorianCalendar calendar = new GregorianCalendar(2010, 12, 1);
            Date winterDate = calendar.getTime();
            synchronizationTimer.testDaylightSavingTime(winterDate);
        }
        else {
            GregorianCalendar calendar = new GregorianCalendar(2010, 6, 1);
            Date summerDate = calendar.getTime();
            synchronizationTimer.testDaylightSavingTime(summerDate);
        }
        TestPLCDriver driver = (TestPLCDriver) plcFactory.getPLCDriver();
        assertEquals(driver.getLastSend().getMsgID(), StdConstants.SET_TIME_MSG);
    }
    
    @Test
    public void testNoAdjustment() {
        if (new GregorianCalendar().getTimeZone().inDaylightTime(new Date())) {
            GregorianCalendar calendar = new GregorianCalendar(2010, 6, 1);
            Date summerDate = calendar.getTime();
            synchronizationTimer.testDaylightSavingTime(summerDate);
        }
        else {
            GregorianCalendar calendar = new GregorianCalendar(2010, 12, 1);
            Date winterDate = calendar.getTime();
            synchronizationTimer.testDaylightSavingTime(winterDate);
        }
        TestPLCDriver driver = (TestPLCDriver) plcFactory.getPLCDriver();
        assertNull(driver.getLastSend());
    }
}
