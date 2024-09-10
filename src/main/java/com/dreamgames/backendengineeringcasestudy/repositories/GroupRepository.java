
package com.dreamgames.backendengineeringcasestudy.repositories;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dreamgames.backendengineeringcasestudy.models.Group;

public interface GroupRepository extends JpaRepository<Group, Long> {

    // Find all groups by tournamentId
    List<Group> findByTournamentId(Long tournamentId);
}