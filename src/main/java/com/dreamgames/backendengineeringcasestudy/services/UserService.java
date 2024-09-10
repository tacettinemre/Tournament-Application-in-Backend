package com.dreamgames.backendengineeringcasestudy.services;

import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dreamgames.backendengineeringcasestudy.models.User;
import com.dreamgames.backendengineeringcasestudy.repositories.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Updated createUser method: no longer requires a username, default values are assigned
    public User createUser() {
        User user = new User();
        user.setCoins(5000);  // Default coins
        user.setLevel(1);     // Default level
        user.setCountry(randomCountry());  // Randomly assign a country
        return userRepository.save(user);
    }

    // Method for updating a user's level and coins
    public User updateLevel(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            int currentLevel = user.getLevel();
            int newLevel = currentLevel + 1; 
            int coinsEarned = 25;  // Coins awarded per level

            user.setLevel(newLevel);
            user.setCoins(user.getCoins() + coinsEarned);

            if (user.getGroup() != null && "active".equals(user.getGroup().getGroupStatus())) {
                user.setScore(user.getScore() + 1);
            }

            return userRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    // Utility method to randomly assign a country
    private String randomCountry() {
        String[] countries = {"Turkey", "United States", "United Kingdom", "France", "Germany"};
        return countries[new Random().nextInt(countries.length)];
    }

    public User claimReward(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("User not found");
        }

        User user = optionalUser.get();

        // Use a rule-based switch statement to check the hasReward field
        switch (user.getHasReward()) {
            case 1 -> {
                // First place reward: 10,000 coins
                user.setCoins(user.getCoins() + 10000);
                user.setHasReward(0);  // Reset the hasReward field after claiming
            }
            case 2 -> {
                // Second place reward: 5,000 coins
                user.setCoins(user.getCoins() + 5000);
                user.setHasReward(0);  // Reset the hasReward field after claiming
            }
            case 0 -> throw new RuntimeException("No rewards to claim.");
            default -> throw new RuntimeException("Invalid reward status.");
        }

        return userRepository.save(user);  // Save the user with updated coins and hasReward field
    }
}
