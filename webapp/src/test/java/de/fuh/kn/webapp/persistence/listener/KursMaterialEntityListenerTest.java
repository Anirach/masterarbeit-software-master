package de.fuh.kn.webapp.persistence.listener;

import de.fuh.kn.webapp.llm.rag.event.KursMaterialEvent;
import de.fuh.kn.webapp.persistence.entity.KursMaterial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KursMaterialEntityListenerTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Captor
    private ArgumentCaptor<KursMaterialEvent> eventCaptor;

    private KursMaterialEntityListener listener;

    @BeforeEach
    void setUp() {
        listener = new KursMaterialEntityListener();
        listener.setEventPublisher(eventPublisher);
    }

    @Test
    @DisplayName("PostRemove - Dokument wird gelöscht, Event wird gefeuert")
    void postRemove_MitDokumentTyp_SollteEventFeuern() {
        // Given
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setId(100L);
        kursMaterial.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        kursMaterial.setName("test.pdf");

        // When
        listener.postRemove(kursMaterial);

        // Then
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        KursMaterialEvent firedEvent = eventCaptor.getValue();
        assertThat(firedEvent).isNotNull();
        assertThat(firedEvent.getOperation()).isEqualTo(KursMaterialEvent.KursMaterialOperation.DELETE);
        assertThat(firedEvent.getKursMaterialId()).isEqualTo(100L);
        assertThat(firedEvent.getSource()).isEqualTo(listener);
    }

    @Test
    @DisplayName("PostRemove - Bild wird gelöscht, kein Event wird gefeuert")
    void postRemove_MitBildTyp_SollteKeinEventFeuern() {
        // Given
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setId(100L);
        kursMaterial.setTyp(KursMaterial.KursMaterialTyp.BILD);
        kursMaterial.setName("test.jpg");

        // When
        listener.postRemove(kursMaterial);

        // Then
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("PostRemove - Ohne ID")
    void postRemove_OhneId_SollteKeinEventFeuern() {
        // Given
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        kursMaterial.setName("test.pdf");
        // ID ist null

        // When
        listener.postRemove(kursMaterial);

        // Then
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("PostRemove - EventPublisher ist null")
    void postRemove_MitNullEventPublisher_SollteNichtAbstuerzen() {
        // Given
        listener.setEventPublisher(null);
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setId(100L);
        kursMaterial.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);

        // When
        listener.postRemove(kursMaterial);

        // Then - sollte keine Exception werfen
        // EventPublisher ist null, also kann auch kein Event gefeuert werden
    }

    @Test
    @DisplayName("SetEventPublisher - Setzt statischen EventPublisher")
    void setEventPublisher_SollteStatischenPublisherSetzen() {
        // Given
        ApplicationEventPublisher newPublisher = mock(ApplicationEventPublisher.class);

        // When
        listener.setEventPublisher(newPublisher);

        // Then
        // Teste mit einem neuen Listener, ob der statische Publisher gesetzt wurde
        KursMaterialEntityListener newListener = new KursMaterialEntityListener();
        KursMaterial kursMaterial = new KursMaterial();
        kursMaterial.setId(200L);
        kursMaterial.setTyp(KursMaterial.KursMaterialTyp.DOKUMENT);
        
        newListener.postRemove(kursMaterial);
        
        verify(newPublisher).publishEvent(any(KursMaterialEvent.class));
    }

    @Test
    @DisplayName("PostRemove - Verschiedene KursMaterial Typen")
    void postRemove_MitVerschiedenenTypen_SollteNurBeiDokumentEventFeuern() {
        // Given
        KursMaterial dokument = createKursMaterial(1L, KursMaterial.KursMaterialTyp.DOKUMENT);
        KursMaterial bild = createKursMaterial(2L, KursMaterial.KursMaterialTyp.BILD);
        KursMaterial nullTyp = createKursMaterial(3L, null);

        // When
        listener.postRemove(dokument);
        listener.postRemove(bild);
        listener.postRemove(nullTyp);

        // Then
        verify(eventPublisher, times(1)).publishEvent(any(KursMaterialEvent.class));
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getKursMaterialId()).isEqualTo(1L);
    }

    // Helper method
    private KursMaterial createKursMaterial(Long id, KursMaterial.KursMaterialTyp typ) {
        KursMaterial material = new KursMaterial();
        material.setId(id);
        material.setTyp(typ);
        material.setName("material_" + id);
        return material;
    }
}