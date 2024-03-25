module HttpServer {
	requires java.net.http;
	requires org.junit.jupiter.api;
	
	exports httpserver.server;
	exports httpserver.util;
}