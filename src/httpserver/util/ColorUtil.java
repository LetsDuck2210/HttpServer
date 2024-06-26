package httpserver.util;

import java.lang.System.Logger.Level;
import java.net.InetAddress;
import java.text.MessageFormat;

public class ColorUtil {
	public static String rst() {
		return "\033[0m";
	}
	public static enum Prefix {
		FOREGROUND(38), BACKGROUND(48);
		
		private int code;
		private Prefix(int code) {
			this.code = code;
		}
		public int code() {
			return code;
		}
	}
	public static enum Severity {
		INFO("\033[34m"),
		WARNING("\033[33m"),
		ERR("\033[31m"),
		DEBUG("\033[38;5;69m"),
		TRACE("\033[38;5;208m"),
		GENERIC("\033[37m");
		
		private String c;
		private Severity(String c) {
			this.c = c;
		}
		public String toString() {
			return c;
		}
		
		public static Severity from(Level level) {
			try {
				var s = valueOf(level.name());
				if(s == null)
					return GENERIC;
				return s;
			} catch(IllegalArgumentException e) { 
				return GENERIC; 
			}
		}
	}
	
	public static String fromIP(InetAddress addr, Prefix ground) {
		var byteAddr = addr.getAddress();
		var avg = 0;
		for(var b : byteAddr) {
			avg += b;
		}
		avg /= byteAddr.length;
		
		return "\033[" + ground.code() + ";5;" + (int) (avg + 128)+ "m";
	}
	public static String fromIP(InetAddress addr) {
		var byteAddr = addr.getAddress();
		var avg = 0;
		for(var b : byteAddr) {
			avg += b;
		}
		avg /= byteAddr.length;
		
		return MessageFormat.format("\033[{0};5;{1}m\033[{2};5;{3}m", Prefix.BACKGROUND.code(), (int) (avg+128), Prefix.FOREGROUND.code(), 255 - (int) (avg+128));
	}
}
