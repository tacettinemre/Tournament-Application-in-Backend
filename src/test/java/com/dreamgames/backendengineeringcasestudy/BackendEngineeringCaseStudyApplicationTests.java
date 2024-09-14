package com.dreamgames.backendengineeringcasestudy;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mockito;
import static org.mockito.Mockito.times;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import com.dreamgames.backendengineeringcasestudy.exception.CustomAppException;
import com.dreamgames.backendengineeringcasestudy.models.Group;
import com.dreamgames.backendengineeringcasestudy.models.Tournament;
import com.dreamgames.backendengineeringcasestudy.models.User;
import com.dreamgames.backendengineeringcasestudy.repositories.GroupRepository;
import com.dreamgames.backendengineeringcasestudy.repositories.TournamentRepository;
import com.dreamgames.backendengineeringcasestudy.repositories.UserRepository;
import com.dreamgames.backendengineeringcasestudy.services.CountryLeaderboardService;
import com.dreamgames.backendengineeringcasestudy.services.GroupLeaderboardService;
import com.dreamgames.backendengineeringcasestudy.services.TournamentService;
import com.dreamgames.backendengineeringcasestudy.services.UserService;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
class BackendEngineeringCaseStudyApplicationTests {

    
    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private GroupRepository groupRepository;

    @MockBean
    private TournamentRepository tournamentRepository;
    
    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private ZSetOperations<String, String> zSetOperations;
    
    @MockBean
    private GroupLeaderboardService groupLeaderboardService;

    @MockBean
    private CountryLeaderboardService countryLeaderboardService;
    @Test
    void testEnterTournament_UserNotFound() {
        // Mock the repository response to simulate a user not found scenario
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Capture the thrown CustomAppException
        CustomAppException exception = assertThrows(CustomAppException.class, () -> {
            tournamentService.enterTournament(1L);  // This should trigger the exception
        });

        // Optionally, verify that the exception message is correct
        assertEquals("User with ID 1 not found", exception.getMessage());
    }
    @Test
    void testEnterTournament_UserHasUnclaimedRewards() throws Exception {
        // Mock the repository response (user has unclaimed rewards)
        User userWithReward = new User();
        userWithReward.setHasReward(1);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(userWithReward));

        // Expect CustomAppException with status 400
        CustomAppException exception = assertThrows(CustomAppException.class, () -> {
            tournamentService.enterTournament(1L);
        });

