package util;

import static util.ColorUtil.rst;

import java.lang.System.Logger;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.UnknownFormatConversionException;

import util.ColorUtil.Severity;

public class DefaultLogger implements Logger {
	private final String name;
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
	
	@Override
	public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
		if(level.compareTo(this.level) < 0) return;
		System.out.println(Severity.from(level) + "[" + level + "] " + rst() + new Date() + ": " + sanitize(msg));
	}

	@Override
	public void log(Level level, ResourceBundle bundle, String format, Object... params) {
		if(level.compareTo(this.level) < 0) return;
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
