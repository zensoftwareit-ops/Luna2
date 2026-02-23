package it.zensoftware.luna2.service.calendar;

import it.zensoftware.luna2.model.CalendarAccount;
import it.zensoftware.luna2.model.CalendarEvent;

import java.time.LocalDateTime;
import java.util.List;

public interface CalendarProvider {
    CalendarAccount.Provider getProvider();

    List<CalendarEvent> listEvents(CalendarAccount account, LocalDateTime from, LocalDateTime to);

    String createOrUpdateEvent(CalendarAccount account, CalendarEvent event);

    void deleteEvent(CalendarAccount account, String externalEventId);
}
