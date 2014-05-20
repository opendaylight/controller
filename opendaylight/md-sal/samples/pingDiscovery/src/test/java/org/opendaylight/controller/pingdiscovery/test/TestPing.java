/*
* Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/
package org.opendaylight.controller.pingdiscovery.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;

import org.junit.Test;

public class TestPing {

    @Test
    public void test()
    {
        try {
            InetAddress inetAddress =  InetAddress.getByName( "www.google.com" );
            System.out.println( inetAddress.isReachable( 3000 ) );

            Process p = Runtime.getRuntime().exec( "ping -c 1 www.google.com" );

            InputStream inputStream = p.getInputStream();

            BufferedReader reader = new BufferedReader( new InputStreamReader( inputStream ));
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                System.out.println( line );
            }


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


}
