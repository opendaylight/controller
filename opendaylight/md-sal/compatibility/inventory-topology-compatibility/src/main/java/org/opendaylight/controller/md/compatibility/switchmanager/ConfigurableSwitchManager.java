/**
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.md.compatibility.switchmanager;

import java.util.List;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.SpanConfig;
import org.opendaylight.controller.switchmanager.SubnetConfig;
import org.opendaylight.controller.switchmanager.SwitchConfig;

/**
 * These methods should be backed by config subsystem.
 */
public abstract class ConfigurableSwitchManager implements ISwitchManager {
    @Override
    public Status saveSwitchConfig() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public Status removeSpanConfig(final SpanConfig cfgObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public Status addSubnet(final SubnetConfig configObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public final Status addSpanConfig(final SpanConfig configObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public final List<SpanConfig> getSpanConfigList() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public final void updateSwitchConfig(final SwitchConfig cfgObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public final Status updateNodeConfig(final SwitchConfig switchConfig) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public final SubnetConfig getSubnetConfig(final String subnet) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public final Status removeNodeConfig(final String nodeId) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public final Status removeSubnet(final SubnetConfig configObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public final List<SubnetConfig> getSubnetsConfigList() {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public final SwitchConfig getSwitchConfig(final String nodeId) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }

    @Override
    public Status modifySubnet(final SubnetConfig configObject) {
        throw new UnsupportedOperationException("TODO: auto-generated method stub");
    }
}
