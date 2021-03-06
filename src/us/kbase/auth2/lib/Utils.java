package us.kbase.auth2.lib;

import java.util.Date;

import us.kbase.auth2.lib.exceptions.MissingParameterException;

public class Utils {

	public static void checkString(final String s, final String name)
			throws MissingParameterException {
		if (s == null || s.isEmpty()) {
			throw new MissingParameterException("Missing parameter: " + name);
		}
	}
	
	public static void checkString(
			final String s,
			final String name,
			final boolean argexcept) {
		if (s == null || s.isEmpty()) {
			throw new IllegalArgumentException("Missing argument: " + name);
		}
	}
	
	public static long dateToSec(final Date date) {
		if (date == null) {
			throw new NullPointerException("date");
		}
		return (long) Math.floor(date.getTime() / 1000.0);
	}

	// prevents overflows by returning max long if a + b > maxlong
	public static long addLong(final long a, final long b) {
		final long c;
		if (Long.MAX_VALUE - a < b) {
			c = Long.MAX_VALUE;
		} else {
			c = a + b;
		}
		return c;
	}
}
