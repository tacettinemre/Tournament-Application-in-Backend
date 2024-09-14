package com.dreamgames.backendengineeringcasestudy.services;


import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import org.springframework.stereotype.Service;




@Service
public class GroupLeaderboardService {

    private final RedisTemplate<String, String> redisTemplate; // Add redisTemplate as a field
    private final ZSetOperations<String, String> zSetOperations;

    @Autowired
    public GroupLeaderboardService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate; // Initialize redisTemplate
        this.zSetOperations = redisTemplate.opsForZSet(); // Initialize zSetOperations
    }


    // Update the score for a user in the group's leaderboard
    public void updateUserScoreInGroup(Long groupId, Long userId, double score) {
        // Build the Redis key for the group's leaderboard
        String leaderboardKey = "group:leaderboard:" + groupId;
        
        // Set the score for the user in the group's leaderboard (not incrementing)
        zSetOperations.add(leaderboardKey, userId.toString(), score);
    }


    // Retrieve the real-time leaderboard for the group
    public Set<ZSetOperations.TypedTuple<String>> getGroupLeaderboard(Long groupId) {
        // Build the Redis key for the group's leaderboard
        String leaderboardKey = "group:leaderboard:" + groupId;
        
        // Get all users in the group ordered by their scores in descending order
        return zSetOperations.reverseRangeWithScores(leaderboardKey, 0, -1);
    }

    // Reset the leaderboard for the group (optional, for new tournaments)
    public void resetGroupLeaderboard(Long groupId) {
        // Build the Redis key for the group's leaderboard
        String leaderboardKey = "group:leaderboard:" + groupId;
        
        // Delete the leaderboard for the group
        redisTemplate.delete(leaderboardKey);
    }
}
