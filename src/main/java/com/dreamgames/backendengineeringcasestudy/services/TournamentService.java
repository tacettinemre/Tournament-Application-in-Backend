package com.dreamgames.backendengineeringcasestudy.services;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.dreamgames.backendengineeringcasestudy.exception.UserNotEligibleException;
import com.dreamgames.backendengineeringcasestudy.models.Group;
import com.dreamgames.backendengineeringcasestudy.models.Tournament;
import com.dreamgames.backendengineeringcasestudy.models.User;
import com.dreamgames.backendengineeringcasestudy.repositories.GroupRepository;
import com.dreamgames.backendengineeringcasestudy.repositories.TournamentRepository;
import com.dreamgames.backendengineeringcasestudy.repositories.UserRepository;

@Service
public class TournamentService {

    @Autowired
    private TournamentRepository tournamentRepository;
    private void resetUsersAfterTournament() {
        // Assuming you have a UserRepository to interact with users
        userRepository.resetGroupIdAndScore();
    }

    private void assignRewardsToTopUsersInGroups(Tournament tournament) {
        // Get all groups in the tournament
        List<Group> groups = groupRepository.findByTournamentId(tournament.getId());

        // Iterate through each group and assign rewards to the top two users
        for (Group group : groups) {
            // Retrieve the users in this group, sorted by score
            List<User> usersInGroup = userRepository.findByGroupIdOrderByScoreDesc(group.getGroupId());

            // Ensure the group has exactly five users
            if (usersInGroup.size() == 5) {
                // First place: 10,000 coins
                User firstPlaceUser = usersInGroup.get(0);
                firstPlaceUser.setHasReward(1);  // Set hasReward to 1 for first place
                userRepository.save(firstPlaceUser);

                // Second place: 5,000 coins
                User secondPlaceUser = usersInGroup.get(1);
                secondPlaceUser.setHasReward(2);  // Set hasReward to 2 for second place
                userRepository.save(secondPlaceUser);
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
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")  // Runs at 00:00 UTC daily
    public void startNewTournament() {
        // End any active tournament
        endCurrentTournament();

        // Create and start a new tournament
        Tournament newTournament = new Tournament();
        newTournament.setActive(true);
        tournamentRepository.save(newTournament);
    }

    // Ends the current tournament at 20:00 UTC daily
    @Scheduled(cron = "0 0 20 * * *", zone = "UTC")  // Runs at 20:00 UTC daily
    public void endCurrentTournament() {
        Optional<Tournament> activeTournament = tournamentRepository.findActiveTournament();
        if (activeTournament.isPresent()) {
            Tournament tournament = activeTournament.get();
            tournament.setActive(false);
            tournamentRepository.save(tournament);

            completeGroupsForTournament(tournament);

            assignRewardsToTopUsersInGroups(tournament);

            resetUsersAfterTournament();
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
            throw new UserNotEligibleException("User not found");
        }

        User user = optionalUser.get();

        if (user.getHasReward() == 1 || user.getHasReward() == 2) {
            throw new UserNotEligibleException("User has unclaimed rewards and cannot join the tournament.");
        }

        if (user.getGroup() != null) {
            throw new RuntimeException("User is already in a group.");
        }

        if (user.getLevel() < 20 || user.getCoins() < 1000) {
            throw new UserNotEligibleException("User is not eligible to join the tournament");
        }

        // Step 2: Check if there is an active tournament
        Optional<Tournament> activeTournament = tournamentRepository.findActiveTournament();
        if (!activeTournament.isPresent()) {
            throw new RuntimeException("No active tournament found");
        }

        // Step 3: Find a suitable group in the tournament
        Group group = findOrCreateGroupForUser(user, activeTournament.get());

        // Step 4: Add user to the group and update the group's countries field
        addUserToGroup(user, group);

        // Step 5: Return the group's leaderboard (all users in the group)
        return userRepository.findByGroupId(group.getGroupId());
    }

    private Group findOrCreateGroupForUser(User user, Tournament tournament) {
        // Find all groups in the active tournament
        List<Group> groups = groupRepository.findByTournamentId(tournament.getId());

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
        newGroup = groupRepository.save(newGroup);  // Save the new group

        return newGroup;
    }

    private void addUserToGroup(User user, Group group) {
        user.setCoins(user.getCoins()-1000);
        // Add user to the group
        user.setGroup(group);
        userRepository.save(user);

        // Step 1: Add the user's country to the group's countries string
        String updatedCountries = addCountryToGroup(group.getCountries(), user.getCountry());
        group.setCountries(updatedCountries);

        long userCount = countUsersInGroup(updatedCountries);
        if (userCount == 5) {
            group.setGroupStatus("active");  // Set the group status to "active" when 5 users have joined
        }
        // Step 2: Save the group with the updated countries field
        groupRepository.save(group);
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


    public List<User> getGroupLeaderboard(Long groupId) {
        // Retrieve all users in the specified group, sorted by score
        return userRepository.findByGroupIdOrderByScoreDesc(groupId);
    }

    public int getUserRankInGroup(Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (!optionalUser.isPresent()) {
            throw new RuntimeException("User not found");
        }

        User user = optionalUser.get();

        if (user.getGroup() == null) {
            throw new RuntimeException("User is not part of any group.");
        }

        // Get the leaderboard for the user's group
        List<User> leaderboard = userRepository.findByGroupIdOrderByScoreDesc(user.getGroup().getGroupId());

        // Find the rank of the user in the leaderboard
        for (int i = 0; i < leaderboard.size(); i++) {
            if (leaderboard.get(i).getId().equals(user.getId())) {
                return i + 1;  // Rank is index + 1 (1-based index)
            }
        }

        // If user is not found in the group (which shouldn't happen)
        throw new RuntimeException("User not found in the group.");
    }

    public List<Object[]> getCountryLeaderboard() {
        // Retrieve the country leaderboard, grouping by country and summing scores
        return userRepository.getCountryLeaderboard();
    }

}
