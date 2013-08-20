package org.opendaylight.controller.sal.binding.api.data;

import java.util.Set;

import org.opendaylight.controller.sal.common.DataStoreIdentifier;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.DataRoot;

public interface RuntimeDataProvider {

    Set<DataStoreIdentifier> getSupportedStores();
    
    
    Set<Class<? extends DataRoot>> getProvidedDataRoots();
    
    
    /**
     * Returns a data from specified Data Store.
     * 
     * Returns all the data visible to the consumer from specified Data Store.
     * 
     * @param <T>
     *            Interface generated from YANG module representing root of data
     * @param store
     *            Identifier of the store, from which will be data retrieved
     * @return data visible to the consumer
     */
    <T extends DataRoot> T getData(DataStoreIdentifier store, Class<T> rootType);

    /**
     * Returns a filtered subset of data from specified Data Store.
     * 
     * <p>
     * The filter is modeled as an hierarchy of Java TOs starting with
     * implementation of {@link DataRoot} representing data root. The semantics
     * of the filter tree is the same as filter semantics defined in the NETCONF
     * protocol for rpc operations <code>get</code> and <code>get-config</code>
     * in Section 6 of RFC6241.
     * 
     * 
     * @see http://tools.ietf.org/html/rfc6241#section-6
     * @param <T>
     *            Interface generated from YANG module representing root of data
     * @param store
     *            Identifier of the store, from which will be data retrieved
     * @param filter
     *            Data tree filter similar to the NETCONF filter
     * @return
     */
    <T extends DataRoot> T getData(DataStoreIdentifier store, T filter);
}
