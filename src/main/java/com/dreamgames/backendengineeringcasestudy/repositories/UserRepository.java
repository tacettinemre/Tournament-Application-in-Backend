package com.dreamgames.backendengineeringcasestudy.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dreamgames.backendengineeringcasestudy.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    

    @Query("SELECT u FROM User u WHERE u.group_id = :groupId ORDER BY u.score DESC")
    List<User> findByGroupIdOrderByScoreDesc(Long groupId);


    @Query("SELECT u.country, SUM(u.score) as totalScore FROM User u GROUP BY u.country ORDER BY totalScore DESC")
    List<Object[]> getCountryLeaderboard();
    // Find all users by the groupId
    List<User> findByGroupId(Long groupId);
    
    @Modifying
    @Query("UPDATE User u SET u.group_id = null, u.score = 0")
    void resetGroupIdAndScore();
}