/******************************************************************************
 * Copyright (C) 2010-2019 CERN. All rights not expressly granted are reserved.
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
package cern.c2mon.server.common.alarm;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;

import cern.c2mon.shared.util.parser.SimpleXMLParser;

import static org.junit.Assert.assertEquals;

/**
 * Unit test of Range alarm condition implementation.
 *
 * @author Matthias Braeger
 *
 */
public class RangeAlarmConditionTest {

  @Test
  public void testRangeInteger() throws ParserConfigurationException {
    RangeAlarmCondition<Integer> rangeAlarmCondition = new RangeAlarmCondition<>(0, 100);
    assertEquals(true, rangeAlarmCondition.evaluateState(5));
    assertEquals(true, rangeAlarmCondition.evaluateState(0));
    assertEquals(true, rangeAlarmCondition.evaluateState(100));

    assertEquals(true, rangeAlarmCondition.evaluateState(89.5f));
    assertEquals(false, rangeAlarmCondition.evaluateState(-10));
    assertEquals(false, rangeAlarmCondition.evaluateState(150));
  }

  @Test
  public void testOnlyMinRangeSet() throws ParserConfigurationException {
    RangeAlarmCondition<Integer> rangeAlarmCondition = new RangeAlarmCondition<>(0, null);
    assertEquals(true, rangeAlarmCondition.evaluateState(5));
    assertEquals(true, rangeAlarmCondition.evaluateState(0));
    assertEquals(true, rangeAlarmCondition.evaluateState(100));

    assertEquals(true, rangeAlarmCondition.evaluateState(89.5f));
    assertEquals(true, rangeAlarmCondition.evaluateState("89.5"));
    assertEquals(true, rangeAlarmCondition.evaluateState(true)); // true == 1

    assertEquals(false, rangeAlarmCondition.evaluateState(-10));
    assertEquals(true, rangeAlarmCondition.evaluateState(150));
  }

  @Test
  public void testOnlyMaxRangeSet() throws ParserConfigurationException {
    RangeAlarmCondition<Integer> rangeAlarmCondition = new RangeAlarmCondition<>(null, 100);
    assertEquals(true, rangeAlarmCondition.evaluateState(5));
    assertEquals(true, rangeAlarmCondition.evaluateState(0));
    assertEquals(true, rangeAlarmCondition.evaluateState(100));

    assertEquals(true, rangeAlarmCondition.evaluateState(89.5f));

    assertEquals(true, rangeAlarmCondition.evaluateState(-10));
    assertEquals(false, rangeAlarmCondition.evaluateState(150));
  }

  @Test
  public void testRangeFloat() throws ParserConfigurationException {
    RangeAlarmCondition<Float> rangeAlarmCondition = new RangeAlarmCondition<>(0f, 100f);
    checkFloatConditions(rangeAlarmCondition);
  }

  @Test
  public void testXmlDeserialization() throws ParserConfigurationException {
    RangeAlarmCondition<Float> rangeAlarmCondition = new RangeAlarmCondition<>(0f, 100f);
    String xmlString = rangeAlarmCondition.toConfigXML();

    SimpleXMLParser parser = new SimpleXMLParser();
    Document document = parser.parse(xmlString);
    AlarmCondition condition = AlarmCondition.fromConfigXML(document.getDocumentElement());

    checkFloatConditions(condition);
  }

  private void checkFloatConditions(AlarmCondition rangeAlarmCondition) {
    assertEquals(true, rangeAlarmCondition.evaluateState(5f));
    assertEquals(true, rangeAlarmCondition.evaluateState(0f));
    assertEquals(true, rangeAlarmCondition.evaluateState(99.9f));
    assertEquals(true, rangeAlarmCondition.evaluateState(100f));

    assertEquals(true, rangeAlarmCondition.evaluateState(89));

    assertEquals(false, rangeAlarmCondition.evaluateState(-10f));
    assertEquals(false, rangeAlarmCondition.evaluateState(150f));
  }
}
