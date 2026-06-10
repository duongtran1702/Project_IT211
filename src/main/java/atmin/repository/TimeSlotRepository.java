package atmin.repository;

import atmin.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    @Query("SELECT t FROM TimeSlot t WHERE t.id = :id AND t.isAvailable = true AND t.isDeleted = false")
    Optional<TimeSlot> findActiveById(@Param("id") Long id);
}
