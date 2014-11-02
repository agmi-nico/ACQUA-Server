package fr.inria.acqua;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Log {
	static Calendar c = Calendar.getInstance(Locale.getDefault());;
	static SimpleDateFormat dateFormat = new SimpleDateFormat(
			"dd/MM/yyyy HH:mm:ss");

	public static String now() {
		c.setTimeInMillis(System.currentTimeMillis());
		return dateFormat.format(c.getTime());
	}

	public static void e(String msg) {
		System.err.println(now() + " " + msg);
	}

	public static void d(String msg) {
		System.out.println(now() + " " + msg);
	}
}