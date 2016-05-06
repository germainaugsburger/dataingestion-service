/*
 * Copyright (c) 2015 General Electric Company. All rights reserved.
 *
 * The copyright to the computer software herein is the property of
 * General Electric Company. The software may be used and/or copied only
 * with the written permission of General Electric Company or in accordance
 * with the terms and conditions stipulated in the agreement/contract
 * under which the software has been supplied.
 */

package com.ge.predix.solsvc.dataingestion.websocket;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This makes Web Socket client calls to post data to a web socket endpoint
 * @author predix -
 */
@Component
public class WebSocketClient {
	private static Logger log = Logger.getLogger(WebSocketClient.class);
	private WebSocketContainer container = ContainerProvider.getWebSocketContainer();

	@Autowired
	private WebSocketConfig endPointConfig;

	@Autowired
	private WebsocketEndpoint websocketEndpoint;

	private Map<String, Session> sessions = new HashMap<String, Session>();

	/**
	 * @param data -
	 */
	@SuppressWarnings({ "resource", "nls" })
	public void postToWebSocketServer(String data) {
		try {
			Session predixWSSession = this.sessions.get("messages");
			if (predixWSSession == null || !predixWSSession.isOpen()) {
				log.info("Opening New Connection : "
						+ this.endPointConfig.getPredixWebSocketURI());
				URI predixWebSocketURI;
				predixWebSocketURI = new URI(
						this.endPointConfig.getPredixWebSocketURI());
				this.websocketEndpoint = new WebsocketEndpoint();
				predixWSSession = this.container.connectToServer(this.websocketEndpoint,
						predixWebSocketURI);
				predixWSSession.setMaxIdleTimeout(0);
				this.sessions.put("messages", predixWSSession);
			} else {
				log.info("Reusing existing Connection" + this.endPointConfig.getPredixWebSocketURI());
			}
			predixWSSession.getBasicRemote().sendText(data);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		} catch (DeploymentException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
