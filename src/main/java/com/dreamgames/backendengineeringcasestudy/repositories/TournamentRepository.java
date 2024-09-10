package com.dreamgames.backendengineeringcasestudy.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.dreamgames.backendengineeringcasestudy.models.Tournament;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    @Query("SELECT t FROM Tournament t WHERE t.isActive = true")
    Optional<Tournament> findActiveTournament();
}
