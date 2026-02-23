package it.zensoftware.luna2.api.controller;

import it.zensoftware.luna2.api.service.CalendarSyncService;
import it.zensoftware.luna2.dao.CalendarAccountDAO;
import it.zensoftware.luna2.dao.CalendarEventDAO;
import it.zensoftware.luna2.model.CalendarAccount;
import it.zensoftware.luna2.model.CalendarEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * REST API per Calendario.
 * 
 * GET    /api/v1/calendario/events   - Lista eventi
 * GET    /api/v1/calendario/events/{id} - Dettaglio evento
 * POST   /api/v1/calendario/events   - Crea evento
 * PUT    /api/v1/calendario/events/{id} - Aggiorna evento
 * DELETE /api/v1/calendario/events/{id} - Cancella evento
 * POST   /api/v1/calendario/sync     - Sincronizza con provider esterno
 * GET    /api/v1/calendario/providers - Provider configurati
 */
@RestController
@RequestMapping("/api/v1/calendario")
public class CalendarioController {

    private final CalendarEventDAO eventDAO = new CalendarEventDAO();
    private final CalendarAccountDAO accountDAO = new CalendarAccountDAO();
    private final CalendarSyncService calendarSyncService = new CalendarSyncService();

    /**
     * Lista eventi nel range di date.
     */
    @GetMapping("/events")
    public ResponseEntity<Page<EventoDTO>> listEvents(
            @RequestParam String userId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) String provider,
            Pageable pageable) {
        List<CalendarEvent> events = eventDAO.findByUserId(userId);
        List<EventoDTO> dtos = new ArrayList<>();
        LocalDateTime fromDate = from.atStartOfDay();
        LocalDateTime toDate = to.atTime(23, 59, 59);

        for (CalendarEvent event : events) {
            if (provider != null && !provider.isEmpty()
                    && !event.getProvider().name().equalsIgnoreCase(provider)) {
                continue;
            }

            if (event.getStartTime() == null || event.getEndTime() == null) {
                continue;
            }

            if (event.getStartTime().isBefore(fromDate) || event.getStartTime().isAfter(toDate)) {
                continue;
            }

            dtos.add(toDto(event));
        }

        return ResponseEntity.ok(paginate(dtos, pageable));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventoDTO> getEvent(@PathVariable Long id) {
        CalendarEvent event = eventDAO.findById(id);
        if (event == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(event));
    }

    @PostMapping("/events")
    public ResponseEntity<EventoDTO> createEvent(@RequestBody EventoDTO dto) {
        CalendarEvent event = fromDto(dto, null);
        eventDAO.save(event);
        return ResponseEntity.status(201).body(toDto(event));
    }

    @PutMapping("/events/{id}")
    public ResponseEntity<EventoDTO> updateEvent(@PathVariable Long id, @RequestBody EventoDTO dto) {
        CalendarEvent existing = eventDAO.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        CalendarEvent updated = fromDto(dto, existing);
        updated.setId(id);
        eventDAO.update(updated);
        return ResponseEntity.ok(toDto(updated));
    }

    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        CalendarEvent existing = eventDAO.findById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        eventDAO.delete(existing);
        return ResponseEntity.noContent().build();
    }

    /**
     * Sincronizza calendario con provider esterno (Google, iCloud).
     * Bidirezionale: scarica dal provider + carica i nuovi eventi.
     */
    @PostMapping("/sync")
    public ResponseEntity<SyncResult> syncCalendar(
            @RequestParam String provider,  // google, icloud
            @RequestParam(required = false) Long accountId) {
        CalendarSyncService.SyncResult result = calendarSyncService.syncCalendars(provider, accountId);
        SyncResult response = new SyncResult(
            result.provider,
            result.downloadedEvents,
            result.uploadedEvents,
            result.timestamp
        );
        response.success = result.success;
        response.errorMessage = result.errorMessage;
        return ResponseEntity.ok(response);
    }

    /**
     * Lista provider calendario configurati.
     */
    @GetMapping("/providers")
    public ResponseEntity<List<ProviderDTO>> getProviders() {
        List<CalendarAccount> accounts = accountDAO.findEnabled();
        List<ProviderDTO> providers = new ArrayList<>();

        for (CalendarAccount account : accounts) {
            providers.add(new ProviderDTO(
                account.getProvider().name().toLowerCase(),
                account.getProvider().name() + " Calendar",
                account.getSyncEnabled()
            ));
        }

        return ResponseEntity.ok(providers);
    }

    
    
    public static class EventoDTO {
        public Long id;
        public String userId;
        public String provider;
        public String titolo;
        public String descrizione;
        public LocalDateTime dataInizio;
        public LocalDateTime dataFine;
        public String tipo;  // MEETING, REMINDER, TASK
        public Long sourceId;
        public String externalEventId;
        public String status;

        public EventoDTO() {}

        public EventoDTO(Long id, String titolo, LocalDateTime dataInizio, LocalDateTime dataFine, String tipo) {
            this.id = id;
            this.titolo = titolo;
            this.dataInizio = dataInizio;
            this.dataFine = dataFine;
            this.tipo = tipo;
        }
    }

    
    
    public static class SyncResult {
        public String provider;
        public Integer eventiScaricati;
        public Integer eventiCaricati;
        public LocalDateTime timestamp;
        public boolean success;
        public String errorMessage;

        public SyncResult() {}

        public SyncResult(String provider, Integer eventiScaricati, Integer eventiCaricati, LocalDateTime timestamp) {
            this.provider = provider;
            this.eventiScaricati = eventiScaricati;
            this.eventiCaricati = eventiCaricati;
            this.timestamp = timestamp;
        }
    }

    
    
    public static class ProviderDTO {
        public String id;
        public String nome;
        public Boolean configurato;

        public ProviderDTO() {}

        public ProviderDTO(String id, String nome, Boolean configurato) {
            this.id = id;
            this.nome = nome;
            this.configurato = configurato;
        }
    }

    private EventoDTO toDto(CalendarEvent event) {
        EventoDTO dto = new EventoDTO();
        dto.id = event.getId();
        dto.userId = event.getUserId();
        dto.provider = event.getProvider() != null ? event.getProvider().name() : null;
        dto.titolo = event.getTitle();
        dto.descrizione = event.getDescription();
        dto.dataInizio = event.getStartTime();
        dto.dataFine = event.getEndTime();
        dto.tipo = event.getSourceType() != null ? event.getSourceType().name() : null;
        dto.sourceId = event.getSourceId();
        dto.externalEventId = event.getExternalEventId();
        dto.status = event.getStatus() != null ? event.getStatus().name() : null;
        return dto;
    }

    private CalendarEvent fromDto(EventoDTO dto, CalendarEvent existing) {
        CalendarEvent event = existing != null ? existing : new CalendarEvent();
        if (dto == null) {
            return event;
        }

        if (dto.userId != null) {
            event.setUserId(dto.userId);
        }

        if (dto.provider != null) {
            event.setProvider(CalendarAccount.Provider.valueOf(dto.provider.toUpperCase()));
        }

        if (dto.titolo != null) {
            event.setTitle(dto.titolo);
        }

        event.setDescription(dto.descrizione);
        event.setStartTime(dto.dataInizio);
        event.setEndTime(dto.dataFine);

        if (dto.tipo != null) {
            event.setSourceType(CalendarEvent.SourceType.valueOf(dto.tipo.toUpperCase()));
        } else if (event.getSourceType() == null) {
            event.setSourceType(CalendarEvent.SourceType.MEETING);
        }

        if (dto.sourceId != null) {
            event.setSourceId(dto.sourceId);
        } else if (event.getSourceId() == null) {
            event.setSourceId(0L);
        }

        if (dto.externalEventId != null) {
            event.setExternalEventId(dto.externalEventId);
        }

        if (dto.status != null) {
            event.setStatus(CalendarEvent.Status.valueOf(dto.status.toUpperCase()));
        }

        event.setUpdatedAt(LocalDateTime.now());
        return event;
    }

    private Page<EventoDTO> paginate(List<EventoDTO> list, Pageable pageable) {
        if (list == null) {
            list = Collections.emptyList();
        }
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<EventoDTO> content = start > end ? Collections.emptyList() : list.subList(start, end);
        return new PageImpl<>(content, pageable, list.size());
    }
}
