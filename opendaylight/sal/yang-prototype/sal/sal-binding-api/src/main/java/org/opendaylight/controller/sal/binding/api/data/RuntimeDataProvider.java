package org.opendaylight.controller.sal.binding.api.data;

import java.util.Set;

import org.opendaylight.yangtools.yang.binding.DataRoot;

public interface RuntimeDataProvider {

    Set<Class<? extends DataRoot>> getProvidedDataRoots();
}
