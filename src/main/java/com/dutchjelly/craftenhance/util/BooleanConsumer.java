package com.dutchjelly.craftenhance.util;

@FunctionalInterface
public interface BooleanConsumer {

	/**
	 * Performs this operation on the given argument.
	 *
	 * @param valid if it did successfully do the task.
	 */
	void accept(boolean valid);
}