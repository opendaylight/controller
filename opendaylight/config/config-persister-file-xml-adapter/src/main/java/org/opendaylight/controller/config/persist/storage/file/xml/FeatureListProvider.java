package org.opendaylight.controller.config.persist.storage.file.xml;

import java.util.Set;

/**
 * Wrapper for services providing list of features to be stored along with the config snapshot
 */
public interface FeatureListProvider {

    Set<String> listFeatures();
}
