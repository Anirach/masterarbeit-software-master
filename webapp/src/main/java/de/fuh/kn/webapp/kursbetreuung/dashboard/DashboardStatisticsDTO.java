package de.fuh.kn.webapp.kursbetreuung.dashboard;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO für Dashboard-Statistiken der letzten 24 Stunden
 */
@Data
@Builder
public class DashboardStatisticsDTO {
    private int anzahlLoesungsversuche;
    private int anzahlChatNachrichten;
    private BigDecimal aiKostenGesamt;
    private int anzahlAktiveNutzer;
}