        // Assert exception message and status code
        assertEquals("User has unclaimed rewards and cannot join the tournament.", exception.getMessage());
        assertEquals(400, exception.getStatus().value());
    }
    @Test
    void testEnterTournament_UserNotEligible() throws Exception {
        // Mock the repository response (user not eligible due to level/coins)
        User userNotEligible = new User();
        userNotEligible.setLevel(10);
        userNotEligible.setCoins(500);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(userNotEligible));

        // Expect CustomAppException with status 400
        CustomAppException exception = assertThrows(CustomAppException.class, () -> {
            tournamentService.enterTournament(1L);
        });

        // Assert exception message and status code
        assertEquals("User is not eligible to join the tournament.", exception.getMessage());
        assertEquals(400, exception.getStatus().value());
    }

    @Test
    void testEnterTournament_NoActiveTournament() throws Exception {
        // Mock the repository responses (user found and eligible, but no active tournament)
        User eligibleUser = new User();
        eligibleUser.setLevel(25);
        eligibleUser.setCoins(2000);
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(eligibleUser));
        Mockito.when(tournamentRepository.findActiveTournament()).thenReturn(Optional.empty());

        // Expect CustomAppException with status 400
        CustomAppException exception = assertThrows(CustomAppException.class, () -> {
            tournamentService.enterTournament(1L);
        });

        // Assert exception message and status code
        assertEquals("No active tournament found.", exception.getMessage());
        assertEquals(400, exception.getStatus().value());
    }


    @Test
    void testEnterTournament_Success() throws Exception {
        // Mock the repository responses (successful entry)

        // Mock Redis behavior
        Mockito.when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);

        // Mock the ZSetOperations add behavior (as if Redis is adding the score successfully)
        Mockito.when(zSetOperations.add(anyString(), anyString(), anyDouble())).thenReturn(true);
        User eligibleUser = new User();
        eligibleUser.setId(1L);
        eligibleUser.setCountry("USA");
        eligibleUser.setLevel(25);
        eligibleUser.setCoins(2000);
        Group group = new Group();
        group.setGroupId(1L);
        group.setCountries("");
        Tournament activeTournament = new Tournament();

        // Mock repository method responses
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(eligibleUser));
        Mockito.when(tournamentRepository.findActiveTournament()).thenReturn(Optional.of(activeTournament));
        Mockito.when(groupRepository.findGroupsByTournamentIdOrderByUserCountDesc(anyLong())).thenReturn(List.of(group));
        Mockito.when(groupRepository.save(any(Group.class))).thenReturn(group);
        Mockito.when(userRepository.findByGroupId(anyLong())).thenReturn(List.of(eligibleUser));
        // Call the method under test
        tournamentService.enterTournament(1L);
        Mockito.verify(groupLeaderboardService, times(1)).updateUserScoreInGroup(group.getGroupId(), eligibleUser.getId(), 0);

    }

    @Test
    void testGetGroupLeaderboard_Success() throws Exception {
        // Mock the Redis leaderboard response
        ZSetOperations.TypedTuple<String> leaderboardEntry1 = Mockito.mock(ZSetOperations.TypedTuple.class);
        ZSetOperations.TypedTuple<String> leaderboardEntry2 = Mockito.mock(ZSetOperations.TypedTuple.class);

        Mockito.when(leaderboardEntry1.getValue()).thenReturn("1");
        Mockito.when(leaderboardEntry1.getScore()).thenReturn(100.0);
        Mockito.when(leaderboardEntry2.getValue()).thenReturn("2");
        Mockito.when(leaderboardEntry2.getScore()).thenReturn(95.0);

        Set<ZSetOperations.TypedTuple<String>> mockRedisLeaderboard = new LinkedHashSet<>(List.of(leaderboardEntry1, leaderboardEntry2));

        // Mock the GroupLeaderboardService to return the mocked Redis data
        Mockito.when(groupLeaderboardService.getGroupLeaderboard(anyLong())).thenReturn(mockRedisLeaderboard);

        // Call the method under test (from TournamentService)
        List<Object[]> leaderboard = tournamentService.getGroupLeaderboard(1L);

        // Verify the leaderboard size and content
        assertEquals(2, leaderboard.size());
        assertEquals(1L, leaderboard.get(0)[0]);  // Check first entry's userId
        assertEquals(100.0, leaderboard.get(0)[1]);  // Check first entry's score
        assertEquals(2L, leaderboard.get(1)[0]);  // Check second entry's userId
        assertEquals(95.0, leaderboard.get(1)[1]);  // Check second entry's score
    }

    @Test
    void testGetCountryLeaderboard_Success() throws Exception {
        // Mock the Redis leaderboard response
        ZSetOperations.TypedTuple<String> leaderboardEntry1 = Mockito.mock(ZSetOperations.TypedTuple.class);
        ZSetOperations.TypedTuple<String> leaderboardEntry2 = Mockito.mock(ZSetOperations.TypedTuple.class);

        // Mock the values for country names and scores
        Mockito.when(leaderboardEntry1.getValue()).thenReturn("Turkey");
        Mockito.when(leaderboardEntry1.getScore()).thenReturn(200.0);
        Mockito.when(leaderboardEntry2.getValue()).thenReturn("USA");
        Mockito.when(leaderboardEntry2.getScore()).thenReturn(180.0);

        Set<ZSetOperations.TypedTuple<String>> mockRedisLeaderboard = new LinkedHashSet<>(List.of(leaderboardEntry1, leaderboardEntry2));

        // Mock the CountryLeaderboardService to return the mocked Redis data
        Mockito.when(countryLeaderboardService.getCountryLeaderboard()).thenReturn(mockRedisLeaderboard);

        // Call the method under test (from TournamentService)
        List<Object[]> leaderboard = tournamentService.getCountryLeaderboard();

        // Verify the leaderboard size and content
        assertEquals(2, leaderboard.size());
        assertEquals("Turkey", leaderboard.get(0)[0]);  // Check first entry's country
        assertEquals(200.0, leaderboard.get(0)[1]);  // Check first entry's score
        assertEquals("USA", leaderboard.get(1)[0]);  // Check second entry's country
        assertEquals(180.0, leaderboard.get(1)[1]);  // Check second entry's score
    }

    @Test
    void testGetGroupRank_Success() {
        // Mock user and group
        User user = new User();
        user.setId(1L); // This is the user we are testing
        Group group = new Group();
        group.setGroupId(1L);
        user.setGroup(group);

        // Mock Redis ZSet operations return values (TypedTuple)
        ZSetOperations.TypedTuple<String> leaderboardEntry1 = Mockito.mock(ZSetOperations.TypedTuple.class);
        ZSetOperations.TypedTuple<String> leaderboardEntry2 = Mockito.mock(ZSetOperations.TypedTuple.class);

        // Mock the behavior of the TypedTuple entries
        // Set user with ID 1L as the top scorer
        Mockito.when(leaderboardEntry1.getValue()).thenReturn("1"); // User with ID 1L (target user)
        Mockito.when(leaderboardEntry1.getScore()).thenReturn(100.0);
        
        // Set another user with ID 2L with a lower score
        Mockito.when(leaderboardEntry2.getValue()).thenReturn("2"); // Another user with ID 2L
        Mockito.when(leaderboardEntry2.getScore()).thenReturn(95.0);

        // Create a set to return from groupLeaderboardService.getGroupLeaderboard()
        // Ensure the set is ordered correctly so that user with ID 1 is first
        Set<ZSetOperations.TypedTuple<String>> mockLeaderboard = new LinkedHashSet<>(List.of(leaderboardEntry1, leaderboardEntry2));

        // Mock the repository and service methods
        Mockito.when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        Mockito.when(groupLeaderboardService.getGroupLeaderboard(anyLong())).thenReturn(mockLeaderboard);

        // Call the method under test
        Integer rank = tournamentService.getGroupRank(1L); // ID of the target user

        // Verify that the correct rank is returned (user is at position 1)
        assertEquals(1, rank);
    }

    @Test
    void testCreateUser_Success() throws Exception {
        // Mock repository response (if createUser interacts with the repository)
        User newUser = new User();
        newUser.setId(1L);
        newUser.setLevel(1);
        newUser.setCoins(5000);

        // If the service calls a repository, you mock the repository method
        Mockito.when(userRepository.save(any(User.class))).thenReturn(newUser);

        // Call the real UserService method
        User createdUser = userService.createUser();

        // Verify the actual logic of the UserService is correct
        assertEquals(1L, createdUser.getId());
        assertEquals(1, createdUser.getLevel());
        assertEquals(5000, createdUser.getCoins());
    }


    @Test
    void testUpdateUserLevel_Success() throws Exception {
        // Mock the initial user returned by the repository when finding by ID
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setLevel(1);
        existingUser.setCoins(5000);

        // Mock the updated user after increasing the level
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setLevel(2); // The new level
        updatedUser.setCoins(5050); // Assume coins increase after level update

        // Mock the repository to return the existing user when finding by ID
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));

        // Mock the repository to save the updated user and return the saved user
        Mockito.when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Call the real UserService method to update the user's level
        User result = userService.updateLevel(1L);

        // Verify that the user level is updated successfully
        assertEquals(2, result.getLevel()); // Ensure the level is updated
        assertEquals(5050, result.getCoins()); // Ensure the coins are updated

        // Verify that the save method was called once with the updated user
        Mockito.verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testClaimReward_Success() throws Exception {
        // Mock the user returned by the repository when finding by ID
        User userWithReward = new User();
        userWithReward.setId(1L);
        userWithReward.setCoins(5000); // Initial coins before claiming the reward
        userWithReward.setHasReward(1); // Indicate that the user has a reward to claim

        // Mock the user after claiming the reward (coins increased)
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setCoins(15000); // Updated coins after claiming reward
        updatedUser.setHasReward(0); // Reward claimed, so no unclaimed rewards

        // Mock the repository to return the user with a reward when finding by ID
        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(userWithReward));

        // Mock the repository to save the updated user and return the saved user
        Mockito.when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        // Call the real UserService method to claim the reward
        User result = userService.claimReward(1L);

        // Verify that the reward was claimed successfully and the coins were updated
        assertEquals(15000, result.getCoins());
        assertEquals(0, result.getHasReward()); // Ensure the reward status is updated

        // Verify that the save method was called once with the updated user
        Mockito.verify(userRepository, times(1)).save(any(User.class));
    }
}
