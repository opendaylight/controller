/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.configpusherfeature.internal;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Capability;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.ConfigInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Library;
import org.apache.karaf.features.Requirement;
import org.apache.karaf.features.Scoping;
import org.opendaylight.controller.config.persist.storage.file.xml.model.ConfigSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

/*
 * Wrap a Feature for the purposes of extracting the FeatureConfigSnapshotHolders from
 * its underlying ConfigFileInfo's
 *
 * Delegates the the contained feature and provides additional methods.
 */
public class AbstractFeatureWrapper implements Feature {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractFeatureWrapper.class);

    protected static final String CONFIG_FILE_SUFFIX = "xml";

    protected Feature feature = null;

    protected AbstractFeatureWrapper() {
        // prevent instantiation without Feature
    }

    /*
     * @param f Feature to wrap
     */
    public AbstractFeatureWrapper(final Feature f) {
        Preconditions.checkNotNull(f,"FeatureWrapper requires non-null Feature in constructor");
        this.feature = f;
    }

    /*
     * Get FeatureConfigSnapshotHolders appropriate to feed to the config subsystem
     * from the underlying Feature Config files
     */
    public Set<FeatureConfigSnapshotHolder> getFeatureConfigSnapshotHolders() throws Exception {
        final Set<FeatureConfigSnapshotHolder> snapShotHolders = new LinkedHashSet<>();
        for(final ConfigFileInfo c: getConfigurationFiles()) {
            // Skip non config snapshot XML files
            if(isConfigSnapshot(c.getFinalname())) {
                final Optional<FeatureConfigSnapshotHolder> featureConfigSnapshotHolder = getFeatureConfigSnapshotHolder(c);
                if(featureConfigSnapshotHolder.isPresent()) {
                    snapShotHolders.add(featureConfigSnapshotHolder.get());
                }
            }
        }
        return snapShotHolders;
    }

    protected Optional<FeatureConfigSnapshotHolder> getFeatureConfigSnapshotHolder(final ConfigFileInfo c) {
        try {
            return Optional.of(new FeatureConfigSnapshotHolder(c, this));
        } catch (final JAXBException e) {
            LOG.warn("Unable to parse configuration snapshot. Config from '{}' will be IGNORED. " +
                    "Note that subsequent config files may fail due to this problem. " +
                    "Xml markup in this file needs to be fixed, for detailed information see enclosed exception.",
                    c.getFinalname(), e);
        } catch (final XMLStreamException e) {
            // Files that cannot be loaded are ignored as non config subsystem files e.g. jetty.xml
            LOG.debug("Unable to read configuration file '{}'. Not a configuration snapshot",
                    c.getFinalname(), e);
        }
        return Optional.absent();
    }

    private static boolean isConfigSnapshot(String fileName) {
        if(!Files.getFileExtension(fileName).equals(CONFIG_FILE_SUFFIX)) {
            return false;
        }

        if(fileName.endsWith("jetty.xml")) {
            // Special case - ignore the jetty.xml file as it contains a DTD and causes a "Connection refused"
            // error when it tries to go out to the network to retrieve it. We don't want it trying to go out
            // to the network nor do we want an error logged trying to parse it.
            return false;
        }

        File file = new File(System.getProperty("karaf.home"), fileName);
        try(FileInputStream fis = new FileInputStream(file)) {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);
            builderFactory.setCoalescing(true);
            builderFactory.setIgnoringElementContentWhitespace(true);
            builderFactory.setIgnoringComments(true);

            Element root = builderFactory.newDocumentBuilder().parse(fis).getDocumentElement();
            return ConfigSnapshot.SNAPSHOT_ROOT_ELEMENT_NAME.equals(root.getLocalName());
        } catch (Exception e) {
            LOG.error("Could not parse XML file {}", file, e);
            return false;
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (feature == null ? 0 : feature.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AbstractFeatureWrapper other = (AbstractFeatureWrapper) obj;
        if (feature == null) {
            if (other.feature != null) {
                return false;
            }
        } else if (!feature.equals(other.feature)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return feature.getName();
    }

    @Override
    public String getId() {
        return feature.getId();
    }

    @Override
    public String getName() {
        return feature.getName();
    }

    @Override
    public String getDescription() {
        return feature.getDescription();
    }

    @Override
    public String getDetails() {
        return feature.getDetails();
    }

    @Override
    public String getVersion() {
        return feature.getVersion();
    }

    @Override
    public boolean hasVersion() {
        return feature.hasVersion();
    }

    @Override
    public String getResolver() {
        return feature.getResolver();
    }

    @Override
    public String getInstall() {
        return feature.getInstall();
    }

    @Override
    public List<Dependency> getDependencies() {
        return feature.getDependencies();
    }

    @Override
    public List<BundleInfo> getBundles() {
        return feature.getBundles();
    }

    @Override
    public List<ConfigInfo> getConfigurations() {
        return feature.getConfigurations();
    }

    @Override
    public List<ConfigFileInfo> getConfigurationFiles() {
        return feature.getConfigurationFiles();
    }

    @Override
    public List<? extends Conditional> getConditional() {
        return feature.getConditional();
    }

    @Override
    public int getStartLevel() {
        return feature.getStartLevel();
    }

    @Override
    public List<? extends Capability> getCapabilities() {
        return feature.getCapabilities();
    }

    @Override
    public List<? extends Library> getLibraries() {
        return feature.getLibraries();
    }

    @Override
    public String getNamespace() {
        return feature.getNamespace();
    }

    @Override
    public String getRepositoryUrl() {
        return feature.getRepositoryUrl();
    }

    @Override
    public List<? extends Requirement> getRequirements() {
        return feature.getRequirements();
    }

    @Override
    public List<String> getResourceRepositories() {
        return feature.getResourceRepositories();
    }

    @Override
    public Scoping getScoping() {
        return feature.getScoping();
    }

    @Override
    public boolean isHidden() {
        return feature.isHidden();
    }

}
