
/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract consists of methods that extract information from a list of tweets.
 */
public class Extract {

    /**
     * Get the time period spanned by tweets.
     *
     * @param tweets list of tweets with distinct ids, not modified by this method.
     * @return a minimum-length time interval that contains the timestamp of
     *         every tweet in the list.
     */
    public static Timespan getTimespan(List<Tweet> tweets) {
        Instant start = null;
        Instant end = null;

        for (Tweet tweet : tweets) {
            Instant timestamp = tweet.getTimestamp();

            if (start == null || timestamp.isBefore(start)) {
                start = timestamp;
            }

            if (end == null || timestamp.isAfter(end)) {
                end = timestamp;
            }
        }

        // With no tweets, any zero-length interval is valid.
        if (start == null) {
            start = Instant.EPOCH;
            end = Instant.EPOCH;
        }

        return new Timespan(start, end);
    }

    /*
     * Match a username preceded by a character that is not part of a Twitter
     * username, or by the beginning of the text.
     *
     * Valid username characters are ASCII letters, digits, underscore, and
     * hyphen. The trailing boundary prevents partial username matches.
     */
    private static final Pattern MENTION_PATTERN =
            Pattern.compile("(?<![A-Za-z0-9_-])@([A-Za-z0-9_-]+)(?![A-Za-z0-9_-])");

    /**
     * Get usernames mentioned in a list of tweets.
     *
     * @param tweets list of tweets with distinct ids, not modified by this method.
     * @return the set of usernames mentioned in the tweet text, with each
     *         username included at most once, ignoring case.
     */
    public static Set<String> getMentionedUsers(List<Tweet> tweets) {
        Set<String> mentionedUsers = new HashSet<>();

        for (Tweet tweet : tweets) {
            Matcher matcher = MENTION_PATTERN.matcher(tweet.getText());

            while (matcher.find()) {
                // Normalize capitalization so case variants count as one user.
                String username = matcher.group(1).toLowerCase(Locale.ROOT);
                mentionedUsers.add(username);
            }
        }

        return mentionedUsers;
    }
}