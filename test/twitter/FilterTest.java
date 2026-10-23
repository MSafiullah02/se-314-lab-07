/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FilterTest {

    /*
     * Testing strategy
     *
     * writtenBy():
     * - input list: empty, no matching author, one match, and multiple matches.
     * - author matching: exact case and different capitalization.
     * - output: contains only matches and preserves input order.
     *
     * inTimespan():
     * - timestamps: before, at the start, inside, at the end, and after interval.
     * - interval: ordinary interval and zero-length interval.
     * - input list: empty and mixed matching/nonmatching tweets.
     * - output order follows input order, not timestamp order.
     *
     * containing():
     * - search words: empty, one word, and multiple words (OR semantics).
     * - comparison: case-insensitive and exact whole-token matching.
     * - text: match at different token positions, extra spaces, punctuation,
     *   no matches, and an empty tweet list.
     * - matching multiple search words in one tweet still returns that tweet once.
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");
    private static final Instant d4 = Instant.parse("2016-02-17T13:00:00Z");

    private static final Tweet tweet1 =
            new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 =
            new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    private static final Tweet tweet3 =
            new Tweet(3, "ALYSSA", "Hello   WORLD", d3);
    private static final Tweet tweet4 =
            new Tweet(4, "charlie", "a talkative person says talk, not talk", d4);

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
        assert false;
    }

    // ---------- writtenBy() ----------

    @Test
    public void testWrittenByOneMatch() {
        List<Tweet> actual = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "alyssa");
        assertEquals(Arrays.asList(tweet1), actual);
    }

    @Test
    public void testWrittenByCaseInsensitiveAndPreservesOrder() {
        List<Tweet> input = Arrays.asList(tweet3, tweet2, tweet1);
        List<Tweet> actual = Filter.writtenBy(input, "AlYsSa");
        assertEquals(Arrays.asList(tweet3, tweet1), actual);
    }

    @Test
    public void testWrittenByNoMatches() {
        List<Tweet> actual = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "nobody");
        assertTrue("expected empty list", actual.isEmpty());
    }

    @Test
    public void testWrittenByEmptyList() {
        List<Tweet> actual = Filter.writtenBy(Collections.<Tweet>emptyList(), "alyssa");
        assertTrue("expected empty list", actual.isEmpty());
    }

    @Test
    public void testWrittenByReturnsAllMatchingTweetsInInputOrder() {
        List<Tweet> input = Arrays.asList(tweet1, tweet2, tweet3, tweet4);
        List<Tweet> actual = Filter.writtenBy(input, "ALYSSA");
        assertEquals(Arrays.asList(tweet1, tweet3), actual);
    }

    // ---------- inTimespan() ----------

    @Test
    public void testInTimespanIncludesBothEndpoints() {
        Timespan timespan = new Timespan(d1, d2);
        List<Tweet> actual = Filter.inTimespan(
                Arrays.asList(tweet1, tweet2, tweet3), timespan);
        assertEquals(Arrays.asList(tweet1, tweet2), actual);
    }

    @Test
    public void testInTimespanPreservesInputOrder() {
        Timespan timespan = new Timespan(d1, d3);
        List<Tweet> input = Arrays.asList(tweet3, tweet1, tweet2);
        List<Tweet> actual = Filter.inTimespan(input, timespan);
        assertEquals(Arrays.asList(tweet3, tweet1, tweet2), actual);
    }

    @Test
    public void testInTimespanZeroLengthInterval() {
        Timespan timespan = new Timespan(d2, d2);
        List<Tweet> actual = Filter.inTimespan(
                Arrays.asList(tweet1, tweet2, tweet3), timespan);
        assertEquals(Arrays.asList(tweet2), actual);
    }

    @Test
    public void testInTimespanNoMatches() {
        Timespan timespan = new Timespan(d3, d4);
        List<Tweet> actual = Filter.inTimespan(Arrays.asList(tweet1, tweet2), timespan);
        assertTrue("expected empty list", actual.isEmpty());
    }

    @Test
    public void testInTimespanEmptyList() {
        List<Tweet> actual = Filter.inTimespan(
                Collections.<Tweet>emptyList(), new Timespan(d1, d3));
        assertTrue("expected empty list", actual.isEmpty());
    }

    // ---------- containing() ----------

    @Test
    public void testContainingSingleWord() {
        List<Tweet> actual = Filter.containing(
                Arrays.asList(tweet1, tweet2, tweet3), Arrays.asList("talk"));
        assertEquals(Arrays.asList(tweet1, tweet2), actual);
    }

    @Test
    public void testContainingIsCaseInsensitive() {
        List<Tweet> actual = Filter.containing(
                Arrays.asList(tweet3), Arrays.asList("world"));
        assertEquals(Arrays.asList(tweet3), actual);
    }

    @Test
    public void testContainingMatchesAnySearchWordAndReturnsTweetOnlyOnce() {
        List<Tweet> actual = Filter.containing(
                Arrays.asList(tweet1, tweet2, tweet3),
                Arrays.asList("talk", "rivest", "WORLD"));
        assertEquals(Arrays.asList(tweet1, tweet2, tweet3), actual);
    }

    @Test
    public void testContainingUsesWholeWhitespaceDelimitedWords() {
        List<Tweet> actual = Filter.containing(
                Arrays.asList(tweet4), Arrays.asList("talk"));
        // "talkative" and "talk," are different words, but the final "talk" matches.
        assertEquals(Arrays.asList(tweet4), actual);

        Tweet onlyPartialMatches = new Tweet(5, "dana", "talkative person says talk,", d1);
        List<Tweet> noMatch = Filter.containing(
                Arrays.asList(onlyPartialMatches), Arrays.asList("talk"));
        assertTrue("partial tokens should not match", noMatch.isEmpty());
    }

    @Test
    public void testContainingEmptySearchWords() {
        List<Tweet> actual = Filter.containing(
                Arrays.asList(tweet1, tweet2), Collections.<String>emptyList());
        assertTrue("expected empty list", actual.isEmpty());
    }

    @Test
    public void testContainingEmptyTweetList() {
        List<Tweet> actual = Filter.containing(
                Collections.<Tweet>emptyList(), Arrays.asList("talk"));
        assertTrue("expected empty list", actual.isEmpty());
    }

    @Test
    public void testContainingNoMatches() {
        List<Tweet> actual = Filter.containing(
                Arrays.asList(tweet1, tweet2), Arrays.asList("nonexistent"));
        assertTrue("expected empty list", actual.isEmpty());
    }
}