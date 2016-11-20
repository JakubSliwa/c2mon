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
package cern.c2mon.client.core.config;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import cern.c2mon.shared.util.jms.ActiveJmsSender;
import cern.c2mon.shared.util.jms.JmsSender;

/**
 * @author Justin Lewis Salmon
 */
@Configuration
public class JmsConfig {

  @Autowired
  private C2monClientProperties properties;

  @Bean
  public ActiveMQConnectionFactory jmsConnectionFactory() {
    return new ActiveMQConnectionFactory(properties.getJms().getUrl());
  }

  @Bean
  public JmsSender jmsSender(JmsTemplate jmsTemplate) {
    ActiveJmsSender jmsSender = new ActiveJmsSender();
    jmsSender.setJmsTemplate(jmsTemplate);
    return jmsSender;
  }

  @Bean
  @ConditionalOnMissingBean(JmsTemplate.class)
  public JmsTemplate jmsTemplate() {
    return new JmsTemplate(jmsConnectionFactory());
  }
}
