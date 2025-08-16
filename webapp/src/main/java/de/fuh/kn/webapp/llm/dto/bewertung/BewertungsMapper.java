package de.fuh.kn.webapp.llm.dto.bewertung;

import de.fuh.kn.webapp.persistence.entity.LoesungsVersuch;
import de.fuh.kn.webapp.persistence.entity.Teilaufgabe;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper für die Konvertierung zwischen Entitäten und DTOs im Kontext der LLM-Bewertung.
 */
@Mapper
public interface BewertungsMapper {

    /**
     * Erstellt ein BewertungsRequestDto aus einer Teilaufgabe und einem Lösungsversuch.
     *
     * @param teilaufgabe Die Teilaufgabe, die bewertet werden soll
     * @param loesungsVersuch Der Lösungsversuch, der bewertet werden soll
     * @return Das BewertungsRequestDto für die LLM-Anfrage
     */
    @Mapping(source = "teilaufgabe.id", target = "teilaufgabeId")
    @Mapping(source = "teilaufgabe.aufgabe.aufgabenText", target = "aufgabenstellungAufgabe")
    @Mapping(source = "teilaufgabe.aufgabenstellungMarkdown", target = "aufgabenstellungTeilaufgabe")
    @Mapping(source = "teilaufgabe.musterloesungBewertungshinweise", target = "bewertungshinweise")
    @Mapping(source = "teilaufgabe.musterloesungFelder", target = "musterloesungFelder")
    @Mapping(source = "loesungsVersuch.loesungFelder", target = "loesungFelder")
    @Mapping(source = "loesungsVersuch.student.id", target = "studentId")
    BewertungsRequestDto createRequestDto(Teilaufgabe teilaufgabe, LoesungsVersuch loesungsVersuch);

    /**
     * Aktualisiert einen Lösungsversuch mit den Bewertungsergebnissen.
     *
     * @param loesungsVersuch Der zu aktualisierende Lösungsversuch
     * @param responseDto Die Bewertungsergebnisse
     * @return Der aktualisierte Lösungsversuch
     */
    @Mapping(source = "responseDto.punkte", target = "bewertungPunkte")
    @Mapping(source = "responseDto.feedback", target = "bewertungFeedback")
    @Mapping(source = "responseDto.felderBewertung", target = "bewertungFelderFarbe")
    @Mapping(source = "responseDto.inputToken", target = "inputToken")
    @Mapping(source = "responseDto.outputToken", target = "outputToken")
    @Mapping(source = "responseDto.model", target = "modell")
    @Mapping(source = "responseDto.cost", target = "kosten")
    LoesungsVersuch updateLoesungsVersuch(LoesungsVersuch loesungsVersuch, BewertungsResponseDto responseDto);

    /**
     * Summiert die Input- und Output-Token.
     *
     * @param inputToken Die Anzahl der Input-Token
     * @param outputToken Die Anzahl der Output-Token
     * @return Die Summe aus Input- und Output-Token
     */
    default Integer sumTokens(Integer inputToken, Integer outputToken) {
        if (inputToken == null) {
            return outputToken;
        }
        if (outputToken == null) {
            return inputToken;
        }
        return inputToken + outputToken;
    }
}