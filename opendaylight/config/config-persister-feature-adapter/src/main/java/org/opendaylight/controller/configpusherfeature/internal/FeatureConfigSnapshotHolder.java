/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configpusherfeature.internal;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Feature;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.storage.file.xml.DataEncrypter;
import org.opendaylight.controller.config.persist.storage.file.xml.model.ConfigSnapshot;

/*
 * A ConfigSnapshotHolder that can track all the additional information
 * relavent to the fact we are getting these from a Feature.
 *
 * Includes tracking the 'featureChain' - an reverse ordered list of the dependency
 * graph of features that caused us to push this FeatureConfigSnapshotHolder.
 * So if A -> B -> C, then the feature chain would be C -> B -> A
 */
public class FeatureConfigSnapshotHolder implements ConfigSnapshotHolder {
    private ConfigSnapshot unmarshalled = null;
    private ConfigFileInfo fileInfo = null;
    private List<Feature> featureChain = new ArrayList<Feature>();

    /*
     * @param holder - FeatureConfigSnapshotHolder that we
     * @param feature - new
     */
    public FeatureConfigSnapshotHolder(final FeatureConfigSnapshotHolder holder, final Feature feature) throws JAXBException {
        this(holder.fileInfo,holder.getFeature());
        this.featureChain.add(feature);
    }

    /*
     * Create a FeatureConfigSnapshotHolder for a given ConfigFileInfo and record the associated
     * feature we are creating it from.
     * @param fileInfo - ConfigFileInfo to read into the ConfigSnapshot
     * @param feature - Feature the ConfigFileInfo was attached to
     */
    public FeatureConfigSnapshotHolder(final ConfigFileInfo fileInfo, final Feature feature) throws JAXBException {
        Preconditions.checkNotNull(fileInfo);
        Preconditions.checkNotNull(fileInfo.getFinalname());
        Preconditions.checkNotNull(feature);
        this.fileInfo = fileInfo;
        this.featureChain.add(feature);
        // TODO extract utility method for umarshalling config snapshots
        JAXBContext jaxbContext = JAXBContext.newInstance(ConfigSnapshot.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();
        XMLInputFactory xif = XMLInputFactory.newFactory();
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        try {
            //XMLStreamReader xsr = xif.createXMLStreamReader(new StreamSource(new File(fileInfo.getFinalname())));
            XMLStreamReader xsr = xif.createXMLStreamReader(DataEncrypter.decryptCredentialAttributes(fileInfo.getFinalname()));
            unmarshalled = ((ConfigSnapshot) um.unmarshal(xsr));
            DataEncrypter.encryptCredentialAttributes(fileInfo.getFinalname());
        } catch (final XMLStreamException e) {
            throw new JAXBException(e);
        }
    }
    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     *
     * We really care most about the underlying ConfigShapshot, so compute hashcode on that
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((unmarshalled != null && unmarshalled.getConfigSnapshot() == null) ? 0 : unmarshalled.getConfigSnapshot().hashCode());
        return result;
    }
    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     * *
     * We really care most about the underlying ConfigShapshot, so compute equality on that
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FeatureConfigSnapshotHolder fcsh = (FeatureConfigSnapshotHolder)obj;
        if(this.unmarshalled.getConfigSnapshot().equals(fcsh.unmarshalled.getConfigSnapshot())) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        Path p = Paths.get(fileInfo.getFinalname());
        b.append(p.getFileName())
            .append("(")
            .append(getCauseFeature())
            .append(",")
            .append(getFeature())
            .append(")");
        return b.toString();
    }

    @Override
    public String getConfigSnapshot() {
        return unmarshalled.getConfigSnapshot();
    }

    @Override
    public SortedSet<String> getCapabilities() {
        return unmarshalled.getCapabilities();
    }

    public ConfigFileInfo getFileInfo() {
        return fileInfo;
    }

    /*
     * @returns The original feature to which the ConfigFileInfo was attached
     * Example:
     * A -> B -> C, ConfigFileInfo Foo is attached to C.
     * feature:install A
     * thus C is the 'Feature' Foo was attached.
     */
    public Feature getFeature() {
        return featureChain.get(0);
    }

    /*
     * @return The dependency chain of the features that caused the ConfigFileInfo to be pushed in reverse order.
     * Example:
     * A -> B -> C, ConfigFileInfo Foo is attached to C.
     * The returned list is
     * [C,B,A]
     */
    public ImmutableList<Feature> getFeatureChain() {
        return ImmutableList.copyOf(Lists.reverse(featureChain));
    }

    /*
     * @return The feature the installation of which was the root cause
     * of this pushing of the ConfigFileInfo.
     * Example:
     * A -> B -> C, ConfigFileInfo Foo is attached to C.
     * feature:install A
     * this A is the 'Cause' of the installation of Foo.
     */
    public Feature getCauseFeature() {
        return Iterables.getLast(featureChain);
    }
}
