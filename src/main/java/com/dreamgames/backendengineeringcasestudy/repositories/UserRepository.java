package com.dreamgames.backendengineeringcasestudy.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dreamgames.backendengineeringcasestudy.models.User;

import jakarta.transaction.Transactional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    


    // Find all users by the groupId
    List<User> findByGroupId(Long groupId);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.group = null, u.score = 0")
    void resetGroupIdAndScore();
}