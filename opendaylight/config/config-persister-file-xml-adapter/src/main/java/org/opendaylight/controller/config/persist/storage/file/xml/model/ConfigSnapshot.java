/*
 * Copyright (c) 2013 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.config.persist.storage.file.xml.model;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;

@XmlRootElement(name = ConfigSnapshot.SNAPSHOT_ROOT_ELEMENT_NAME)
public class ConfigSnapshot {

    public static final String SNAPSHOT_ROOT_ELEMENT_NAME = "snapshot";

    private String configXml;
    private SortedSet<String> capabilities = new TreeSet<>();
    private Set<String> features = new HashSet<>();

    ConfigSnapshot(final String configXml, final SortedSet<String> capabilities) {
        this.configXml = configXml;
        this.capabilities = capabilities;
    }

    ConfigSnapshot(final String configXml, final SortedSet<String> capabilities, final Set<String> features) {
        this.configXml = configXml;
        this.capabilities = capabilities;
        this.features = features;
    }

    ConfigSnapshot() {
    }

    public static ConfigSnapshot fromConfigSnapshot(final ConfigSnapshotHolder cfg) {
        return new ConfigSnapshot(cfg.getConfigSnapshot(), cfg.getCapabilities());
    }

    public static ConfigSnapshot fromConfigSnapshot(final ConfigSnapshotHolder cfg, final Set<String> features) {
        return new ConfigSnapshot(cfg.getConfigSnapshot(), cfg.getCapabilities(), features);
    }

    @XmlAnyElement(SnapshotHandler.class)
    public String getConfigSnapshot() {
        return configXml;
    }

    public void setConfigSnapshot(final String configSnapshot) {
        this.configXml = configSnapshot;
    }

    @XmlElement(name = "capability")
    @XmlElementWrapper(name = "required-capabilities")
    @XmlJavaTypeAdapter(value=StringTrimAdapter.class)
    public SortedSet<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(final SortedSet<String> capabilities) {
        this.capabilities = capabilities;
    }

    @XmlElement(name = "feature")
    @XmlElementWrapper(name = "features")
    @XmlJavaTypeAdapter(value=StringTrimAdapter.class)
    public Set<String> getFeatures() {
        return features;
    }

    public void setFeatures(final Set<String> features) {
        this.features = features;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ConfigSnapshot{");
        sb.append("configSnapshot='").append(configXml).append('\'');
        sb.append(", capabilities=").append(capabilities);
        sb.append(", features=").append(features);
        sb.append('}');
        return sb.toString();
    }
}

