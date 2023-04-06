# HttpServer

### Quickstart:
```java
public class Main {
	public static void main(String[] args) {
		// setup a default logger
		Logger logger = new DefaultLogger("SimpleHttpServer");

		// setup Server
		Server server = new Server(80, logger);
		
		server.route("/", Main::index);
		server.route("/index.html", Main::index);
	}

	public static void index(String httpMethod, String resource, Session session) throws IOException {
		if(!httpMethod.equals("GET")) {
			getLogger().log(Level.WARNING, "405 \"" + httpMethod + "\" Not Allowed"); // => e.g. 405 "POST" Not Allowed
			
			session.sendStatus(HttpStatus.METHOD_NOT_ALLOWED); // send 405 status to client
			return; // session will automatically dispose after return
		}
		
		session.sendStatus(HttpStatus.OK); // send 200 status to client
		session.sendBody("Hello World"); // additional body
	}
}
```
