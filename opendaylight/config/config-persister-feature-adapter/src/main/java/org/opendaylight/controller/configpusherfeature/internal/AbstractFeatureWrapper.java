package org.opendaylight.controller.configpusherfeature.internal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public class AbstractFeatureWrapper implements Feature {
    private static final Logger logger = LoggerFactory.getLogger(AbstractFeatureWrapper.class);
    protected Feature feature = null;

    protected AbstractFeatureWrapper() {
        // prevent instantiation without Feature
    }

    public AbstractFeatureWrapper(Feature f) {
        Preconditions.checkNotNull(f,"FeatureWrapper requires non-null Feature in constructor");
        this.feature = f;
    }

    public Set<FeatureConfigSnapshotHolder> getFeatureConfigSnapshotHolders() throws Exception {
        LinkedHashSet <FeatureConfigSnapshotHolder> snapShotHolders = new LinkedHashSet<FeatureConfigSnapshotHolder>();
        for(ConfigFileInfo c: getConfigurationFiles()) {
            try {
                snapShotHolders.add(new FeatureConfigSnapshotHolder(c,this));
            } catch (JAXBException e) {
                logger.debug("{} is not a config subsystem config file",c.getFinalname());
            }
        }
        return snapShotHolders;
    }

    public Feature getFeature() {
        return feature;
    }

    public String getId() {
        return feature.getId();
    }

    public String getName() {
        return feature.getName();
    }

    public String getDescription() {
        return feature.getDescription();
    }

    public String getDetails() {
        return feature.getDetails();
    }

    public String getVersion() {
        return feature.getVersion();
    }

    public boolean hasVersion() {
        return feature.hasVersion();
    }

    public String getResolver() {
        return feature.getResolver();
    }

    public String getInstall() {
        return feature.getInstall();
    }

    public List<Dependency> getDependencies() {
        return feature.getDependencies();
    }

    public List<BundleInfo> getBundles() {
        return feature.getBundles();
    }

    public Map<String, Map<String, String>> getConfigurations() {
        return feature.getConfigurations();
    }

    public List<ConfigFileInfo> getConfigurationFiles() {
        return feature.getConfigurationFiles();
    }

    public List<? extends Conditional> getConditional() {
        return feature.getConditional();
    }

    public int getStartLevel() {
        return feature.getStartLevel();
    }

    public String getRegion() {
        return feature.getRegion();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((feature == null) ? 0 : feature.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractFeatureWrapper other = (AbstractFeatureWrapper) obj;
        if (feature == null) {
            if (other.feature != null)
                return false;
        } else if (!feature.equals(other.feature))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return feature.getName();
    }

}