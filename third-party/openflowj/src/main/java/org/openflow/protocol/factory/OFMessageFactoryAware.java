/**
 * 
 */
package org.openflow.protocol.factory;

/**
 * @author Rob Sherwood (rob.sherwood@stanford.edu)
 *
 */
public interface OFMessageFactoryAware {

       /**
        * Sets the message factory for this object
        * 
        * @param factory
        */
       void setMessageFactory(OFMessageFactory factory);
}
