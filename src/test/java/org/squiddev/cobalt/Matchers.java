package org.squiddev.cobalt;

import org.hamcrest.Matcher;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.Matchers.*;

public class Matchers {
	public static Matcher<Integer> between(int min, int max) {
		return both(greaterThanOrEqualTo(min)).and(lessThanOrEqualTo(max));
	}

	public static Matcher<Integer> betweenExclusive(int min, int max) {
		return both(greaterThan(min)).and(lessThan(max));
	}
}
