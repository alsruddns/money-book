package com.moneybook.backend.holiday.repository.impl;

import com.moneybook.backend.entity.Holiday;
import com.moneybook.backend.entity.HolidaySyncStatus;
import com.moneybook.backend.holiday.repository.HolidayRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HolidayRepositoryImpl implements HolidayRepository {
    private final EntityManager em;

    @Override
    public List<Holiday> findBetween(LocalDate start, LocalDate endExclusive) {
        return em.createQuery("""
                select h from Holiday h where h.holidayDate >= :start and h.holidayDate < :end
                order by h.holidayDate, h.name
                """, Holiday.class)
                .setParameter("start", start).setParameter("end", endExclusive).getResultList();
    }

    @Override
    public Optional<HolidaySyncStatus> findStatus(int year) {
        return Optional.ofNullable(em.find(HolidaySyncStatus.class, year));
    }

    @Override
    public void saveStatus(HolidaySyncStatus status) {
        if (!em.contains(status)) em.persist(status);
    }

    /** Replaces a full year only after the client has parsed every API page successfully. */
    @Override
    public void replaceYear(int year, List<Holiday> holidays) {
        LocalDate start = LocalDate.of(year, 1, 1);
        em.createQuery("delete from Holiday h where h.holidayDate >= :start and h.holidayDate < :end")
                .setParameter("start", start).setParameter("end", start.plusYears(1)).executeUpdate();
        holidays.forEach(em::persist);
    }
}
