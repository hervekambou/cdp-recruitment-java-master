package adeo.leroymerlin.cdp.repository;

import adeo.leroymerlin.cdp.domaine.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface EventRepository extends JpaRepository<Event, Long> {
    void deleteById(Long eventId);
}
