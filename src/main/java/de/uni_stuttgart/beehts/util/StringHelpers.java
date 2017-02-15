package de.uni_stuttgart.beehts.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class StringHelpers {

	public static void fillToNCharacters(StringBuilder sb, int n, char fill) {
		while (sb.length() < n) {
			sb.append(fill);
		}
	}

	public static void fillToEqualLength(char fill, StringBuilder... builders) {
		int n = Arrays.stream(builders).mapToInt(sb -> sb.length()).max().getAsInt();
		Arrays.stream(builders).forEach(sb -> fillToNCharacters(sb, n, fill));
	}

	public static String fillToNCharacters(String s, int n, char fill) {
		return s + new String(new char[n - s.length()]).replace('\0', fill);
	}

	public static String[] fillToEqualLength(char fill, String... strings) {
		int n = Arrays.stream(strings).mapToInt(s -> s.length()).max().getAsInt();
		return Arrays.stream(strings).map(s -> fillToNCharacters(s, n, fill)).collect(Collectors.toList())
				.toArray(strings);
	}
}
