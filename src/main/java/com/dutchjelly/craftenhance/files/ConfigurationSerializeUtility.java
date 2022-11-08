package com.dutchjelly.craftenhance.files;

import java.util.Map;

/**
 * Represents an object that may be serialized.
 * <p>
 * These objects MUST implement one of the following, in addition to the
 * methods as defined by this interface:
 * <ul>
 * <li>A static method "deserialize" that accepts a single {@link java.util.Map}&lt;
 * {@link String}, {@link Object}&gt; and returns the class.</li>
 * <li>A static method "valueOf" that accepts a single {@link java.util.Map}&lt;{@link
 * String}, {@link Object}&gt; and returns the class.</li>
 * </ul>
 */
public interface ConfigurationSerializeUtility {

	Map<String, Object> serialize();

}
