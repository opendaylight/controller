/*
 * Copyright (c) 2006-2011 Christian Plattner. All rights reserved.
 * Please refer to the LICENSE.txt for licensing details.
 */
package ch.ethz.ssh2.transport;

/**
 * NegotiatedParameters.
 * 
 * @author Christian Plattner
 * @version 2.50, 03/15/10
 */
public class NegotiatedParameters
{
	public boolean guessOK;
	public String kex_algo;
	public String server_host_key_algo;
	public String enc_algo_client_to_server;
	public String enc_algo_server_to_client;
	public String mac_algo_client_to_server;
	public String mac_algo_server_to_client;
	public String comp_algo_client_to_server;
	public String comp_algo_server_to_client;
	public String lang_client_to_server;
	public String lang_server_to_client;
}
