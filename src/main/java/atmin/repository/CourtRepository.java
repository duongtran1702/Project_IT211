package atmin.repository;

import atmin.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface CourtRepository extends JpaRepository<Court, Long> {
    @Query("SELECT c FROM Court c WHERE c.id = :id AND c.isAvailable = true AND c.isDeleted = false")
    Optional<Court> findActiveById(@Param("id") Long id);
}
