package httpserver.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Session {
	private boolean disposed = false;
	
	// request
	private OutputStream outputStream;
	private BufferedWriter writer;
	private Map<String, String> requestHeaders, parameters;
	private String httpVersion, body;
	
	// response
	private HttpStatus status;
	
	public Session(Socket client, String httpVersion) throws IOException {
		this(client, new HashMap<>(), httpVersion);
	}
	public Session(Socket client, Map<String,String> requestHeaders, String httpVersion) throws IOException {
		this(client, requestHeaders, new HashMap<>(), httpVersion);
	}
	public Session(Socket client, Map<String,String> requestHeaders, Map<String,String> parameters, String httpVersion) throws IOException {
		this(client, requestHeaders, parameters, httpVersion, "");
	}
	public Session(Socket client, Map<String,String> requestHeaders, Map<String,String> parameters, String httpVersion, String body) throws IOException {
		this.requestHeaders = requestHeaders;
		this.httpVersion = httpVersion;
		this.parameters = parameters;
		this.body = body;
		writer = new BufferedWriter(new OutputStreamWriter(outputStream = client.getOutputStream()));
	}
	
	public void setRequestHeaders(Map<String,String> requestHeaders) {
		this.requestHeaders.putAll(requestHeaders);
	}
	public void requestHeader(String key, String value) {
		requestHeaders.put(key, value);
	}
	public void setParameters(Map<String,String> parameters) {
		this.parameters = parameters;
	}
	public void setRequestBody(String body) {
		this.body = body;
	}
	
	
	public Optional<String> requestHeader(String key) {
		return Optional.ofNullable(requestHeaders.get(key));
	}
	public Map<String,String> getRequestHeaders() {
		return Map.copyOf(requestHeaders);
	}
	public Map<String,String> getParameters() {
		return Map.copyOf(parameters);
	}
	public String getHttpVersion() {
		return httpVersion;
	}
	public String getRequestBody() {
		return body;
	}
	
	/**
	 * sends a status code with http version to the client
	 * @param status the http status code
	 * @throws IOException If an I/O error occurs (e.g. the client disconnects)
	 * @throws IllegalStateException If the status has already been set before or the session has been disposed
	 */
	public void sendStatus(HttpStatus status) throws IOException {
		if(disposed)
			throw new IllegalStateException("Session is disposed");
		if(this.status != null)
			throw new IllegalStateException("Status has already been set");
		
		this.status = status;
		writer.write(httpVersion + " " + status.code + " " + status.name() + "\r\n");
	}
	/**
	 * sends a header to the client
	 * @throws IOException If an I/O error occurs (e.g. the client disconnects)
	 * @throws IllegalStateException If the status hasn't been set, the session has been disposed or body data has already been sent
	 */
	public void sendHeader(String key, String value) throws IOException {
		if(disposed)
			throw new IllegalStateException("Session is disposed");
		if(status == null)
			throw new IllegalStateException("Status has not been set");
		if(sentBodyData)
			throw new IllegalStateException("Body data has already been sent");
		writer.write(key + ": " + value + "\r\n");
	}
	
	private boolean sentBodyData;
	/**
	 * sends body-data to the client, it will also ensure that the body is seperated by a newline
	 * 
	 * @throws IOException If an I/O error occurs (e.g. the client disconnects)
	 * @throws IllegalStateException If the status hasn't been set or the session has been disposed
	 */
	public void sendBody(String body) throws IOException {
		if(disposed)
			throw new IllegalStateException("Session is disposed");
		if(status == null)
			throw new IllegalStateException("Status has not been set");
		if(!sentBodyData) {
			writer.write("\r\n");
			sentBodyData = true;
		}
		writer.write(body);
	}
	/**
	 * sends body-data to the client
	 * 
	 * @throws IOException If an I/O error occurs (e.g. the client disconnects)
	 * @throws IllegalStateException If the status hasn't been set or the session has been disposed
	 */
	public void sendBody(byte[] body) throws IOException {
		if(disposed)
			throw new IllegalStateException("Session is disposed");
		if(status == null)
			throw new IllegalStateException("Status has not been set");
		if(!sentBodyData) {
			writer.write("\r\n");
			sentBodyData = true;
		}
		outputStream.write(body);
		outputStream.flush();
	}
	/**
	 * sends n bytes of the body-data to the client
	 * 
	 * @throws IOException If an I/O error occurs (e.g. the client disconnects)
	 * @throws IllegalStateException If the status hasn't been set or the session has been disposed
	 */
	public void sendBody(byte[] body, int offset, int length) throws IOException {
		if(disposed)
			throw new IllegalStateException("Session is disposed");
		if(status == null)
			throw new IllegalStateException("Status has not been set");
		if(!sentBodyData) {
			writer.write("\r\n");
			sentBodyData = true;
		}
		writer.flush(); // make sure to not mess up order
		outputStream.write(body, offset, length);
		outputStream.flush();
	}
	
	/**
	 * completes the transaction and disposes this session
	 * 
	 * @throws IOException If an I/O error occurs (e.g. the client disconnects)
	 * @throws IllegalStateException If the session has already been disposed
	 */
	public void complete() throws IOException {
		if(disposed)
			throw new IllegalStateException("Session is disposed");
		writer.write("\r\n");
		writer.flush();
		outputStream.flush();
		writer.close();
		outputStream.close();
		
		writer = null;
		requestHeaders = null;
		status = null;
		disposed = true;
	}
	
	
	public boolean isDisposed() {
		return disposed;
	}
}
