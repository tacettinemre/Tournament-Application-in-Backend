package com.dreamgames.backendengineeringcasestudy.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dreamgames.backendengineeringcasestudy.models.User;
import com.dreamgames.backendengineeringcasestudy.services.TournamentService;

@RestController
@RequestMapping("/tournaments")
public class TournamentController {
    
    @Autowired
    private TournamentService tournamentService;

    // Endpoint for a user to enter the tournament
    @PostMapping("/enterTournament")
    public ResponseEntity<List<User>> enterTournament(@RequestParam Long userId) {
        List<User> group = tournamentService.enterTournament(userId);
        return ResponseEntity.ok(group);  // Return the group's leaderboard
    }
    

    @GetMapping("/getGroupLeaderboard")
    public ResponseEntity<List<Object[]>> getGroupLeaderboard(@RequestParam Long groupId) {
        // Use the TournamentService to retrieve the leaderboard
        List<Object[]> leaderboard = tournamentService.getGroupLeaderboard(groupId);

        // Return the leaderboard as a ResponseEntity
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/getCountryLeaderboard")
    public ResponseEntity<List<Object[]>> getCountryLeaderboard() {
        // Call the service method to get the country leaderboard
        List<Object[]> leaderboard = tournamentService.getCountryLeaderboard();
        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/getGroupRank")
    public ResponseEntity<Integer> getGroupRank(@RequestParam Long userId) {
        // Call the service method to get the user's rank in their group
        Integer rank = tournamentService.getGroupRank(userId);

        // Return the rank as a ResponseEntity
        return ResponseEntity.ok(rank);
    }
}
