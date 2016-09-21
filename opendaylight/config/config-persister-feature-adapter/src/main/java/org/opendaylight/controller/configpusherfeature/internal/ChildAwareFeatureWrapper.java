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
import java.util.LinkedHashSet;
import java.util.List;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Wrap a Feature for the purposes of extracting the FeatureConfigSnapshotHolders from
 * its underlying ConfigFileInfo's and those of its children recursively
 *
 * Delegates the the contained feature and provides additional methods.
 */
public class ChildAwareFeatureWrapper extends AbstractFeatureWrapper implements Feature {
    private static final Logger LOG = LoggerFactory.getLogger(ChildAwareFeatureWrapper.class);
    private FeaturesService featuresService= null;

    protected ChildAwareFeatureWrapper(Feature f) {
        // Don't use without a feature service
    }

    /*
     * @param f Feature to wrap
     * @param s FeaturesService to look up dependencies
     */
    ChildAwareFeatureWrapper(Feature f, FeaturesService s) throws Exception {
        super(s.getFeature(f.getName(), f.getVersion()));
        Preconditions.checkNotNull(s, "FeatureWrapper requires non-null FeatureService in constructor");
        this.featuresService = s;
    }

    protected FeaturesService getFeaturesService() {
        return featuresService;
    }

    /*
     * Get FeatureConfigSnapshotHolders appropriate to feed to the config subsystem
     * from the underlying Feature Config files and those of its children recursively
     */
    public LinkedHashSet <? extends ChildAwareFeatureWrapper> getChildFeatures() throws Exception {
        List<Dependency> dependencies = feature.getDependencies();
        LinkedHashSet <ChildAwareFeatureWrapper> childFeatures = new LinkedHashSet<>();
        if(dependencies != null) {
            for(Dependency dependency: dependencies) {
                Feature fi = extractFeatureFromDependency(dependency);
                if(fi != null) {
                    if(featuresService.getFeature(fi.getName(), fi.getVersion()) == null) {
                        LOG.warn("Feature: {}, {} is missing from features service. Skipping", fi.getName(), fi.getVersion());
                    } else {
                        ChildAwareFeatureWrapper wrappedFeature = new ChildAwareFeatureWrapper(fi,featuresService);
                        childFeatures.add(wrappedFeature);
                    }
                }
            }
        }
        return childFeatures;
    }

    @Override
    public LinkedHashSet<FeatureConfigSnapshotHolder> getFeatureConfigSnapshotHolders() throws Exception {
        LinkedHashSet <FeatureConfigSnapshotHolder> snapShotHolders = new LinkedHashSet<>();
        for(ChildAwareFeatureWrapper c: getChildFeatures()) {
            for(FeatureConfigSnapshotHolder h: c.getFeatureConfigSnapshotHolders()) {
                final Optional<FeatureConfigSnapshotHolder> featureConfigSnapshotHolder = getFeatureConfigSnapshotHolder(h.getFileInfo());
                if(featureConfigSnapshotHolder.isPresent()) {
                    snapShotHolders.add(featureConfigSnapshotHolder.get());
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
                if (range.contains(v) &&
                    (fi == null || VersionTable.getVersion(fi.getVersion()).compareTo(v) < 0)) {
                    fi = f;
                    break;
                }
            }
        }
        return fi;
    }

}
