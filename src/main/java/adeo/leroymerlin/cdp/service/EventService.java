package adeo.leroymerlin.cdp.service;

import adeo.leroymerlin.cdp.domaine.Band;
import adeo.leroymerlin.cdp.domaine.Event;
import adeo.leroymerlin.cdp.domaine.Member;
import adeo.leroymerlin.cdp.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    /**
     * Retrieves a list of all events from the repository.
     *
     * @return a list containing all events
     */
    public List<Event> getEvents() {
        return eventRepository.findAll();
    }

    /**
     * Deletes an event by its unique identifier.
     *
     * @param id the unique identifier of the event to be deleted
     */
    @Transactional
    public void delete(Long id) {
        eventRepository.deleteById(id);
    }

    /**
     * Retrieves a list of events filtered by a query. The query is used to search for a match
     * within the names of members associated with bands in the events. If the query is null or blank,
     * no filtering is applied, but the events are still decorated with additional details.
     *
     * @param query the search query for filtering events based on members' names; could be null or blank.
     * @return a list of events filtered and decorated with relevant counts.
     */
    public List<Event> getFilteredEvents(String query) {
        List<Event> all = eventRepository.findAll();

        if (query == null || query.isBlank()) {
            // Pas de filtre, mais on applique tout de même la décoration avec les compteurs
            return all.stream()
                    .filter(Objects::nonNull)
                    .map(this::decorateEventWithCounts)
                    .toList();
        }

        final String q = query.toLowerCase();

        return all.stream()
                .filter(Objects::nonNull)
                .filter(event -> {
                    Set<Band> bands = safeBands(event);
                    return bands.stream().filter(Objects::nonNull).anyMatch(band -> {
                        Set<Member> members = safeMembers(band);
                        return members.stream()
                                .filter(Objects::nonNull)
                                .map(Member::getName)
                                .filter(Objects::nonNull)
                                .map(String::toLowerCase)
                                .anyMatch(name -> name.contains(q));
                    });
                })
                .map(this::decorateEventWithCounts)
                .toList();
    }

    /**
     * Updates an existing event with the specified changes. If any property in the provided event is null,
     * the corresponding property in the current event is not updated.
     *
     * @param id the unique identifier of the event to be updated
     * @param event the event object containing updated properties
     * @return the updated event after being saved to the repository
     * @throws IllegalArgumentException if no event with the specified id is found
     */
    @Transactional
    public Event updateEvent(Long id, Event event) {
        Event current = eventRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + id));

        if (event.getTitle() != null) {
            current.setTitle(event.getTitle());
        }
        if (event.getImgUrl() != null) {
            current.setImgUrl(event.getImgUrl());
        }
        if (event.getNbStars() != null) {
            current.setNbStars(event.getNbStars());
        }
        if (event.getComment() != null) {
            current.setComment(event.getComment());
        }

        return eventRepository.save(current);
    }

    // --- Helpers ---

    private Event decorateEventWithCounts(Event src) {
        Event copy = new Event();
        copy.setId(src.getId());
        copy.setImgUrl(src.getImgUrl());
        copy.setNbStars(src.getNbStars());
        copy.setComment(src.getComment());

        Set<Band> srcBands = safeBands(src);
        // Décorer les bands d'abord pour calculer la taille (membres)
        Set<Band> decoratedBands = new LinkedHashSet<>();
        for (Band b : srcBands) {
            if (b == null) continue;
            Band bCopy = new Band();
            bCopy.setMembers(safeMembers(b)); // on réutilise la même collection (pas de deep copy des members)
            int membersCount = bCopy.getMembers().size();
            String originalName = b.getName() == null ? "" : b.getName();
            bCopy.setName(originalName + " [" + membersCount + "]");
            decoratedBands.add(bCopy);
        }
        copy.setBands(decoratedBands);

        // Titre avec le nombre de bands
        int bandsCount = decoratedBands.size();
        String originalTitle = src.getTitle() == null ? "" : src.getTitle();
        copy.setTitle(originalTitle + " [" + bandsCount + "]");

        return copy;
    }

    private Set<Band> safeBands(Event e) {
        return e.getBands() != null ? e.getBands() : Collections.emptySet();
    }

    private Set<Member> safeMembers(Band b) {
        return b.getMembers() != null ? b.getMembers() : Collections.emptySet();
    }
}
