package de.fuh.kn.webapp.common.aktivitaeten;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetMapper;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.persistence.entity.Aktivitaet;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import de.fuh.kn.webapp.persistence.entity.Nutzer;
import de.fuh.kn.webapp.persistence.repository.AktivitaetRepository;
import de.fuh.kn.webapp.persistence.repository.NutzerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

//TODO Add javadoc
@Service
@Slf4j
@RequiredArgsConstructor
public class AktivitaetsProtokollierungService {

    private final ObjectMapper objectMapper;
    private final AktivitaetRepository aktivitaetRepository;
    private final NutzerRepository nutzerRepository;
    private final AktivitaetMapper aktivitaetMapper;

    /**
     * Protokolliert eine neue Aktivität im System mit zusätzlichen Details und Objektreferenz.
     *
     * @param nutzerDTO Der Nutzer, der die Aktivität ausgeführt hat
     * @param aktivitaetsTyp Der Typ der Aktivität
     * @param beschreibung Eine textuelle Beschreibung der Aktivität
     * @param detailsMap Eine Map mit zusätzlichen Details zur Aktivität
     * @param erfolg Flag, ob die Aktivität erfolgreich war
     * @param referenzTyp Der Klassenname des referenzierten Objekts
     * @param referenzId Die ID des referenzierten Objekts
     * @return Die erstellte Aktivität
     */
    @Transactional
    public AktivitaetDTO protokolliereAktivitaet(NutzerDTO nutzerDTO, AktivitaetsTyp aktivitaetsTyp,
                                                 String beschreibung, Map<String, Object> detailsMap,
                                                 Boolean erfolg, String referenzTyp, Long referenzId) {

        Nutzer nutzer = null;
        if(nutzerDTO != null) {
            Optional<Nutzer> optionalNutzer = nutzerRepository.findById(nutzerDTO.getId());
            if (optionalNutzer.isPresent()) {
                nutzer = optionalNutzer.get();
            }
        }


        Aktivitaet aktivitaet = new Aktivitaet();
        aktivitaet.setNutzer(nutzer);
        aktivitaet.setAktivitaetsTyp(aktivitaetsTyp);
        aktivitaet.setBeschreibung(beschreibung);
        aktivitaet.setZeitpunkt(LocalDateTime.now());
        aktivitaet.setErfolg(erfolg);
        aktivitaet.setReferenzTyp(referenzTyp);
        aktivitaet.setReferenzId(referenzId);

        // Konvertierung der Details-Map zu JSON, falls vorhanden
        if (detailsMap != null && !detailsMap.isEmpty()) {
            try {
                aktivitaet.setDetails(objectMapper.writeValueAsString(detailsMap));
            } catch (JsonProcessingException e) {
                log.error("Fehler beim Konvertieren der Aktivitätsdetails zu JSON", e);
                aktivitaet.setDetails("Fehler bei Details-Serialisierung");
            }
        }

        Aktivitaet savedAktivitaet = aktivitaetRepository.save(aktivitaet);
        return aktivitaetMapper.toDTO(savedAktivitaet);
    }


}
