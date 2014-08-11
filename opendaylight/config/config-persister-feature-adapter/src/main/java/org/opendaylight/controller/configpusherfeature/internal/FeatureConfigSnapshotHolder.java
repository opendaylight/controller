package org.opendaylight.controller.configpusherfeature.internal;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.Feature;
import org.opendaylight.controller.config.persist.api.ConfigSnapshotHolder;
import org.opendaylight.controller.config.persist.storage.file.xml.model.ConfigSnapshot;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class FeatureConfigSnapshotHolder implements ConfigSnapshotHolder {
    private ConfigSnapshot unmarshalled = null;
    private ConfigFileInfo fileInfo = null;
    private List<Feature> featureChain = new ArrayList<Feature>();

    public FeatureConfigSnapshotHolder(final FeatureConfigSnapshotHolder holder, final Feature feature) throws JAXBException {
        this(holder.fileInfo,holder.getFeature());
        this.featureChain.add(feature);
    }

    public FeatureConfigSnapshotHolder(final ConfigFileInfo fileInfo, final Feature feature) throws JAXBException {
        Preconditions.checkNotNull(fileInfo);
        Preconditions.checkNotNull(fileInfo.getFinalname());
        Preconditions.checkNotNull(feature);
        this.fileInfo = fileInfo;
        this.featureChain.add(feature);
        JAXBContext jaxbContext = JAXBContext.newInstance(ConfigSnapshot.class);
        Unmarshaller um = jaxbContext.createUnmarshaller();
        File file = new File(fileInfo.getFinalname());
        unmarshalled = ((ConfigSnapshot) um.unmarshal(file));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((unmarshalled != null && unmarshalled.getConfigSnapshot() == null) ? 0 : unmarshalled.getConfigSnapshot().hashCode());
        return result;
    }

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
    public String getConfigSnapshot() {
        return unmarshalled.getConfigSnapshot();
    }

    @Override
    public SortedSet<String> getCapabilities() {
        return unmarshalled.getCapabilities();
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
    public ConfigFileInfo getFileInfo() {
        return fileInfo;
    }

    public Feature getFeature() {
        return featureChain.get(0);
    }

    public ImmutableList<Feature> getFeatureChain() {
        return ImmutableList.copyOf(Lists.reverse(featureChain));
    }

    public Feature getCauseFeature() {
        return Iterables.getLast(featureChain);
    }
}
