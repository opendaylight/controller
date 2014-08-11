package org.opendaylight.controller.configpusherfeature.internal;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class ChildAwareFeatureWrapper extends AbstractFeatureWrapper implements Feature {
    private static final Logger logger = LoggerFactory.getLogger(ChildAwareFeatureWrapper.class);
    private FeaturesService featuresService= null;

    protected ChildAwareFeatureWrapper(Feature f) {
        // Don't use without a feature service
    }

    ChildAwareFeatureWrapper(Feature f, FeaturesService s) throws Exception {
        super(s.getFeature(f.getName(), f.getVersion()));
        Preconditions.checkNotNull(s, "FeatureWrapper requires non-null FeatureService in constructor");
        this.featuresService = s;
    }

    protected FeaturesService getFeaturesService() {
        return featuresService;
    }

    public ImmutableList <? extends ChildAwareFeatureWrapper> getChildFeatures() throws Exception {
        List<Dependency> dependencies = feature.getDependencies();
        LinkedHashSet <ChildAwareFeatureWrapper> childFeatures = new LinkedHashSet<ChildAwareFeatureWrapper>();
        if(dependencies != null) {
            for(Dependency dependency: dependencies) {
                Feature fi = extractFeatureFromDependency(dependency);
                if(fi != null){
                    ChildAwareFeatureWrapper wrappedFeature = new ChildAwareFeatureWrapper(fi,featuresService);
                    childFeatures.add(wrappedFeature);
                }
            }
        }
        return ImmutableList.copyOf(childFeatures);
    }

    public LinkedHashSet<FeatureConfigSnapshotHolder> getFeatureConfigSnapshotHolders() throws Exception {
        LinkedHashSet <FeatureConfigSnapshotHolder> snapShotHolders = new LinkedHashSet<FeatureConfigSnapshotHolder>();
        for(ChildAwareFeatureWrapper c: getChildFeatures()) {
            for(FeatureConfigSnapshotHolder h: c.getFeatureConfigSnapshotHolders()) {
                FeatureConfigSnapshotHolder f;
                try {
                    f = new FeatureConfigSnapshotHolder(h,this);
                    snapShotHolders.add(f);
                } catch (JAXBException e) {
                    logger.debug("{} is not a config subsystem config file",h.getFileInfo().getFinalname());
                }
            }
        }
        snapShotHolders.addAll(super.getFeatureConfigSnapshotHolders());
        return snapShotHolders;
    }

    protected Feature extractFeatureFromDependency(Dependency dependency) throws Exception {
        Feature[] features = featuresService.listFeatures();
        VersionRange range = org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION.equals(dependency.getVersion())
                ? VersionRange.ANY_VERSION : new VersionRange(dependency.getVersion(), true, true);
        Feature fi = null;
        for(Feature f: features) {
            if (f.getName().equals(dependency.getName())) {
                Version v = VersionTable.getVersion(f.getVersion());
                if (range.contains(v)) {
                    if (fi == null || VersionTable.getVersion(fi.getVersion()).compareTo(v) < 0) {
                        fi = f;
                        break;
                    }
                }
            }
        }
        return fi;
    }

}
