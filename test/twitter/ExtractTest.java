
/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

public class ExtractTest {

    /*
     * Testing strategy
     *
     * getTimespan():
     * - List size: empty, one tweet, multiple tweets.
     * - Timestamps: identical or distinct.
     * - Input order: chronological and unordered.
     * - Minimum and maximum timestamps may occur at different positions.
     *
     * getMentionedUsers():
     * - Mentions: none, one, multiple, and repeated.
     * - Username boundaries: beginning of text, whitespace, and punctuation.
     * - Invalid boundaries: an @ preceded by a username character, including
     *   email addresses.
     * - Username characters: letters, digits, underscore, and hyphen.
     * - Capitalization: repeated mentions with different capitalization.
     *
     * Username assertions ignore capitalization, because the specification
     * does not require a particular capitalization in the returned set.
     */

    private static final Instant d1 =
            Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 =
            Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 =
            Instant.parse("2016-02-17T12:00:00Z");

    private static final Tweet tweet1 =
            new Tweet(1, "alyssa",
                    "is it reasonable to talk about rivest so much?", d1);

    private static final Tweet tweet2 =
            new Tweet(2, "bbitdiddle",
                    "rivest talk in 30 minutes #hype", d2);

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
        assert false;
    }

    // ---------- getTimespan tests ----------

    @Test
    public void testGetTimespanTwoTweets() {
        Timespan timespan =
                Extract.getTimespan(Arrays.asList(tweet1, tweet2));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    @Test
    public void testGetTimespanSingleTweet() {
        Timespan timespan =
                Extract.getTimespan(Arrays.asList(tweet1));

        assertEquals("start should equal tweet timestamp",
                d1, timespan.getStart());
        assertEquals("end should equal tweet timestamp",
                d1, timespan.getEnd());
    }

    @Test
    public void testGetTimespanUnorderedTweets() {
        Tweet tweet3 =
                new Tweet(3, "charlie", "another tweet", d3);

        // The earliest and latest timestamps are not determined by list order.
        Timespan timespan =
                Extract.getTimespan(Arrays.asList(tweet2, tweet1, tweet3));

        assertEquals("expected earliest timestamp",
                d1, timespan.getStart());
        assertEquals("expected latest timestamp",
                d3, timespan.getEnd());
    }

    @Test
    public void testGetTimespanIdenticalTimestamps() {
        Tweet first =
                new Tweet(10, "alice", "first", d2);
        Tweet second =
                new Tweet(11, "bob", "second", d2);

        Timespan timespan =
                Extract.getTimespan(Arrays.asList(first, second));

        assertEquals("start should equal the common timestamp",
                d2, timespan.getStart());
        assertEquals("end should equal the common timestamp",
                d2, timespan.getEnd());
    }

    @Test
    public void testGetTimespanEmptyList() {
        Timespan timespan =
                Extract.getTimespan(Arrays.asList());

        // With no tweets, a zero-length interval is a valid result.
        assertEquals("expected a zero-length interval",
                timespan.getStart(), timespan.getEnd());
    }

    // ---------- getMentionedUsers tests ----------

    @Test
    public void testGetMentionedUsersNoMention() {
        Set<String> mentionedUsers =
                Extract.getMentionedUsers(Arrays.asList(tweet1));

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    @Test
    public void testGetMentionedUsersEmptyTweetList() {
        Set<String> mentionedUsers =
                Extract.getMentionedUsers(Arrays.asList());

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    @Test
    public void testGetMentionedUsersMultipleMentions() {
        Tweet tweet = new Tweet(
                20,
                "writer",
                "@Alice, (@bob-2)! @under_score9.",
                d1);

        Set<String> actual =
                Extract.getMentionedUsers(Arrays.asList(tweet));

        Set<String> expected =
                new HashSet<>(Arrays.asList(
                        "alice", "bob-2", "under_score9"));

        assertMentionedUsers(expected, actual);
    }

    @Test
    public void testGetMentionedUsersIgnoresEmailAndEmbeddedAtSign() {
        Tweet tweet = new Tweet(
                21,
                "writer",
                "Contact bitdiddle@mit.edu or use foo@bar",
                d1);

        Set<String> actual =
                Extract.getMentionedUsers(Arrays.asList(tweet));

        assertTrue("email addresses and embedded @ signs are not mentions",
                actual.isEmpty());
    }

    @Test
    public void testGetMentionedUsersRepeatedCaseInsensitiveMentions() {
        Tweet tweet = new Tweet(
                22,
                "writer",
                "@Alice @ALICE @bob @BoB",
                d1);

        Set<String> actual =
                Extract.getMentionedUsers(Arrays.asList(tweet));

        Set<String> expected =
                new HashSet<>(Arrays.asList("alice", "bob"));

        assertMentionedUsers(expected, actual);
    }

    @Test
    public void testGetMentionedUsersAcrossMultipleTweets() {
        Tweet first = new Tweet(
                23, "writer", "Hello @Alice", d1);
        Tweet second = new Tweet(
                24, "writer2", "Hello @bob and @alice", d2);

        Set<String> actual =
                Extract.getMentionedUsers(Arrays.asList(first, second));

        Set<String> expected =
                new HashSet<>(Arrays.asList("alice", "bob"));

        assertMentionedUsers(expected, actual);
    }

    /*
     * Compare sets without requiring a specific capitalization.
     * Also verify that case variants of the same username do not appear
     * as separate entries in the returned set.
     */
    private static void assertMentionedUsers(
            Set<String> expectedLowercase, Set<String> actual) {

        assertEquals("expected one entry per distinct username",
                expectedLowercase.size(), actual.size());

        Set<String> actualLowercase = new HashSet<>();
        for (String username : actual) {
            actualLowercase.add(username.toLowerCase(Locale.ROOT));
        }

        assertEquals("unexpected mentioned usernames",
                expectedLowercase, actualLowercase);

        assertEquals("duplicate usernames differing only by case",
                actual.size(), actualLowercase.size());
    }
}