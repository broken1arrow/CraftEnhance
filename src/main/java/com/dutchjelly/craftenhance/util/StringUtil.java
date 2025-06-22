package com.dutchjelly.craftenhance.util;

import com.dutchjelly.craftenhance.updatechecking.VersionChecker;
import com.dutchjelly.craftenhance.updatechecking.VersionChecker.ServerVersion;

import java.util.Objects;
import java.util.StringJoiner;

import static com.dutchjelly.craftenhance.CraftEnhance.self;

public class StringUtil {

	static VersionChecker versionChecker = self().getVersionChecker();

	private StringUtil() {
	}

	public static CharSequence stripEnd(final String line, final String space) {
		try {
			return org.apache.commons.lang3.StringUtils.stripEnd(line, space);
		} catch (NoClassDefFoundError error) {
			return stripEndOld(line, space);
		}
	}

	public static String join(final Object[] params) {
		return join(params, (String) null);
	}

	public static String capitalizeFully(final String str) {
		try {
			if (versionChecker.newerThan(ServerVersion.v1_20))
				return org.apache.commons.lang3.text.WordUtils.capitalizeFully(str,'_').replace("_", " ");
			else return capitalizeFully(str, '_').replace("_", " ");
		} catch (NoClassDefFoundError error) {
			return capitalizeFully(str, '_').replace("_", " ");
		}
	}

	private static String stripEndOld(String str, String stripChars) {
		int end = length(str);
		if (end == 0) {
			return str;
		} else {
			if (stripChars == null) {
				while (end != 0 && Character.isWhitespace(str.charAt(end - 1))) {
					--end;
				}
			} else {
				if (stripChars.isEmpty()) {
					return str;
				}

				while (end != 0 && stripChars.indexOf(str.charAt(end - 1)) != -1) {
					--end;
				}
			}
			return str.substring(0, end);
		}
	}

	private static int length(CharSequence cs) {
		return cs == null ? 0 : cs.length();
	}

	private static String capitalizeFully(String str, char... delimiters) {
		int delimLen = delimiters == null ? -1 : delimiters.length;
		if (!isEmpty(str) && delimLen != 0) {
			str = str.toLowerCase();
			return capitalize(str, delimiters);
		} else {
			return str;
		}
	}

	private static String capitalize(String str, char... delimiters) {
		int delimLen = delimiters == null ? -1 : delimiters.length;
		if (!isEmpty(str) && delimLen != 0) {
			char[] buffer = str.toCharArray();
			boolean capitalizeNext = true;

			for (int i = 0; i < buffer.length; ++i) {
				char ch = buffer[i];
				if (isDelimiter(ch, delimiters)) {
					capitalizeNext = true;
				} else if (capitalizeNext) {
					buffer[i] = Character.toTitleCase(ch);
					capitalizeNext = false;
				}
			}

			return new String(buffer);
		} else {
			return str;
		}
	}

	private static boolean isDelimiter(char ch, char[] delimiters) {
		if (delimiters == null) {
			return Character.isWhitespace(ch);
		} else {
			char[] var2 = delimiters;
			int var3 = delimiters.length;

			for (int var4 = 0; var4 < var3; ++var4) {
				char delimiter = var2[var4];
				if (ch == delimiter) {
					return true;
				}
			}

			return false;
		}
	}

	private static String join(Object[] array, String delimiter) {
		return array == null ? null : join((Object[]) array, delimiter, 0, array.length);
	}

	private static String join(Object[] array, String delimiter, int startIndex, int endIndex) {
		if (array == null) {
			return null;
		} else if (endIndex - startIndex <= 0) {
			return "";
		} else {
			StringJoiner joiner = new StringJoiner(toStringOrEmpty(delimiter));

			for (int i = startIndex; i < endIndex; ++i) {
				joiner.add(toStringOrEmpty(array[i]));
			}

			return joiner.toString();
		}
	}

	private static boolean isEmpty(CharSequence cs) {
		return cs == null || cs.length() == 0;
	}

	private static String toStringOrEmpty(Object obj) {
		return Objects.toString(obj, "");
	}
}
