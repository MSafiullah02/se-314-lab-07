/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SocialNetwork provides methods that operate on a social network.
 *
 * A social network is represented by a Map<String, Set<String>> where map[A]
 * is the set of people that person A follows on Twitter.
 */
public class SocialNetwork {

    /**
     * Guess who might follow whom, from evidence found in tweets.
     *
     * Required evidence: if author A @-mentions B in a tweet, infer that A
     * follows B. Usernames are case-insensitive, and users cannot follow
     * themselves.
     *
     * @param tweets tweets providing evidence; this method does not modify them
     * @return an inferred follows graph
     */
    public static Map<String, Set<String>> guessFollowsGraph(List<Tweet> tweets) {
        Map<String, Set<String>> followsGraph = new HashMap<>();

        for (Tweet tweet : tweets) {
            String author = tweet.getAuthor().toLowerCase(Locale.ROOT);

            // It is valid for a user with no outgoing follows to have an empty
            // set, so keeping each author as a key is a valid representation.
            followsGraph.computeIfAbsent(author, k -> new HashSet<>());

            Set<String> mentionedUsers = Extract.getMentionedUsers(
                    Collections.singletonList(tweet));

            for (String mentionedUser : mentionedUsers) {
                String mentioned = mentionedUser.toLowerCase(Locale.ROOT);

                // The SocialNetwork specification says users cannot follow
                // themselves.
                if (!author.equals(mentioned)) {
                    followsGraph.get(author).add(mentioned);
                }
            }
        }

        return followsGraph;
    }

    /**
     * Find all distinct users in the graph, ordered by descending follower
     * count. A user has one follower for each distinct account whose follow set
     * contains that user.
     *
     * @param followsGraph social network to analyze
     * @return all distinct usernames in descending order of follower count
     */
    public static List<String> influencers(Map<String, Set<String>> followsGraph) {
        Map<String, Integer> followerCounts = new HashMap<>();
        Set<String> allUsers = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : followsGraph.entrySet()) {
            String follower = entry.getKey().toLowerCase(Locale.ROOT);
            allUsers.add(follower);
            followerCounts.putIfAbsent(follower, 0);

            for (String followedUser : entry.getValue()) {
                String followed = followedUser.toLowerCase(Locale.ROOT);
                allUsers.add(followed);
                followerCounts.put(followed,
                        followerCounts.getOrDefault(followed, 0) + 1);
            }
        }

        List<String> result = new ArrayList<>(allUsers);
        result.sort(new Comparator<String>() {
            @Override
            public int compare(String first, String second) {
                int byFollowers = Integer.compare(
                        followerCounts.getOrDefault(second, 0),
                        followerCounts.getOrDefault(first, 0));
                if (byFollowers != 0) {
                    return byFollowers;
                }
                // A deterministic tie-break is allowed; tests should not
                // depend on a particular order for equally influential users.
                return first.compareTo(second);
            }
        });

        return result;
    }
}
