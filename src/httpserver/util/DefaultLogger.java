package httpserver.util;

import static httpserver.util.ColorUtil.rst;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.UnknownFormatConversionException;

import httpserver.util.ColorUtil.Severity;

public class DefaultLogger implements Logger {
	private final String name;
	private OutputStream lowlevelOutput;
	private Level level;
	
	public DefaultLogger(String name) {
		this.name = name;
		level = Level.ALL;
	}
	
	public void setLevel(Level level) {
		this.level = level;
	}
	public Level getLevel() {
		return level;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isLoggable(Level level) {
		return true;
	}
	
	public void setLowlevelOutput(OutputStream output) {
		lowlevelOutput = output;
	}
	public void setLowlevelOutput(FileOutputStream output) {
		lowlevelOutput = output;
	}
	public void setLowlevelOutput(File output) throws FileNotFoundException {
		lowlevelOutput = new FileOutputStream(output);
	}
	
	@Override
	public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
		if(level.compareTo(this.level) < 0) return;
		System.out.println(Severity.from(level) + "[" + level + "] " + rst() + new Date() + ": " + sanitize(msg));
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String format, Object... params) {
		if(level.compareTo(this.level) < 0) {
			if(lowlevelOutput == null) return;
			
			byte[] data;
			try {
				data = String.format("%s[" + level + "] " + rst() + new Date() + ": " + sanitize(format), Severity.from(level), params).getBytes();
			} catch(UnknownFormatConversionException e) {
				data = (Severity.from(level) + "[" + level + "] " + rst() + new Date() + ": " + sanitize(format)).getBytes();
			}
			try {
				lowlevelOutput.write(data);
				lowlevelOutput.write('\n');
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		try {
			System.out.println(String.format("%s[" + level + "] " + rst() + new Date() + ": " + sanitize(format), Severity.from(level), params));
		} catch(UnknownFormatConversionException e) {
			System.out.println(Severity.from(level) + "[" + level + "] " + rst() + new Date() + ": " + sanitize(format));
		}
	}

	private static String sanitize(String text) {
		var builder = new StringBuilder();
		for(var c : text.toCharArray()) {
			if((c < 0x20 || c == '%') && c != '\033') {
				builder.append("\\0x" + Integer.toHexString(c));
			} else
				builder.append(c);
		}
		return builder.toString();
	}
}
