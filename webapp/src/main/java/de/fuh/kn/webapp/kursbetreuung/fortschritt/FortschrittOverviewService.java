package de.fuh.kn.webapp.kursbetreuung.fortschritt;

import de.fuh.kn.webapp.chat.dto.ChatNachrichtReferenzMapper;
import de.fuh.kn.webapp.common.markdown.flexmark.MarkdownRenderer;
import de.fuh.kn.webapp.common.markdown.flexmark.field.InputFieldNodeRenderer;
import de.fuh.kn.webapp.kursverwaltung.dto.KursDTO;
import de.fuh.kn.webapp.kursverwaltung.dto.KursMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.StudentMapper;
import de.fuh.kn.webapp.persistence.entity.*;
import de.fuh.kn.webapp.persistence.repository.*;
import de.fuh.kn.webapp.uebung.dto.KursFortschrittDTO;
import de.fuh.kn.webapp.uebung.service.FortschrittService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service für die Berechnung und Bereitstellung von Fortschrittsübersichten.
 * 
 * Dieser Service aggregiert Fortschrittsdaten aus verschiedenen Quellen:
 * - Lernfortschritt (abgeschlossene Aufgaben)
 * - Chat-Aktivität (Anzahl der Nachrichten)
 * - Letzte Aktivität
 * - KI-Nutzungskosten (Summe aus Chat-Nachrichten und Lösungsversuchen)
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class FortschrittOverviewService {

    private final BelegungRepository belegungRepository;
    private final KursRepository kursRepository;
    private final StudentRepository studentRepository;
    private final TeilaufgabeRepository teilaufgabeRepository;
    private final FortschrittService fortschrittService;
    private final ChatNachrichtRepository chatNachrichtRepository;
    private final ChatRepository chatRepository;
    private final LoesungsVersuchRepository loesungsVersuchRepository;
    private final StudentMapper studentMapper;
    private final KursMapper kursMapper;
    private final MarkdownRenderer markdownRenderer;
    private final ChatNachrichtReferenzMapper chatNachrichtReferenzMapper;
    private final ChatNachrichtReferenzRepository chatNachrichtReferenzRepository;

    public FortschrittOverviewService(BelegungRepository belegungRepository, KursRepository kursRepository, StudentRepository studentRepository, TeilaufgabeRepository teilaufgabeRepository, AufgabeRepository aufgabeRepository, KurseinheitRepository kurseinheitRepository, FortschrittService fortschrittService, ChatNachrichtRepository chatNachrichtRepository, ChatRepository chatRepository, LoesungsVersuchRepository loesungsVersuchRepository, StudentMapper studentMapper, KursMapper kursMapper, MarkdownRenderer markdownRenderer, ChatNachrichtReferenzMapper chatNachrichtReferenzMapper, ChatNachrichtReferenzRepository chatNachrichtReferenzRepository) {
        this.belegungRepository = belegungRepository;
        this.kursRepository = kursRepository;
        this.studentRepository = studentRepository;
        this.teilaufgabeRepository = teilaufgabeRepository;
        this.fortschrittService = fortschrittService;
        this.chatNachrichtRepository = chatNachrichtRepository;
        this.chatRepository = chatRepository;
        this.loesungsVersuchRepository = loesungsVersuchRepository;
        this.studentMapper = studentMapper;
        this.kursMapper = kursMapper;
        this.markdownRenderer = markdownRenderer;
        this.chatNachrichtReferenzMapper = chatNachrichtReferenzMapper;
        this.chatNachrichtReferenzRepository = chatNachrichtReferenzRepository;
    }

    /**
     * Erstellt eine paginierte Fortschrittsübersicht für einen Kurs.
     * 
     * @param kursDto Der Kurs als DTO
     * @param search Suchbegriff für Studentennamen oder E-Mail
     * @param nurAktiv Ob nur aktive Belegungen angezeigt werden sollen
     * @param pageable Paginierungsinformationen
     * @return Eine paginierte Liste von Fortschrittsübersichten
     */
    public Page<FortschrittOverviewDTO> getFortschrittOverviewForKurs(
            KursDTO kursDto, String search, boolean nurAktiv, Pageable pageable) {
        
        // Kurs-Entity aus Repository laden - benötigt für Repository-Abfragen
        Kurs kurs = kursRepository.findById(kursDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Kurs nicht gefunden"));
        
        // Belegungen basierend auf Filterkriterien abrufen
        Page<Belegung> belegungen;
        LocalDate heute = LocalDate.now();
        
        if (search.isEmpty()) {
            if (nurAktiv) {
                // Aktive Belegungen finden (endDatum ist null oder in der Zukunft)
                belegungen = belegungRepository.findAll((root, query, cb) -> {
                    var kursCondition = cb.equal(root.get("kurs"), kurs);
                    var aktivCondition = cb.or(
                        cb.isNull(root.get("endDatum")),
                        cb.greaterThanOrEqualTo(root.get("endDatum"), heute)
                    );
                    return cb.and(kursCondition, aktivCondition);
                }, pageable);
            } else {
                belegungen = belegungRepository.findByKurs(kurs, pageable);
            }
        } else {
            // Bei Suche die benutzerdefinierten Query-Methoden verwenden
            if (nurAktiv) {
                belegungen = belegungRepository.findByKursAndStudentNameContaining(kurs, search, pageable);
                // Aktive Belegungen filtern
                List<Belegung> filtered = belegungen.getContent().stream()
                        .filter(b -> b.getEndDatum() == null || b.getEndDatum().isAfter(heute) || b.getEndDatum().isEqual(heute))
                        .collect(Collectors.toList());
                belegungen = new PageImpl<>(filtered, pageable, filtered.size());
            } else {
                belegungen = belegungRepository.findByKursAndStudentNameContaining(
                        kurs, search, pageable);
            }
        }

        // Belegungen in Fortschrittsübersicht-DTOs transformieren
        List<FortschrittOverviewDTO> overviews = belegungen.getContent().stream()
                .map(belegung -> createFortschrittOverview(belegung, kursDto))
                .collect(Collectors.toList());

        return new PageImpl<>(overviews, pageable, belegungen.getTotalElements());
    }

    /**
     * Erstellt ein FortschrittOverviewDTO für eine einzelne Belegung.
     * 
     * @param belegung Die Belegung
     * @param kursDto Der Kurs als DTO
     * @return Das erstellte FortschrittOverviewDTO
     */
    private FortschrittOverviewDTO createFortschrittOverview(Belegung belegung, KursDTO kursDto) {
        Student student = belegung.getStudent();
        
        // Entities in DTOs konvertieren
        StudentDTO studentDto = studentMapper.toDto(student);
        
        // Prüfen, ob Belegung aktiv ist
        LocalDate heute = LocalDate.now();
        boolean isAktiv = belegung.getEndDatum() == null || 
                         belegung.getEndDatum().isAfter(heute) || 
                         belegung.getEndDatum().isEqual(heute);
        
        // Fortschritt mit vorhandenem FortschrittService berechnen
        KursFortschrittDTO kursFortschritt = fortschrittService.berechneFortschritt(studentDto, kursDto, isAktiv);
        
        // Aufgabenfortschritt aus KursFortschrittDTO extrahieren
        int gesamtTeilaufgaben = kursFortschritt.getGesamtTeilaufgaben() != null ? kursFortschritt.getGesamtTeilaufgaben().intValue() : 0;
        int abgeschlosseneTeilaufgaben = kursFortschritt.getAbgeschlosseneTeilaufgaben() != null ? kursFortschritt.getAbgeschlosseneTeilaufgaben().intValue() : 0;
        double fortschrittProzent = kursFortschritt.getFortschrittProzent() != null ? kursFortschritt.getFortschrittProzent().doubleValue() : 0.0;

        // Chat-Nachrichten zählen (nur Studentennachrichten)
        long anzahlNachrichten = chatNachrichtRepository.countByStudentAndKurs(student, belegung.getKurs());

        // Letzte Aktivität ermitteln (letzter Lösungsversuch)
        List<LoesungsVersuch> loesungsVersuche = loesungsVersuchRepository
                .findByStudentAndKursOrderByZeitpunktDesc(student, belegung.getKurs());
        LocalDateTime letzteAktivitaet = loesungsVersuche.isEmpty() 
                ? belegung.getStartDatum().atStartOfDay()
                : loesungsVersuche.get(0).getZeitpunkt();

        // KI-Kosten berechnen (Summe aus Chat-Nachrichten und Lösungsversuchen)
        BigDecimal chatKosten = chatNachrichtRepository.sumKostenByStudentAndKurs(student, belegung.getKurs());
        BigDecimal loesungsversuchKosten = loesungsVersuchRepository.sumKostenByStudentAndKurs(student, belegung.getKurs());
        
        // Null-Werte abfangen und auf 0 setzen
        if (chatKosten == null) chatKosten = BigDecimal.ZERO;
        if (loesungsversuchKosten == null) loesungsversuchKosten = BigDecimal.ZERO;
        
        BigDecimal gesamtKosten = chatKosten.add(loesungsversuchKosten);

        return FortschrittOverviewDTO.builder()
                .belegungId(belegung.getId())
                .studentId(student.getId())
                .studentName(student.getVorname() + " " + student.getNachname())
                .studentEmail(student.getEmail())
                .aktiv(isAktiv)
                .belegungDatum(belegung.getStartDatum().atStartOfDay()) // LocalDate zu LocalDateTime konvertieren
                .gesamtAufgaben(gesamtTeilaufgaben)
                .abgeschlosseneAufgaben(abgeschlosseneTeilaufgaben)
                .fortschrittProzent(fortschrittProzent)
                .anzahlNachrichten(anzahlNachrichten)
                .aiKostenGesamt(gesamtKosten)
                .letzteAktivitaet(letzteAktivitaet)
                .build();
    }

    /**
     * Erstellt eine detaillierte Ansicht des Fortschritts für einen bestimmten Studenten in einem Kurs.
     * 
     * @param studentId Die ID des Studenten
     * @param kursId Die ID des Kurses
     * @return DetailDTO mit allen Lösungsversuchen und Chat-Nachrichten
     */
    public StudentDetailDTO getStudentDetail(Long studentId, Long kursId) {
        // Entities laden
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student nicht gefunden"));
        Kurs kurs = kursRepository.findById(kursId)
                .orElseThrow(() -> new IllegalArgumentException("Kurs nicht gefunden"));
        
        // Belegung prüfen
        Belegung belegung = belegungRepository.findByStudentAndKurs(student, kurs)
                .orElseThrow(() -> new IllegalArgumentException("Student ist nicht für diesen Kurs belegt"));
        
        // Basis-Fortschrittsdaten berechnen
        StudentDTO studentDto = studentMapper.toDto(student);
        KursDTO kursDto = kursMapper.toDto(kurs);
        
        // Prüfen, ob Belegung aktiv ist
        LocalDate heute = LocalDate.now();
        boolean isAktiv = belegung.getEndDatum() == null || 
                         belegung.getEndDatum().isAfter(heute) || 
                         belegung.getEndDatum().isEqual(heute);

        // Fortschritt mit vorhandenem FortschrittService berechnen
        KursFortschrittDTO kursFortschritt = fortschrittService.berechneFortschritt(studentDto, kursDto, isAktiv);
        
        // Aufgabenfortschritt aus KursFortschrittDTO extrahieren
        int gesamtTeilaufgaben = kursFortschritt.getGesamtTeilaufgaben() != null ? kursFortschritt.getGesamtTeilaufgaben().intValue() : 0;
        int abgeschlosseneTeilaufgaben = kursFortschritt.getAbgeschlosseneTeilaufgaben() != null ? kursFortschritt.getAbgeschlosseneTeilaufgaben().intValue() : 0;
        double fortschrittProzent = kursFortschritt.getFortschrittProzent() != null ? kursFortschritt.getFortschrittProzent().doubleValue() : 0.0;

        // Chat-Nachrichten zählen
        long anzahlNachrichten = chatNachrichtRepository.countByStudentAndKurs(student, kurs);

        // Letzte Aktivität ermitteln
        List<LoesungsVersuch> loesungsVersuche = loesungsVersuchRepository
                .findByStudentAndKursOrderByZeitpunktDesc(student, kurs);
        LocalDateTime letzteAktivitaet = loesungsVersuche.isEmpty() 
                ? belegung.getStartDatum().atStartOfDay()
                : loesungsVersuche.get(0).getZeitpunkt();

        // KI-Kosten berechnen
        BigDecimal chatKosten = chatNachrichtRepository.sumKostenByStudentAndKurs(student, kurs);
        BigDecimal loesungsversuchKosten = loesungsVersuchRepository.sumKostenByStudentAndKurs(student, kurs);
        
        if (chatKosten == null) chatKosten = BigDecimal.ZERO;
        if (loesungsversuchKosten == null) loesungsversuchKosten = BigDecimal.ZERO;
        
        BigDecimal gesamtKosten = chatKosten.add(loesungsversuchKosten);

        // Detailierte Teilaufgaben-Daten sammeln
        List<TeilaufgabeDetailDTO> teilaufgaben = createTeilaufgabenDetails(student, kurs);
        
        // Chat-Übersicht erstellen
        List<ChatUebersichtDTO> chats = createChatUebersicht(student, kurs);

        return StudentDetailDTO.builder()
                .studentId(student.getId())
                .studentName(student.getVorname() + " " + student.getNachname())
                .studentEmail(student.getEmail())
                .kursId(kurs.getId())
                .kursName(kurs.getName())
                .gesamtTeilaufgaben(gesamtTeilaufgaben)
                .abgeschlosseneTeilaufgaben(abgeschlosseneTeilaufgaben)
                .fortschrittProzent(fortschrittProzent)
                .anzahlNachrichten(anzahlNachrichten)
                .aiKostenGesamt(gesamtKosten)
                .letzteAktivitaet(letzteAktivitaet)
                .teilaufgaben(teilaufgaben)
                .chats(chats)
                .build();
    }

    /**
     * Erstellt eine detaillierte Liste aller Teilaufgaben mit den zugehörigen Lösungsversuchen.
     */
    private List<TeilaufgabeDetailDTO> createTeilaufgabenDetails(Student student, Kurs kurs) {
        // Alle Teilaufgaben des Kurses finden
        List<Teilaufgabe> alleTeilaufgaben = teilaufgabeRepository.findByKursOrderByHierarchy(kurs);
        
        return alleTeilaufgaben.stream()
                .map(teilaufgabe -> createTeilaufgabeDetail(teilaufgabe, student))
                .collect(Collectors.toList());
    }

    /**
     * Erstellt ein TeilaufgabeDetailDTO für eine einzelne Teilaufgabe.
     */
    private TeilaufgabeDetailDTO createTeilaufgabeDetail(Teilaufgabe teilaufgabe, Student student) {
        // Alle Lösungsversuche für diese Teilaufgabe
        List<LoesungsVersuch> versuche = loesungsVersuchRepository
                .findByStudentAndTeilaufgabeOrderByZeitpunktDesc(student, teilaufgabe);
        
        // Status prüfen
        boolean istAbgeschlossen = loesungsVersuchRepository
                .existsByStudentAndTeilaufgabeAndIstAbgeschlossenTrueAndIstZurueckGesetztFalse(student, teilaufgabe);
        boolean istUebersprungen = loesungsVersuchRepository
                .existsByStudentAndTeilaufgabeAndIstUebersprungenTrue(student, teilaufgabe);
        
        // Lösungsversuche in DTOs konvertieren
        List<LoesungsVersuchDetailDTO> versuchDtos = versuche.stream()
                .map(this::createLoesungsVersuchDetail)
                .collect(Collectors.toList());

        return TeilaufgabeDetailDTO.builder()
                .teilaufgabeId(teilaufgabe.getId())
                .aufgabeTitel(teilaufgabe.getAufgabe().getTitel())
                .kurseinheitName(teilaufgabe.getAufgabe().getKurseinheit().getName())
                .teilaufgabeReihenfolge(teilaufgabe.getReihenfolge())
                .istAbgeschlossen(istAbgeschlossen)
                .istUebersprungen(istUebersprungen)
                .loesungsversuche(versuchDtos)
                .anzahlVersuche(versuche.size())
                .build();
    }

    /**
     * Erstellt ein LoesungsVersuchDetailDTO für einen einzelnen Lösungsversuch.
     */
    private LoesungsVersuchDetailDTO createLoesungsVersuchDetail(LoesungsVersuch versuch) {
        return LoesungsVersuchDetailDTO.builder()
                .id(versuch.getId())
                .zeitpunkt(versuch.getZeitpunkt())
                .istAbgeschlossen(versuch.getIstAbgeschlossen())
                .istUebersprungen(versuch.getIstUebersprungen())
                .istZurueckGesetzt(versuch.getIstZurueckGesetzt())
                .bewertungPunkte(versuch.getBewertungPunkte())
                .bewertungFeedback(versuch.getBewertungFeedback())
                .bewertungFelderFarbe(versuch.getBewertungFelderFarbe())
                .loesungFelder(versuch.getLoesungFelder())
                .kosten(versuch.getKosten())
                .build();
    }

    /**
     * Erstellt eine Übersicht aller Chat-Aktivitäten gruppiert nach Teilaufgaben.
     */
    private List<ChatUebersichtDTO> createChatUebersicht(Student student, Kurs kurs) {
        // Alle Chats des Studenten im Kurs finden
        List<Chat> chats = chatRepository.findByStudentAndKurs(student, kurs);
        
        return chats.stream()
                .map(chat -> createChatUebersicht(chat))
                .collect(Collectors.toList());
    }

    /**
     * Erstellt ein ChatUebersichtDTO für einen einzelnen Chat.
     */
    private ChatUebersichtDTO createChatUebersicht(Chat chat) {
        Teilaufgabe teilaufgabe = chat.getTeilaufgabe();
        
        // Chat-Nachrichten laden (sortiert nach Zeitpunkt)
        List<ChatNachricht> nachrichten = chatNachrichtRepository
                .findByChatOrderByZeitpunktAsc(chat);
        
        // Letzte Nachricht ermitteln
        LocalDateTime letzteNachrichtZeitpunkt = nachrichten.isEmpty() 
                ? null 
                : nachrichten.get(nachrichten.size() - 1).getZeitpunkt();
        
        // Kosten berechnen
        BigDecimal gesamtKosten = nachrichten.stream()
                .filter(n -> n.getKosten() != null)
                .map(ChatNachricht::getKosten)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Nachrichten in DTOs konvertieren
        List<ChatNachrichtDetailDTO> nachrichtDtos = nachrichten.stream()
                .map(this::createChatNachrichtDetail)
                .collect(Collectors.toList());

        return ChatUebersichtDTO.builder()
                .teilaufgabeId(teilaufgabe.getId())
                .aufgabeTitel(teilaufgabe.getAufgabe().getTitel())
                .kurseinheitName(teilaufgabe.getAufgabe().getKurseinheit().getName())
                .teilaufgabeReihenfolge(teilaufgabe.getReihenfolge())
                .chatId(chat.getId())
                .anzahlNachrichten(nachrichten.size())
                .letzteNachrichtZeitpunkt(letzteNachrichtZeitpunkt)
                .gesamtKosten(gesamtKosten)
                .nachrichten(nachrichtDtos)
                .build();
    }

    /**
     * Erstellt ein ChatNachrichtDetailDTO für eine einzelne Chat-Nachricht.
     */
    private ChatNachrichtDetailDTO createChatNachrichtDetail(ChatNachricht nachricht) {
        String inhaltHtml = null;
        
        // Markdown für System-Nachrichten rendern
        if (nachricht.getIstSystemNachricht() && nachricht.getInhalt() != null) {
            var renderResult = markdownRenderer.renderMarkdown(
                nachricht.getInhalt(), 
                InputFieldNodeRenderer.RenderMode.PREVIEW,
                null // Keine spezifische Kurseinheit für Chat-Nachrichten
            );
            inhaltHtml = renderResult.getHtml();
        }
        
        // Referenzen laden und mappen
        List<ChatNachrichtReferenz> referenzen = chatNachrichtReferenzRepository.findByChatNachricht(nachricht);
        
        return ChatNachrichtDetailDTO.builder()
                .id(nachricht.getId())
                .zeitpunkt(nachricht.getZeitpunkt())
                .inhalt(nachricht.getInhalt())
                .inhaltHtml(inhaltHtml)
                .istSystemNachricht(nachricht.getIstSystemNachricht())
                .kosten(nachricht.getKosten())
                .referenzen(chatNachrichtReferenzMapper.toDtoList(referenzen))
                .build();
    }
}