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
import java.util.Set;
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
    private FeaturesService featuresService = null;

    protected ChildAwareFeatureWrapper(final Feature feature) {
        // Don't use without a feature service
    }

    /* Constructor.

     * @param feature Feature to wrap
     * @param featuresService FeaturesService to look up dependencies
     */
    ChildAwareFeatureWrapper(final Feature feature, final FeaturesService featuresService) throws Exception {
        super(featuresService.getFeature(feature.getName(), feature.getVersion()));
        Preconditions.checkNotNull(featuresService, "FeatureWrapper requires non-null FeatureService in constructor");
        this.featuresService = featuresService;
    }

    protected FeaturesService getFeaturesService() {
        return featuresService;
    }

    /*
     * Get FeatureConfigSnapshotHolders appropriate to feed to the config subsystem
     * from the underlying Feature Config files and those of its children recursively
     */
    public Set<? extends ChildAwareFeatureWrapper> getChildFeatures() throws Exception {
        List<Dependency> dependencies = feature.getDependencies();
        Set<ChildAwareFeatureWrapper> childFeatures = new LinkedHashSet<>();
        if (dependencies != null) {
            for (Dependency dependency : dependencies) {
                Feature fi = extractFeatureFromDependency(dependency);
                if (fi != null) {
                    if (featuresService.getFeature(fi.getName(), fi.getVersion()) == null) {
                        LOG.warn("Feature: {}, {} is missing from features service. Skipping", fi.getName(), fi
                                .getVersion());
                    } else {
                        ChildAwareFeatureWrapper wrappedFeature = new ChildAwareFeatureWrapper(fi, featuresService);
                        childFeatures.add(wrappedFeature);
                    }
                }
            }
        }
        return childFeatures;
    }

    @Override
    public Set<FeatureConfigSnapshotHolder> getFeatureConfigSnapshotHolders() throws Exception {
        Set<FeatureConfigSnapshotHolder> snapShotHolders = new LinkedHashSet<>();
        for (ChildAwareFeatureWrapper c : getChildFeatures()) {
            for (FeatureConfigSnapshotHolder h : c.getFeatureConfigSnapshotHolders()) {
                final Optional<FeatureConfigSnapshotHolder> featureConfigSnapshotHolder =
                        getFeatureConfigSnapshotHolder(h.getFileInfo());
                if (featureConfigSnapshotHolder.isPresent()) {
                    snapShotHolders.add(featureConfigSnapshotHolder.get());
                }
            }
        }
        snapShotHolders.addAll(super.getFeatureConfigSnapshotHolders());
        return snapShotHolders;
    }

    protected Feature extractFeatureFromDependency(final Dependency dependency) throws Exception {
        Feature[] features = featuresService.listFeatures();
        VersionRange range = dependency.hasVersion() ? new VersionRange(dependency.getVersion(), true, true)
                : VersionRange.ANY_VERSION;
        Feature fi = null;
        for (Feature f : features) {
            if (f.getName().equals(dependency.getName())) {
                Version version = VersionTable.getVersion(f.getVersion());
                if (range.contains(version) && (fi == null || VersionTable.getVersion(fi.getVersion())
                        .compareTo(version) < 0)) {
                    fi = f;
                    break;
                }
            }
        }
        return fi;
    }
}
