package org.openflow.protocol.factory;

/**
 * Objects implementing this interface are expected to be instantiated with an
 * instance of an OFStatisticsFactory
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public interface OFStatisticsFactoryAware {
    /**
     * Sets the OFStatisticsFactory
     * @param statisticsFactory
     */
    public void setStatisticsFactory(OFStatisticsFactory statisticsFactory);
}
