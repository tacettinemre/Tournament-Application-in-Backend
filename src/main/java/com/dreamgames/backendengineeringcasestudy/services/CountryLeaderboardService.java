package com.dreamgames.backendengineeringcasestudy.services;


import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;


@Service
public class CountryLeaderboardService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private ZSetOperations<String, String> zSetOperations;

    private static final String COUNTRY_LEADERBOARD_KEY = "country:leaderboard";
    private static final List<String> INITIAL_COUNTRIES = Arrays.asList("Turkey", "Germany", "USA", "France", "United Kingdom");

    @PostConstruct
    public void init() {
        zSetOperations = redisTemplate.opsForZSet();
    }

    // Initialize the leaderboard with countries having score 0
    public void initializeLeaderboard() {
        for (String country : INITIAL_COUNTRIES) {
            if (zSetOperations.score(COUNTRY_LEADERBOARD_KEY, country) == null) {
                zSetOperations.add(COUNTRY_LEADERBOARD_KEY, country, 0);
            }
        }
    }

    // Delete the leaderboard at the end of the tournament
    public void deleteLeaderboard() {
        redisTemplate.delete(COUNTRY_LEADERBOARD_KEY);
    }

    // Update the total score for the country
    public void updateCountryScore(String country) {
        zSetOperations.incrementScore(COUNTRY_LEADERBOARD_KEY, country, 1);
    }

    // Retrieve the whole leaderboard (all 5 countries) along with their scores
    public Set<ZSetOperations.TypedTuple<String>> getCountryLeaderboard() {
        return zSetOperations.reverseRangeWithScores(COUNTRY_LEADERBOARD_KEY, 0, -1);
    }
}

