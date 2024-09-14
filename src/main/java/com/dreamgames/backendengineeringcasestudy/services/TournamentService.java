package com.dreamgames.backendengineeringcasestudy.services;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.dreamgames.backendengineeringcasestudy.exception.CustomAppException;
import com.dreamgames.backendengineeringcasestudy.models.Group;
import com.dreamgames.backendengineeringcasestudy.models.Tournament;
import com.dreamgames.backendengineeringcasestudy.models.User;
import com.dreamgames.backendengineeringcasestudy.repositories.GroupRepository;
import com.dreamgames.backendengineeringcasestudy.repositories.TournamentRepository;
import com.dreamgames.backendengineeringcasestudy.repositories.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class TournamentService {

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private CountryLeaderboardService countryLeaderboardService;

    @Autowired
    private GroupLeaderboardService groupLeaderboardService;

    private void resetUsersAfterTournament() {
        // Assuming you have a UserRepository to interact with users
        userRepository.resetGroupIdAndScore();
    }

private void assignRewardsToTopUsersInGroups(Tournament tournament) {
    // Get all groups in the tournament
    List<Group> groups = groupRepository.findByTournamentId(tournament.getId());

    // Iterate through each group and assign rewards to the top two users
    for (Group group : groups) {
        // Retrieve the real-time leaderboard from Redis
        Set<ZSetOperations.TypedTuple<String>> redisLeaderboard = groupLeaderboardService.getGroupLeaderboard(group.getGroupId());

        // Convert the Redis leaderboard into a list of user IDs (and scores if needed)
        List<Long> userIdsInGroup = new ArrayList<>();
        
        for (ZSetOperations.TypedTuple<String> entry : redisLeaderboard) {
            Long userId = Long.valueOf(entry.getValue());
            userIdsInGroup.add(userId);  // Add userId to the list
        }

        // Ensure the group has exactly five users in the leaderboard
        if (userIdsInGroup.size() == 5) {
            // First place: 10,000 coins
            Long firstPlaceUserId = userIdsInGroup.get(0);  // Get first user's ID
            Optional<User> firstPlaceUserOptional = userRepository.findById(firstPlaceUserId);
            if (firstPlaceUserOptional.isPresent()) {
                User firstPlaceUser = firstPlaceUserOptional.get();
                firstPlaceUser.setHasReward(1);  // Set reward for the first place
                userRepository.save(firstPlaceUser);
            }

            // Second place: 5,000 coins
            Long secondPlaceUserId = userIdsInGroup.get(1);  // Get second user's ID
            Optional<User> secondPlaceUserOptional = userRepository.findById(secondPlaceUserId);
            if (secondPlaceUserOptional.isPresent()) {
                User secondPlaceUser = secondPlaceUserOptional.get();
                secondPlaceUser.setHasReward(2);  // Set reward for the second place
                userRepository.save(secondPlaceUser);
            }
        }
    }
}

    private void completeGroupsForTournament(Tournament tournament) {
        List<Group> groups = groupRepository.findByTournamentId(tournament.getId());
        for (Group group : groups) {
            group.setGroupStatus("completed");
            groupRepository.save(group);  // Save the updated group status
        }
    }
    
    // Starts a new tournament at 00:00 UTC daily
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")  // Runs at 00:00 UTC every day
    public void startNewTournament() {
        // End any active tournament
        endCurrentTournament();

        // Create and start a new tournament
        Tournament newTournament = new Tournament();
        newTournament.setActive(true);
        tournamentRepository.save(newTournament);

        countryLeaderboardService.initializeLeaderboard();
        // Reset all group leaderboards at the start of the tournament
        resetAllGroupLeaderboards();
    }

    // Ends the current tournament at 20:00 UTC daily
    @Scheduled(cron = "0 0 20 * * *", zone = "UTC")  // Runs at 20:00 UTC every day
    public void endCurrentTournament() {
        Optional<Tournament> activeTournament = tournamentRepository.findActiveTournament();
        if (activeTournament.isPresent()) {
            Tournament tournament = activeTournament.get();
            tournament.setActive(false);
            tournamentRepository.save(tournament);

            // Complete groups for the current tournament
            completeGroupsForTournament(tournament);

            // Assign rewards to the top users in the groups
            assignRewardsToTopUsersInGroups(tournament);

            // Reset the users after the tournament
            resetUsersAfterTournament();

            // Reset all group leaderboards at the end of the tournament
            resetAllGroupLeaderboards();
            countryLeaderboardService.deleteLeaderboard();
        }
    }

    // Helper method to reset all group leaderboards
    private void resetAllGroupLeaderboards() {
        // Fetch all group IDs (assuming you have a method in the repository to get them)
        List<Long> allGroupIds = groupRepository.findAllGroupIds();

        // Reset each group's leaderboard
        for (Long groupId : allGroupIds) {
            groupLeaderboardService.resetGroupLeaderboard(groupId);
        }
    }

    // Check if the tournament is currently active
    public boolean isTournamentActive() {
        return tournamentRepository.findActiveTournament().isPresent();
    }



    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    public List<User> enterTournament(Long userId) {
        // Step 1: Check if the user exists and is eligible
        Optional<User> optionalUser = userRepository.findById(userId);
        if (!optionalUser.isPresent()) {
            throw new CustomAppException(HttpStatus.NOT_FOUND, "User with ID " + userId + " not found");
        }

        User user = optionalUser.get();

        if (user.getHasReward() == 1 || user.getHasReward() == 2) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "User has unclaimed rewards and cannot join the tournament.");
        }

        if (user.getGroup() != null) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "User is already in a group.");
        }

        if (user.getLevel() < 20 || user.getCoins() < 1000) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "User is not eligible to join the tournament.");
        }

        // Step 2: Check if there is an active tournament
        Optional<Tournament> activeTournament = tournamentRepository.findActiveTournament();
        if (!activeTournament.isPresent()) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "No active tournament found.");
        }

        // Step 3: Retry mechanism for finding/creating group and adding the user
        int maxRetries = 10;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // Step 3: Find a suitable group in the tournament
                Group group = findOrCreateGroupForUser(user, activeTournament.get());

                // Step 4: Add user to the group and update the group's countries field
                addUserToGroup(user, group);

                // Step 5: Return the group's leaderboard (all users in the group)
                return userRepository.findByGroupId(group.getGroupId());

            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new RuntimeException("Could not add user to group after " + maxRetries + " retries due to concurrent updates.");
                }
                // Retry the process if optimistic locking failed
            }
        }

        throw new RuntimeException("Unexpected error: retries exceeded while entering tournament.");
    }


    @Transactional
    public Group findOrCreateGroupForUser(User user, Tournament tournament) {
        int maxRetries = 15;  // Maximum retry attempts in case of optimistic locking failure
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                // Find all groups in the active tournament ordered by the number of users (descending)
                List<Group> groups = groupRepository.findGroupsByTournamentIdOrderByUserCountDesc(tournament.getId());

                // Step 1: Try to find a group where the user can join
                for (Group group : groups) {
                    String countries = group.getCountries();  // The countries stored as a comma-separated string

                    // Check if the group has fewer than 5 users and doesn't already have a user from the same country
                    long userCount = countUsersInGroup(countries);
                    boolean sameCountryExists = checkCountryInGroup(countries, user.getCountry());

                    if (userCount < 5 && !sameCountryExists) {
                        return group;  // Found a suitable group
                    }
                }

                // Step 2: No suitable group found, create a new one
                Group newGroup = new Group();
                newGroup.setTournament(tournament);
                newGroup.setGroupStatus("waiting");  // Set initial group status
                newGroup.setCountries("");  // Initialize countries as an empty string
                return groupRepository.save(newGroup);  // Save the new group

            } catch (OptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw new RuntimeException("Could not find or create group after " + maxRetries + " retries due to concurrent updates.");
                }
                // Retry the operation if optimistic locking failed
            }
        }

        throw new RuntimeException("Unexpected error: group creation retries exceeded.");
    }

