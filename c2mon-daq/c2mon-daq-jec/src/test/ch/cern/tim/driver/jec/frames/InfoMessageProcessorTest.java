package ch.cern.tim.driver.jec.frames;

import static org.junit.Assert.*;
import static org.easymock.classextension.EasyMock.*;

import java.io.CharArrayReader;
import java.io.IOException;

import org.apache.kahadb.util.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;

import cern.tim.driver.common.EquipmentLogger;
import cern.tim.driver.common.EquipmentLoggerFactory;
import ch.cern.tim.driver.jec.IJECFrameController;
import ch.cern.tim.driver.jec.PLCObjectFactory;
import ch.cern.tim.driver.jec.config.PLCConfiguration;
import ch.cern.tim.jec.JECIndexOutOfRangeException;
import ch.cern.tim.jec.JECPFrames;
import ch.cern.tim.jec.StdConstants;

public class InfoMessageProcessorTest {

    private InfoMessageProcessor infoMessageProcessor;
    private PLCObjectFactory plcFactory;
    private IJECFrameController frameController;
    
    @Before
    public void setUp() throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        EquipmentLogger equipmentLogger = new EquipmentLogger("asd", "asd", "asd");
        PLCConfiguration plcConfiguration = new PLCConfiguration();
        plcConfiguration.setProtocol("TestPLCDriver");
        plcFactory = new PLCObjectFactory(plcConfiguration);
        frameController = createMock(IJECFrameController.class);
        infoMessageProcessor = new InfoMessageProcessor(plcFactory, StdConstants.INFO_MSG, frameController, equipmentLogger );
    }
    
    @Test
    public void testInvalidation() throws IOException, JECIndexOutOfRangeException {
        JECPFrames frame = plcFactory.getRawRecvFrame();
        frame.SetMessageIdentifier(StdConstants.INFO_MSG);
        frame.SetDataType((byte) StdConstants.DP_SLAVE_LOST);
        frame.SetDataStartNumber((short) StdConstants.SLAVE_INVALIDATE);
        String slaveName = "slave";
        writeJECString(frame, slaveName);
        
        // expected call
        frameController.invalidateSlaveTags(slaveName);
        
        replay(frameController);
        infoMessageProcessor.processJECPFrame(frame);
        verify(frameController);
    }
    
    @Test
    public void testValidation() throws IOException, JECIndexOutOfRangeException {
        JECPFrames frame = plcFactory.getRawRecvFrame();
        frame.SetMessageIdentifier(StdConstants.INFO_MSG);
        frame.SetDataType((byte) StdConstants.DP_SLAVE_LOST);
        frame.SetDataStartNumber((short) StdConstants.SLAVE_VALIDATE);
        String slaveName = "slave";
        writeJECString(frame, slaveName);
        
        // expected call
        frameController.revalidateSlaveTags(slaveName);
        
        replay(frameController);
        infoMessageProcessor.processJECPFrame(frame);
        verify(frameController);
    }
    
    @Test
    public void testERROR() throws IOException, JECIndexOutOfRangeException {
        JECPFrames frame = plcFactory.getRawRecvFrame();
        frame.SetMessageIdentifier(StdConstants.INFO_MSG);
        frame.SetDataType((byte) StdConstants.DP_SLAVE_LOST);
        frame.SetDataStartNumber((short) StdConstants.SLAVE_VALIDATE);
        String slaveName = "ERROR";
        writeJECString(frame, slaveName);
        
        // nothing should happen
        
        replay(frameController);
        infoMessageProcessor.processJECPFrame(frame);
        verify(frameController);
    }

    private void writeJECString(JECPFrames frame, String slaveName) throws IOException, JECIndexOutOfRangeException {
        CharArrayReader reader = new CharArrayReader(slaveName.toCharArray());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int curChar;
        while ((curChar = reader.read()) != -1) {
            out.write(curChar);
        }
        out.write(StdConstants.END_OF_TEXT);
        frame.AddJECData(out.toByteArray(), 0, slaveName.length() + 1);
    }
}
