/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.transport;

/**
 * KexParameters.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class KexParameters
{
	public byte[] cookie;
	public String[] kex_algorithms;
	public String[] server_host_key_algorithms;
	public String[] encryption_algorithms_client_to_server;
	public String[] encryption_algorithms_server_to_client;
	public String[] mac_algorithms_client_to_server;
	public String[] mac_algorithms_server_to_client;
	public String[] compression_algorithms_client_to_server;
	public String[] compression_algorithms_server_to_client;
	public String[] languages_client_to_server;
	public String[] languages_server_to_client;
	public boolean first_kex_packet_follows;
	public int reserved_field1;
}
