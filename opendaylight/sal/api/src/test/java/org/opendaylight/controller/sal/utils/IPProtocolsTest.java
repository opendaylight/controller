/**
 *
 */
package org.opendaylight.controller.sal.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author ykhodork
 *
 */
public class IPProtocolsTest {

    static short shortVal = 1;
    static int   intVal = 1;
    static byte  byteVal = 1;

    /**
     * Test method for {@link org.opendaylight.controller.sal.utils.IPProtocols#getProtocolName(int)}.
     */
    @Test
    public void testGetProtocolNameInt() {
        assertEquals("ICMP", IPProtocols.getProtocolName(1));
        assertEquals("0x4d2", IPProtocols.getProtocolName(1234));
    }

    /**
     * Test method for {@link org.opendaylight.controller.sal.utils.IPProtocols#getProtocolName(short)}.
     */
    @Test
    public void testGetProtocolNameShort() {
        assertEquals("ICMP", IPProtocols.getProtocolName(shortVal));
    }

    /**
     * Test method for {@link org.opendaylight.controller.sal.utils.IPProtocols#getProtocolName(byte)}.
     */
    @Test
    public void testGetProtocolNameByte() {
        assertEquals("ICMP", IPProtocols.getProtocolName(byteVal));
    }

    /**
     * Test method for {@link org.opendaylight.controller.sal.utils.IPProtocols#getProtocolNumberShort(java.lang.String)}.
     */
    @Test
    public void testGetProtocolNumberShort() {
        assertEquals(shortVal, IPProtocols.getProtocolNumberShort("ICMP"));
    }

    /**
     * Test method for {@link org.opendaylight.controller.sal.utils.IPProtocols#getProtocolNumberInt(java.lang.String)}.
     */
    @Test
    public void testGetProtocolNumberInt() {
        assertEquals(intVal, IPProtocols.getProtocolNumberInt("ICMP"));
    }

    /**
     * Test method for {@link org.opendaylight.controller.sal.utils.IPProtocols#getProtocolNumberByte(java.lang.String)}.
     */
    @Test
    public void testGetProtocolNumberByte() {
        assertEquals(byteVal, IPProtocols.getProtocolNumberByte("ICMP"));
    }

    /**
     * Test method for {@link org.opendaylight.controller.sal.utils.IPProtocols#fromString(java.lang.String)}.
     */
    @Test
    public void testFromString() {
        assertTrue(null == IPProtocols.fromString("Not a protocol"));
        assertTrue(null == IPProtocols.fromString("0xFFF"));
        assertTrue(null == IPProtocols.fromString("-2"));

        assertTrue(IPProtocols.ANY == IPProtocols.fromString("any"));
        assertTrue(IPProtocols.ANY == IPProtocols.fromString("ANY"));
        assertTrue(IPProtocols.ANY == IPProtocols.fromString("*"));
        assertTrue(IPProtocols.ANY == IPProtocols.fromString(null));

        assertTrue(IPProtocols.TCP == IPProtocols.fromString("TCP"));
        assertTrue(IPProtocols.TCP == IPProtocols.fromString("tcp"));
        assertTrue(IPProtocols.UDP == IPProtocols.fromString("0x11"));
        assertTrue(IPProtocols.UDP == IPProtocols.fromString("0X11"));

    }

}
















