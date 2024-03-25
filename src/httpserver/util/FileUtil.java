package httpserver.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import httpserver.server.HttpStatus;
import httpserver.server.Session;

public class FileUtil {
	/**
	 * replace all path-related special characters with underscores
	 * for example: public_folder/../confidential_file -> public_folder/_/confidential_file
	 * this should prevent leaking of possibly confidential content and should always be applied to user input
	 */
	public static String sanitize(String path) {
		return path.replaceAll("([^a-zA-Z0-9.\\-\\/]|\\.\\.)", "_");
	}
	
	/**
	 * convenience method, same as <code> loadTemplate(file, null) </code>
	 * @return contents of the file without rendering template-variables
	 */
	public static Optional<String> read(String file) {
		return loadTemplate(file, null);
	}
	
	/**
	 * load content of a file template and render template-variables
	 */
	public static Optional<String> loadTemplate(String file, Map<String, Supplier<String>> vars) {
		String contents;
		try {
			var reader = new FileReader(file);
		
			contents = "";
			while(reader.ready())
				contents += (char) reader.read();
			reader.close();
			
			if(vars != null)
				for(var var : vars.entrySet()) {
					if(contents.contains("${" + var.getKey() + "}"))
						contents = contents.replaceAll("\\$\\{" + var.getKey() + "\\}", var.getValue().get());
			}
		} catch(IOException e) {
			return Optional.empty();
		}
		
		return Optional.of(contents);
	}
	/**
	 * render template-variables on the content of the template
	 */
	public static String renderTemplate(String content, Map<String, Supplier<String>> vars) {
		if(vars != null)
			for(var var : vars.entrySet()) {
				content = content.replaceAll("\\$\\{" + var.getKey() + "\\}", var.getValue().get());
		}
		return content;
	}
	
	public static void sendFile(String file, Session session) throws IOException {
		sendFile(file, Map.of(), session);
	}
	public static void sendFile(String file, Map<String,String> headers, Session session) throws IOException {
		var stream = new FileInputStream(new File(file));
		var n = stream.available();
		var buf = new byte[4096];
		session.sendStatus(HttpStatus.OK);
		session.sendHeader("Content-Length", "" + n);
		for(var header : headers.entrySet())
			session.sendHeader(header.getKey(), header.getValue());
		while((n = stream.read(buf)) >= 0) {
			session.sendBody(buf, 0, n);
		}
		stream.close();
	}
}
