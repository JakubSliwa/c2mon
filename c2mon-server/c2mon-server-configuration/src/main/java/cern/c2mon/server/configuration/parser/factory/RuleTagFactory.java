/*******************************************************************************
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
 ******************************************************************************/

package cern.c2mon.server.configuration.parser.factory;

import cern.c2mon.cache.api.C2monCache;
import cern.c2mon.cache.config.tag.UnifiedTagCacheFacade;
import cern.c2mon.server.cache.loading.SequenceDAO;
import cern.c2mon.server.common.tag.Tag;
import cern.c2mon.server.configuration.parser.exception.ConfigurationParseException;
import cern.c2mon.shared.client.configuration.ConfigConstants;
import cern.c2mon.shared.client.configuration.ConfigurationElement;
import cern.c2mon.shared.client.configuration.api.tag.RuleTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static cern.c2mon.cache.config.ClientQueryProvider.queryByClientInput;

/**
 * @author Franz Ritter
 */
@Service
public class RuleTagFactory extends EntityFactory<RuleTag> {
  private final C2monCache<cern.c2mon.server.common.rule.RuleTag> ruleTagCache;
  private final UnifiedTagCacheFacade tagFacadeGateway;
  private final SequenceDAO sequenceDAO;

  @Autowired
  public RuleTagFactory(C2monCache<cern.c2mon.server.common.rule.RuleTag> ruleTagCache, UnifiedTagCacheFacade tagFacadeGateway, SequenceDAO sequenceDAO) {
    super(ruleTagCache);
    this.ruleTagCache = ruleTagCache;
    this.tagFacadeGateway = tagFacadeGateway;
    this.sequenceDAO = sequenceDAO;
  }

  @Override
  public List<ConfigurationElement> createInstance(RuleTag configurationEntity) {
    return Collections.singletonList(doCreateInstance(configurationEntity));
  }

  @Override
  Long createId(RuleTag configurationEntity) {
    if (configurationEntity.getName() != null
      && !queryByClientInput(ruleTagCache, Tag::getName, configurationEntity.getName()).isEmpty()) {
        throw new ConfigurationParseException("Error creating rule tag " + configurationEntity.getName() + ": " +
            "Name already exists!");
    } else {
      return configurationEntity.getId() != null ? configurationEntity.getId() : sequenceDAO.getNextTagId();
    }
  }

  @Override
  Long getId(RuleTag configurationEntity) {
    return configurationEntity.getId() != null
      ? configurationEntity.getId()
      : queryByClientInput(ruleTagCache, Tag::getName, configurationEntity.getName())
        .stream().findAny()
        .orElseThrow(() -> new ConfigurationParseException("Data tag " + configurationEntity.getName() + " does not exist!"))
        .getId();
  }

  @Override
  boolean hasEntity(Long id) {
    return id != null && tagFacadeGateway.containsKey(id);
  }

  @Override
  ConfigConstants.Entity getEntity() {
    return ConfigConstants.Entity.RULETAG;
  }
}
