/**
 *
 */
package org.openflow.protocol.queue;

import java.lang.reflect.Constructor;

import org.openflow.protocol.Instantiable;

/**
 * List of OpenFlow Queue Property types and mappings to wire protocol value and
 * derived classes
 *
 * @author David Erickson (daviderickson@cs.stanford.edu)
 */
public class OFQueuePropertyType {
    public static OFQueuePropertyType NONE = new OFQueuePropertyType(0, "NONE",
            OFQueueProperty.class, new Instantiable<OFQueueProperty>() {
                @Override
                public OFQueueProperty instantiate() {
                    return new OFQueueProperty();
                }
            });

    public static OFQueuePropertyType MIN_RATE = new OFQueuePropertyType(1, "MIN_RATE",
            OFQueuePropertyMinRate.class, new Instantiable<OFQueueProperty>() {
                @Override
                public OFQueueProperty instantiate() {
                    return new OFQueuePropertyMinRate();
                }
            });

    protected static OFQueuePropertyType[] mapping;

    protected Class<? extends OFQueueProperty> clazz;
    protected Constructor<? extends OFQueueProperty> constructor;
    protected Instantiable<OFQueueProperty> instantiable;
    protected int minLen;
    protected String name;
    protected short type;

    /**
     * Store some information about the OpenFlow Queue Property type, including wire
     * protocol type number, length, and derived class
     *
     * @param type Wire protocol number associated with this OFQueuePropertyType
     * @param name The name of this type
     * @param clazz The Java class corresponding to this type of OpenFlow Queue Property
     * @param instantiable the instantiable for the OFQueueProperty this type represents
     */
    OFQueuePropertyType(int type, String name, Class<? extends OFQueueProperty> clazz, Instantiable<OFQueueProperty> instantiable) {
        this.type = (short) type;
        this.name = name;
        this.clazz = clazz;
        this.instantiable = instantiable;
        try {
            this.constructor = clazz.getConstructor(new Class[]{});
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failure getting constructor for class: " + clazz, e);
        }
        OFQueuePropertyType.addMapping(this.type, this);
    }

    /**
     * Adds a mapping from type value to OFQueuePropertyType enum
     *
     * @param i OpenFlow wire protocol Action type value
     * @param t type
     */
    static public void addMapping(short i, OFQueuePropertyType t) {
        if (mapping == null)
            mapping = new OFQueuePropertyType[16];
        OFQueuePropertyType.mapping[i] = t;
    }

    /**
     * Given a wire protocol OpenFlow type number, return the OFType associated
     * with it
     *
     * @param i wire protocol number
     * @return OFType enum type
     */

    static public OFQueuePropertyType valueOf(short i) {
        return OFQueuePropertyType.mapping[i];
    }

    /**
     * @return Returns the wire protocol value corresponding to this
     *         OFQueuePropertyType
     */
    public short getTypeValue() {
        return this.type;
    }

    /**
     * @return return the OFQueueProperty subclass corresponding to this OFQueuePropertyType
     */
    public Class<? extends OFQueueProperty> toClass() {
        return clazz;
    }

    /**
     * Returns the no-argument Constructor of the implementation class for
     * this OFQueuePropertyType
     * @return the constructor
     */
    public Constructor<? extends OFQueueProperty> getConstructor() {
        return constructor;
    }

    /**
     * Returns a new instance of the OFQueueProperty represented by this OFQueuePropertyType
     * @return the new object
     */
    public OFQueueProperty newInstance() {
        return instantiable.instantiate();
    }

    /**
     * @return the instantiable
     */
    public Instantiable<OFQueueProperty> getInstantiable() {
        return instantiable;
    }

    /**
     * @param instantiable the instantiable to set
     */
    public void setInstantiable(Instantiable<OFQueueProperty> instantiable) {
        this.instantiable = instantiable;
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