@Transactional
public void addUserToGroup(User user, Group group) {
    int maxRetries = 10;
    int retryCount = 0;

    while (retryCount < maxRetries) {
        try {
            // Step 1: Check if the group is still valid (less than 5 users and no same country)
            String countries = group.getCountries();
            long userCount = countUsersInGroup(countries);
            boolean sameCountryExists = checkCountryInGroup(countries, user.getCountry());

            if (userCount >= 5 || sameCountryExists) {
                // If the group is no longer suitable, find or create a new group
                group = findOrCreateGroupForUser(user, group.getTournament());
            }

            // Step 2: Decrease user's coins and assign the group
            user.setCoins(user.getCoins() - 1000);
            user.setGroup(group);
            userRepository.save(user);

            // Step 3: Add the user's country to the group's countries string
            String updatedCountries = addCountryToGroup(group.getCountries(), user.getCountry());
            group.setCountries(updatedCountries);

            // Step 4: Check again how many users are in the group after adding the user
            userCount = countUsersInGroup(updatedCountries);
            if (userCount == 5) {
                group.setGroupStatus("active");  // Set group status to "active" when 5 users have joined
            }

            // Step 5: Save the updated group
            groupRepository.save(group);  // This will trigger optimistic locking

            // Step 6: Update the group leaderboard
            groupLeaderboardService.updateUserScoreInGroup(group.getGroupId(), user.getId(), 0);

            // If everything is successful, exit the retry loop
            break;

        } catch (OptimisticLockingFailureException e) {
            retryCount++;
            if (retryCount >= maxRetries) {
                throw new RuntimeException("Could not add user to group after " + maxRetries + " retries due to concurrent updates.");
            }
            // Re-fetch the group after an optimistic locking failure
            group = groupRepository.findById(group.getGroupId())
                    .orElseThrow(() -> new RuntimeException("Group not found"));
        }
    }
}

    private String addCountryToGroup(String countries, String userCountry) {
        // If the countries string is empty, simply add the country
        if (countries.isEmpty()) {
            return userCountry;
        } else {
            // Otherwise, append the country to the existing string
            return countries + "," + userCountry;
        }
    }

    private boolean checkCountryInGroup(String countries, String userCountry) {
        // Check if the user's country is already in the comma-separated string
        String[] countryArray = countries.split(",");
        for (String country : countryArray) {
            if (country.equals(userCountry)) {
                return true;
            }
        }
        return false;
    }

    private long countUsersInGroup(String countries) {
        // If the countries string is empty, there are no users in the group
        if (countries.isEmpty()) {
            return 0;
        }
        // Otherwise, count the number of countries (users) by splitting the string
        return countries.split(",").length;
    }


    public List<Object[]> getGroupLeaderboard(Long groupId) {
        // Retrieve the real-time leaderboard from Redis
        Set<ZSetOperations.TypedTuple<String>> redisLeaderboard = groupLeaderboardService.getGroupLeaderboard(groupId);

        // Convert the Redis sorted set into a List<Object[]>
        List<Object[]> leaderboard = new ArrayList<>();

        for (ZSetOperations.TypedTuple<String> entry : redisLeaderboard) {
            Long userId = Long.valueOf(entry.getValue());  // Convert Redis stored user ID (String) to Long
            Double score = entry.getScore();  // Get the user's score from Redis

            // Add userId and score as a tuple to the leaderboard
            leaderboard.add(new Object[] {userId, score});
        }

        return leaderboard;  // Return the list of (userId, score) tuples
    }


    public List<Object[]> getCountryLeaderboard() {
        // Retrieve the leaderboard from Redis
        Set<ZSetOperations.TypedTuple<String>> redisLeaderboard = countryLeaderboardService.getCountryLeaderboard();
        
        // Convert the Redis sorted set into a List<Object[]> (if this is your desired format)
        List<Object[]> leaderboard = new ArrayList<>();
        
        for (ZSetOperations.TypedTuple<String> entry : redisLeaderboard) {
            String country = entry.getValue();
            Double score = entry.getScore();
            leaderboard.add(new Object[] {country, score});
        }

        return leaderboard;
    }

    public Integer getGroupRank(Long userId) {
        // Step 1: Find the user by their userId
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

        // Step 2: Check if the user is part of a group
        Group group = user.getGroup();
        if (group == null) {
            throw new CustomAppException(HttpStatus.BAD_REQUEST, "User is not part of any group.");
        }

        // Step 3: Get the group's ID
        Long groupId = group.getGroupId();

        // Step 4: Retrieve the real-time leaderboard from Redis
        List<Object[]> leaderboard = getGroupLeaderboard(groupId);

        // Step 5: Loop through the leaderboard to find the user's rank
        for (int i = 0; i < leaderboard.size(); i++) {
            Object[] entry = leaderboard.get(i);
            Long leaderboardUserId = (Long) entry[0];  // Get the user ID from the leaderboard

            // Check if this is the user we're looking for
            if (leaderboardUserId.equals(userId)) {
                // Rank is the index + 1 (because ranks are 1-based, not 0-based)
                return i + 1;
            }
        }

        // If the user is not found in the group leaderboard, return null or throw an exception
        throw new RuntimeException("User not found in the group leaderboard");
    }

}
