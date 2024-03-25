package httpserver.server;

import static httpserver.util.ColorUtil.rst;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.PatternSyntaxException;

import httpserver.util.ColorUtil;
import httpserver.util.ColorUtil.Prefix;

public class Server extends Thread {
	private List<Session> sessions;
	private Map<String, RouteEndpoint> routes;
	private ServerSocket socket;
	private Logger logger;
	
	/**
	 * 
	 * */
	public Server(int port, Logger logger) throws IOException {
		socket = new ServerSocket(port);
		this.logger = logger;
		routes = new HashMap<>();
		sessions = new ArrayList<>();
	}
	
	public void route(String route, RouteEndpoint endpoint) {
		routes.put(route, endpoint);
	}
	
	public void run() {
		logger.log(Level.INFO, 
				"Serving HTTP on " + socket.getLocalSocketAddress());
		while(!socket.isClosed()) {
			try {
				var client = socket.accept();
				logger.log(Level.INFO, ColorUtil.fromIP(client.getInetAddress(), Prefix.BACKGROUND) +
						"connected: " 
							+ client.getInetAddress().getHostAddress()
							+ " | " + client.getInetAddress().getHostName()
				+ rst());
				
				new Thread(() -> {
					try {
						handle(client);
						client.close();
						logger.log(Level.INFO, ColorUtil.fromIP(client.getInetAddress(), Prefix.BACKGROUND) +
								"disconnected: " + client.getInetAddress().getHostAddress()
						+ rst());
					} catch (IOException e) {
						logger.log(Level.WARNING, ColorUtil.fromIP(client.getInetAddress(), Prefix.BACKGROUND) +
								"I/O Exception: " + e.getMessage()
						+ rst());
					}
				}).start();
			} catch (IOException e) {
				logger.log(Level.WARNING, 
						"I/O Exception: " + e.getMessage());
			}
		}
		
		logger.log(Level.INFO, "Server stopped");
	}
	
	private void handle(Socket client) throws IOException {
		var inStream = client.getInputStream();
		var reader = new BufferedReader(new InputStreamReader(inStream));
		final var address = client.getInetAddress();
		
		String[] request = null;
		var headers = new HashMap<String,String>();
		var line = "";
		while((line = reader.readLine()) != null && !line.isBlank()) {
			if(request == null) {
				request = line.split(" ");
				logger.log(Level.INFO, ColorUtil.fromIP(address) +
						"[" + address.getHostAddress() + "] " + line
				+ rst());
				
				if(request.length != 3) {
					logger.log(Level.WARNING, ColorUtil.fromIP(address) +
							"[" + address.getHostAddress() + "] < Bad request: " + line 
					+ rst());
					var session = new Session(client, "HTTP/1.1");
					session.sendStatus(HttpStatus.BAD_REQUEST);
					session.complete();
					return;
				}
				continue;
			} else
				logger.log(Level.DEBUG, ColorUtil.fromIP(address) +
						"[" + address.getHostAddress() + "] " + line
				+ rst());
			
			if(!line.matches("([A-Za-z-]+):.+")) {
				logger.log(Level.WARNING, "400 Bad Request: Illegal header formatting");
				return;
			}
			
			var header = line.split(":");
			headers.put(header[0].trim(), header[1].trim());
		}
		if(request == null)
			return;
		
		var parameters = new HashMap<String, String>();
		var httpVersion = request[0];
		var resource = request[1];
		var resourceSplit = resource.split("\\?");
		if(resourceSplit.length == 2) {
			resource = resourceSplit[0];
			var params = resourceSplit[1].split("&");
			for(var param : params) {
				if(!param.contains("=")) {
					logger.log(Level.WARNING, ColorUtil.fromIP(address) +
							"Illegal Parameter Formatting: " + line
					+ rst());
					return;
				}
				parameters.put(param.split("=")[0].replaceAll("\\+", " "), param.split("=")[1].replaceAll("\\+", " "));
			}
		}
		if(resource.endsWith("?"))
			resource = resource.substring(0, resource.length() - 1);
		var session = new Session(client, headers, parameters, request[2]);
		if(!resource.startsWith("/")) {
			session.sendStatus(HttpStatus.BAD_REQUEST);
			session.complete();
			logger.log(Level.WARNING, ColorUtil.fromIP(address) + 
					"[" + address.getHostAddress() + "] < Bad request: " + resource
			+ rst());
			return;
		}
		
		var content_len = headers.get("Content-Length");
		String str = "";
		if(content_len != null) {
			if(!content_len.matches("\\d+")) {
				logger.log(Level.WARNING, "400 Bad Request: Expected numerical value for Content-Length");
				session.sendStatus(HttpStatus.BAD_REQUEST);
				return;
			}
			var len = Integer.parseInt(content_len);
			while(str.length() < len) {
				int c = reader.read();
				str += (char) c;
			}
		}
		session.setRequestBody(str);
		
		sessions.add(session);
		
		getRoute(resource)
			.orElseGet(() -> (m,r,s) -> s.sendStatus(HttpStatus.NOT_FOUND))
			.handle(
				httpVersion,
				resource,
				session
			);
		
		if(!session.isDisposed())
			session.complete();
		
		sessions.remove(session);
	}
	
	private Optional<RouteEndpoint> getRoute(String route) {
		if(routes.containsKey(route))
			return Optional.of(routes.get(route));
		for(var entry : routes.entrySet())
			try {
				if(route.matches(entry.getKey()))
					return Optional.of(entry.getValue());
			} catch(PatternSyntaxException e) { }
		return Optional.empty();
	}
}
