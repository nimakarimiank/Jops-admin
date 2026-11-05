/*
 *  Copyright 2019-2025 Zheng Jie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package me.zhengjie.modules.maint.service.websocket;

import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author ZhangHouYing
 * @date 2019-08-10 15:46
 */
@ServerEndpoint("/webSocket/{sid}")
@Slf4j
@Component
public class WebSocketServer {

	/**
	 * Concurrent thread-safe Set to store WebSocketServer object for each client
	 */
	private static final CopyOnWriteArraySet<WebSocketServer> webSocketSet = new CopyOnWriteArraySet<>();

	/**
	 * Connection session with a client, used to send data to the client
	 */
	private Session session;

	/**
	 * Received sid
	 */
	private String sid="";
	/**
	 * Method called upon successful connection establishment
	 * */
	@OnOpen
	public void onOpen(Session session,@PathParam("sid") String sid) {
		this.session = session;
		// If exists, remove first to prevent duplicate message push
		webSocketSet.removeIf(webSocket -> webSocket.sid.equals(sid));
		webSocketSet.add(this);
		this.sid=sid;
	}

	/**
	 * Method called when connection closes
	 */
	@OnClose
	public void onClose() {
		webSocketSet.remove(this);
	}

	/**
	 * Method called after receiving client message
	 * @param message Message sent from client */
	@OnMessage
	public void onMessage(String message, Session session) {
		log.info("Received message from " + sid + ": " + message);
		// Broadcast message
		for (WebSocketServer item : webSocketSet) {
			try {
				item.sendMessage(message);
			} catch (IOException e) {
				log.error(e.getMessage(),e);
			}
		}
	}

	@OnError
	public void onError(Session session, Throwable error) {
		log.error("An error occurred", error);
	}
	/**
	 * Implement server-initiated push
	 */
	private void sendMessage(String message) throws IOException {
		this.session.getBasicRemote().sendText(message);
	}


	/**
	 * Broadcast custom message
	 * */
	public static void sendInfo(SocketMsg socketMsg,@PathParam("sid") String sid) throws IOException {
		String message = JSON.toJSONString(socketMsg);
		log.info("Pushing message to " + sid + ", content: " + message);
		for (WebSocketServer item : webSocketSet) {
			try {
				// Push only to this sid if specified, or broadcast to all if null
				if(sid==null) {
					item.sendMessage(message);
				}else if(item.sid.equals(sid)){
					item.sendMessage(message);
				}
			} catch (IOException ignored) { }
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		WebSocketServer that = (WebSocketServer) o;
		return Objects.equals(session, that.session) &&
				Objects.equals(sid, that.sid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(session, sid);
	}
}
