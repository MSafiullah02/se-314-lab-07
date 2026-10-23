package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * Tests for SocialNetwork.
 *
 * Testing strategy:
 *
 * guessFollowsGraph():
 * - Input size: empty list and one or more tweets.
 * - Tweet text: no mentions, one mention, and multiple mentions.
 * - Repeated evidence: mentions across multiple tweets from one author.
 * - Usernames: case-insensitive matching and repeated mentions.
 * - Self-mentions: a user must not follow themselves.
 * - Underdetermined postcondition: assert required edges and avoid requiring
 *   optional graph keys or other implementation details.
 *
 * influencers():
 * - Graph size: empty graph and graphs with users.
 * - Influence: one influencer and different follower counts.
 * - Ties: verify tied users are present without requiring their relative order.
 * - Distinct users: include users appearing only as values, not just keys.
 *
 * Note: the provided SocialNetwork.java specification says influencers()
 * returns all distinct usernames in the graph. Therefore, a graph containing
 * one key mapped to an empty set returns that username with influence zero.
 */
public class SocialNetworkTest {

    private static final Instant TIME =
            Instant.parse("2016-02-17T10:00:00Z");

    private Tweet tweet(long id, String author, String text) {
        return new Tweet(id, author, text, TIME);
    }

    @Test(expected = AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // Run JUnit with assertions enabled (-ea).
    }

    // ---------- guessFollowsGraph() ----------

    @Test
    public void testGuessFollowsGraphEmptyTweets() {
        Map<String, Set<String>> graph =
                SocialNetwork.guessFollowsGraph(Collections.<Tweet>emptyList());

        assertTrue(graph.isEmpty());
    }

    @Test
    public void testTweetsWithoutMentionsAddNoFollowEdges() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "Alice", "Hello world"));

        Map<String, Set<String>> graph =
                SocialNetwork.guessFollowsGraph(tweets);

        // The specification permits the author to be absent or mapped to an
        // empty set when they follow nobody.
        for (Set<String> follows : graph.values()) {
            assertTrue(follows.isEmpty());
        }
    }

    @Test
    public void testSingleMention() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "Alice", "Hello @Bob"));

        Map<String, Set<String>> graph =
                SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(hasFollowEdge(graph, "alice", "bob"));
    }

    @Test
    public void testMultipleMentions() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "Alice", "Hello @Bob and @Carol"));

        Map<String, Set<String>> graph =
                SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(hasFollowEdge(graph, "alice", "bob"));
        assertTrue(hasFollowEdge(graph, "alice", "carol"));
    }

    @Test
    public void testMultipleTweetsFromOneUser() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "Alice", "Hello @Bob"),
                tweet(2, "ALICE", "Hello @Carol"),
                tweet(3, "alice", "Again @Bob"));

        Map<String, Set<String>> graph =
                SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(hasFollowEdge(graph, "alice", "bob"));
        assertTrue(hasFollowEdge(graph, "alice", "carol"));
        assertEquals(2, graph.get("alice").size());
    }

    @Test
    public void testMentionsAreCaseInsensitiveAndUnique() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "ALICE", "@Bob @BOB @bob"));

        Map<String, Set<String>> graph =
                SocialNetwork.guessFollowsGraph(tweets);

        assertTrue(hasFollowEdge(graph, "alice", "bob"));
        assertEquals(1, graph.get("alice").size());
    }

    @Test
    public void testUserDoesNotFollowThemself() {
        List<Tweet> tweets = Arrays.asList(
                tweet(1, "Alice", "Talking to @alice"));

        Map<String, Set<String>> graph =
                SocialNetwork.guessFollowsGraph(tweets);

        assertFalse(hasFollowEdge(graph, "alice", "alice"));
    }

    private boolean hasFollowEdge(Map<String, Set<String>> graph,
                                  String follower, String followed) {
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(follower)) {
                for (String username : entry.getValue()) {
                    if (username.equalsIgnoreCase(followed)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ---------- influencers() ----------

    @Test
    public void testInfluencersEmptyGraph() {
        List<String> result =
                SocialNetwork.influencers(new HashMap<String, Set<String>>());

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSingleUserWithoutFollowersIsIncludedAtZeroInfluence() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("alice", new HashSet<String>());

        // The supplied SocialNetwork.java contract says "all distinct"
        // usernames in the graph are returned, including zero-follower users.
        assertEquals(Collections.singletonList("alice"),
                SocialNetwork.influencers(graph));
    }

    @Test
    public void testSingleInfluencer() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("alice", new HashSet<>(Arrays.asList("bob")));
        graph.put("carol", new HashSet<>(Arrays.asList("bob")));

        List<String> result = SocialNetwork.influencers(graph);

        assertEquals(3, result.size());
        assertEquals("bob", result.get(0));
        assertTrue(result.contains("alice"));
        assertTrue(result.contains("carol"));
    }

    @Test
    public void testMultipleInfluencersSortedByFollowerCount() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("alice", new HashSet<>(Arrays.asList("bob", "carol")));
        graph.put("dave", new HashSet<>(Arrays.asList("bob")));
        graph.put("erin", new HashSet<>(Arrays.asList("bob", "carol")));

        List<String> result = SocialNetwork.influencers(graph);

        assertEquals(5, result.size());
        assertEquals("bob", result.get(0));
        assertEquals("carol", result.get(1));
        assertTrue(result.contains("alice"));
        assertTrue(result.contains("dave"));
        assertTrue(result.contains("erin"));
    }

    @Test
    public void testTiedInfluenceDoesNotRequireSpecificOrder() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("alice", new HashSet<>(Arrays.asList("bob")));
        graph.put("carol", new HashSet<>(Arrays.asList("dave")));

        List<String> result = SocialNetwork.influencers(graph);

        assertEquals(4, result.size());
        assertTrue(result.contains("alice"));
        assertTrue(result.contains("bob"));
        assertTrue(result.contains("carol"));
        assertTrue(result.contains("dave"));
    }

    @Test
    public void testUserAppearingOnlyAsMentionIsIncluded() {
        Map<String, Set<String>> graph = new HashMap<>();
        graph.put("alice", new HashSet<>(Arrays.asList("bob")));

        List<String> result = SocialNetwork.influencers(graph);

        assertEquals(2, result.size());
        assertEquals("bob", result.get(0));
        assertTrue(result.contains("alice"));
    }
}
