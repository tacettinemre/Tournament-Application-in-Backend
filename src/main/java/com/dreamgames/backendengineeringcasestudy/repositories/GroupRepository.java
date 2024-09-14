
package com.dreamgames.backendengineeringcasestudy.repositories;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dreamgames.backendengineeringcasestudy.models.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

    // Find all groups by tournamentId
    List<Group> findByTournamentId(Long tournamentId);

    @Query("SELECT g FROM Group g WHERE g.tournament.id = :tournamentId ORDER BY LENGTH(g.countries) - LENGTH(REPLACE(g.countries, ',', '')) DESC")
    List<Group> findGroupsByTournamentIdOrderByUserCountDesc(@Param("tournamentId") Long tournamentId);

    @Query("SELECT g.id FROM Group g")
    List<Long> findAllGroupIds();
}