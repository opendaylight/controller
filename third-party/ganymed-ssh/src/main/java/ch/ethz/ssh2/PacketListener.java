/*
 * Copyright (c) 2011 David Kocher. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2;

/**
 * @version $Id:$
 */
public interface PacketListener {
    void read(String packet);

    void write(String packet);
}
