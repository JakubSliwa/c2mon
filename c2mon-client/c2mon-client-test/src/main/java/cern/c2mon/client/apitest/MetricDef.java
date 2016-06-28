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
package cern.c2mon.client.apitest;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class MetricDef {

    private Long metricTagId;
    private Long ruleTagId;
    private String name;
    private String displayName;
    private String type;
    private Integer testId;
    private String description;

    public long getMetricTagId() {
        return metricTagId == null ? 0 : metricTagId;
    }

    public long getRuleTagId() {
        return ruleTagId == null ? 0 : ruleTagId;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getType() {
        return type;
    }

    public int getTestId() {
        return testId;
    }

    public String getDescription() {
        return description;
    }

    public MetricDef(final Long metricTagId, final Long equipmentRuleTag, final String metricName,
            final String displayName, final String type, final Integer testId, final String description) {
        this.metricTagId = metricTagId;
        this.ruleTagId = equipmentRuleTag;
        this.name = metricName;
        this.displayName = displayName;
        this.type = type;
        this.testId = testId;
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof MetricDef))
            return false;

        MetricDef other = (MetricDef) obj;
        return new EqualsBuilder().append(this.metricTagId, other.metricTagId).append(this.ruleTagId, other.ruleTagId)
                .append(this.name, other.name).append(this.displayName, other.displayName)
                .append(this.type, other.type).append(this.testId, other.testId).append(this.description,
                        other.description).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(3, 21).append(this.metricTagId).append(this.ruleTagId).append(this.metricTagId)
                .append(testId).hashCode();
    }
}