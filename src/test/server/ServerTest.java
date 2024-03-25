package test.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import httpserver.server.HttpStatus;
import httpserver.server.Server;
import httpserver.util.DefaultLogger;

class ServerTest {

	@Test
	void parseHttpHeaders() throws IOException, InterruptedException {
		int port = 57438;
		var logger = new DefaultLogger("test");
		var server = new Server(port, logger);
		var headers = Map.of(
			"TestHeader", "TestValue",
			"SomeHeader", "OtherValue=some"
		);
		var running = new AtomicBoolean(true);
		
		server.route("/", (meth, res, sess) -> {
			assertEquals("GET", meth);
			assertEquals("/", res);
			
			assertEquals(headers, sess.getRequestHeaders());
			
			sess.sendStatus(HttpStatus.OK);
			running.set(false);
		});
		
		
		var client = HttpClient.newBuilder()
			.version(Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(20))
			.build();
		
		var requestBuilder = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port));
		headers.forEach((k,v) -> requestBuilder.header(k, v));
		var request = requestBuilder.GET().build();
		
		client.sendAsync(request, BodyHandlers.ofString())
			.thenApply(t -> t.statusCode())
			.thenAccept(c -> assertEquals(200, c));
		
		while(running.get()) { }
	}
	
	@Test
	void parseBody() throws IOException, InterruptedException {
		int port = 57439;
		var logger = new DefaultLogger("test");
		logger.setLevel(Level.ALL);
		var server = new Server(port, logger);
		var body = "this is some testbody\nAND another line";
		var running = new AtomicBoolean(true);
		
		server.route("/", (meth, res, sess) -> {
			assertEquals("POST", meth);
			assertEquals("/", res);
			
			assertEquals(body, sess.getRequestBody());
			
			sess.sendStatus(HttpStatus.OK);
			running.set(false);
		});
		
		var client = HttpClient.newBuilder()
			.version(Version.HTTP_1_1)
			.connectTimeout(Duration.ofSeconds(20))
			.build();
		
		var request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port))
			.POST(BodyPublishers.ofString(body))
			.build();
		
		System.out.println(client.send(request, BodyHandlers.ofString()));
	}

}
