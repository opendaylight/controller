package org.openflow.protocol.factory;

/**
 * Objects implementing this interface are expected to be instantiated with an
 * instance of an OFQueuePropertyFactory
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface OFQueuePropertyFactoryAware {
    /**
     * Sets the OFQueuePropertyFactory
     * @param queuePropertyFactory
     */
    public void setQueuePropertyFactory(OFQueuePropertyFactory queuePropertyFactory);
}
