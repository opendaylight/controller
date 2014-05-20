/*
 * Copyright (c) 2014 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.controller.sample.pingdiscovery.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.regex.Matcher;

import org.opendaylight.controller.sample.pingdiscovery.PingService;

/**
 * This code is responsible for performing the actual "Ping" to hosts. We actually execute a
 * "ping" command from the command line (which means that "ping" must be resolvable from the command
 * line for this to work). We do this to work around a limitation of {@link InetAddress#isReachable(int)} where
 * you can not "ping" hosts outside of your local lan. :)
 *
 * TODO: This should not be static method calls - we should move this to an interface / implementation
 * design so we can have different implementations of ping...
 * @author Devin Avery
 * @author Greg Hall
 *
 */
public class PingServiceExecSystemImpl implements PingService {



    @Override
    public  double ping( String host )
    {
        return ping( host, 1, 3 );
    }

    @Override
    public  double ping( String host, int count, int timeoutSeconds )
    {
        try
        {
            double returnValue = NOT_FOUND;
            Process p = Runtime.getRuntime().exec( "ping -c " + count + " -W " + timeoutSeconds + " " + host );

            InputStream inputStream = p.getInputStream();

            BufferedReader reader = new BufferedReader( new InputStreamReader( inputStream ));
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                Matcher matcher = VALID_PING_RESPONSE.matcher( line );
                if( matcher.matches() )
                {
                    returnValue = Double.parseDouble( matcher.group( 1 ) );
                    break;
                }
            }
            p.destroy();
            return returnValue;
        }
        catch( IOException e)
        {
            return NOT_FOUND;
        }

    }
}
