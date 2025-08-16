package de.fuh.kn.webapp.common.aktivitaeten;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fuh.kn.webapp.common.dto.BaseDTO;
import de.fuh.kn.webapp.nutzerverwaltung.auth.KursbetreuerUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import de.fuh.kn.webapp.persistence.entity.AktivitaetsTyp;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testklasse für den AktivitaetsProtokollierungsAspect.
 * Testet die aspektorientierte Protokollierung von Aktivitäten.
 */
@ExtendWith(MockitoExtension.class)
public class AktivitaetsProtokollierungsAspectTest {

    @Mock
    private AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    @Mock
    private AktivitaetReferenzHelper aktivitaetReferenzHelper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NutzerService nutzerService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AktivitaetsProtokollierungAspect aspect;

    @BeforeEach
    void setUp() {
        // Mock SecurityContextHolder
        SecurityContextHolder.setContext(securityContext);
    }

    /**
     * Testet die Protokollierung einer Aktivität mit einem authentifizierten Nutzer.
     * Verifiziert, dass die korrekten Daten an den AktivitaetsProtokollierungService weitergegeben werden.
     */
    @Test
    void testProtokolliereAktivitaetMitAuthentifiziertemNutzer() throws Throwable {
        // Arrange
        Long nutzerId = 1L;
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(nutzerId);
        nutzerDTO.setVorname("Test");
        nutzerDTO.setNachname("Nutzer");
        
        // Mock Authentication
        KursbetreuerUserDetails userDetails = mock(KursbetreuerUserDetails.class);
        when(userDetails.getId()).thenReturn(nutzerId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        // Mock NutzerService
        when(nutzerService.getNutzerById(nutzerId)).thenReturn(Optional.of(nutzerDTO));
        
        // Mock ProceedingJoinPoint und MethodSignature
        Method method = TestController.class.getMethod("testMethodeMitAnnotation", String.class);
        ProtokolliereAktivitaet annotation = method.getAnnotation(ProtokolliereAktivitaet.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"testParameter"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Testdaten"});
        
        // Mock für AktivitaetsProtokollierungService
        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(1L);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(
            any(NutzerDTO.class), 
            eq(AktivitaetsTyp.LOGIN),
            anyString(), 
            anyMap(), 
            eq(true), 
            isNull(), 
            isNull()
        )).thenReturn(aktivitaetDTO);
        
        // Mock für den Rückgabewert von joinPoint.proceed()
        when(joinPoint.proceed()).thenReturn("Ergebnis");
        
        // Act
        Object result = aspect.protokolliereAktivitaet(joinPoint);
        
        // Assert
        assertEquals("Ergebnis", result, "Der Rückgabewert der Originalmethode sollte weitergegeben werden");
        
        // Capture der an den Service übergebenen Argumente
        ArgumentCaptor<NutzerDTO> nutzerCaptor = ArgumentCaptor.forClass(NutzerDTO.class);
        ArgumentCaptor<AktivitaetsTyp> typCaptor = ArgumentCaptor.forClass(AktivitaetsTyp.class);
        ArgumentCaptor<String> beschreibungCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
            nutzerCaptor.capture(),
            typCaptor.capture(),
            beschreibungCaptor.capture(),
            detailsCaptor.capture(),
            eq(true),
            isNull(),
            isNull()
        );
        
        // Überprüfe die übergebenen Argumente
        assertEquals(nutzerId, nutzerCaptor.getValue().getId(), "Der Nutzer sollte korrekt übergeben werden");
        assertEquals(AktivitaetsTyp.LOGIN, typCaptor.getValue(), "Der AktivitaetsTyp sollte korrekt sein");
        assertEquals("Test-Beschreibung mit Parameter: Testdaten", beschreibungCaptor.getValue(), "Die Beschreibung sollte formatiert worden sein");
        
        // Überprüfe Details
        Map<String, Object> details = detailsCaptor.getValue();
        assertNotNull(details, "Details sollten nicht null sein");
        assertEquals("testMethodeMitAnnotation", details.get("methode"), "Der Methodenname sollte in den Details sein");
        assertTrue(details.containsKey("klasse"), "Die Klasse sollte in den Details sein");
        
        // Überprüfe Parameter, falls mitParametern=true
        if (annotation.mitParametern()) {
            assertTrue(details.containsKey("parameter"), "Parameter sollten in den Details sein");
            Map<String, Object> parameter = (Map<String, Object>) details.get("parameter");
            assertEquals("Testdaten", parameter.get("testParameter"), "Der Parameter sollte korrekt in den Details sein");
        }
    }
    
    // Private Testklasse für die Annotation
    private static class TestController {
        @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.LOGIN,
            beschreibung = "Test-Beschreibung mit Parameter: {0}",
            mitParametern = true
        )
        public String testMethodeMitAnnotation(String testParameter) {
            return "Ergebnis";
        }
    }
    
    /**
     * Testet die Protokollierung einer Aktivität, bei der die Originalmethode eine Exception wirft.
     * Verifiziert, dass die Exception korrekt weitergegeben und der Fehler protokolliert wird.
     */
    @Test
    void testProtokolliereAktivitaetMitException() throws Throwable {
        // Arrange
        Long nutzerId = 1L;
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(nutzerId);
        
        // Mock Authentication
        KursbetreuerUserDetails userDetails = mock(KursbetreuerUserDetails.class);
        when(userDetails.getId()).thenReturn(nutzerId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        // Mock NutzerService
        when(nutzerService.getNutzerById(nutzerId)).thenReturn(Optional.of(nutzerDTO));
        
        // Mock ProceedingJoinPoint und MethodSignature
        Method method = TestController.class.getMethod("testMethodeMitAnnotation", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"testParameter"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Testdaten"});
        
        // Mock für AktivitaetsProtokollierungService
        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(1L);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(
            any(NutzerDTO.class), 
            any(AktivitaetsTyp.class), 
            anyString(), 
            anyMap(), 
            eq(false), 
            isNull(), 
            isNull()
        )).thenReturn(aktivitaetDTO);
        
        // Exception simulieren
        RuntimeException testException = new RuntimeException("Test-Exception");
        when(joinPoint.proceed()).thenThrow(testException);
        
        // Act & Assert
        RuntimeException thrownException = assertThrows(RuntimeException.class, () -> {
            aspect.protokolliereAktivitaet(joinPoint);
        }, "Die Exception sollte durchgereicht werden");
        
        assertEquals(testException, thrownException, "Es sollte die gleiche Exception sein");
        
        // Capture der an den Service übergebenen Argumente
        ArgumentCaptor<Map<String, Object>> detailsCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Boolean> erfolgCaptor = ArgumentCaptor.forClass(Boolean.class);
        
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
            any(NutzerDTO.class),
            any(AktivitaetsTyp.class),
            anyString(),
            detailsCaptor.capture(),
            erfolgCaptor.capture(),
            isNull(),
            isNull()
        );
        
        // Überprüfe, dass der Fehler protokolliert wurde
        Map<String, Object> details = detailsCaptor.getValue();
        assertFalse(erfolgCaptor.getValue(), "Das Erfolg-Flag sollte false sein");
        assertNotNull(details, "Details sollten nicht null sein");
        assertEquals("Test-Exception", details.get("fehler"), "Die Fehlermeldung sollte in den Details sein");
    }
    
    /**
     * Testet die Protokollierung einer Aktivität ohne authentifizierten Nutzer.
     * Verifiziert, dass die Aktivität trotzdem protokolliert wird, nur ohne Nutzerkontext.
     */
    @Test
    void testProtokolliereAktivitaetOhneAuthentifiziertenNutzer() throws Throwable {
        // Arrange
        // Security Context ohne Authentifizierung simulieren
        when(securityContext.getAuthentication()).thenReturn(null);
        
        // Mock ProceedingJoinPoint und MethodSignature
        Method method = TestController.class.getMethod("testMethodeMitAnnotation", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"testParameter"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Testdaten"});
        
        // Mock für den Rückgabewert von joinPoint.proceed()
        when(joinPoint.proceed()).thenReturn("Ergebnis");
        
        // Mock für AktivitaetsProtokollierungService
        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(1L);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(
            isNull(), 
            any(AktivitaetsTyp.class), 
            anyString(), 
            anyMap(), 
            eq(true), 
            isNull(), 
            isNull()
        )).thenReturn(aktivitaetDTO);
        
        // Act
        Object result = aspect.protokolliereAktivitaet(joinPoint);
        
        // Assert
        assertEquals("Ergebnis", result, "Der Rückgabewert der Originalmethode sollte weitergegeben werden");
        
        // Verifiziere, dass die Aktivität ohne Nutzer protokolliert wurde
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
            isNull(),
            any(AktivitaetsTyp.class),
            anyString(),
            anyMap(),
            eq(true),
            isNull(),
            isNull()
        );
    }
    
    /**
     * Testet die Protokollierung einer Aktivität mit einem DTO als Parameter.
     * Verifiziert die korrekte Handhabung von Referenzen und DTO-Werten.
     */
    @Test
    void testProtokolliereAktivitaetMitDTOParameter() throws Throwable {
        // Arrange
        Long nutzerId = 1L;
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(nutzerId);
        
        // Test-DTO erstellen
        TestDTO testDTO = new TestDTO();
        testDTO.setId(2L);
        testDTO.setName("Test-DTO");
        
        // Mock Authentication
        KursbetreuerUserDetails userDetails = mock(KursbetreuerUserDetails.class);
        when(userDetails.getId()).thenReturn(nutzerId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        // Mock NutzerService
        when(nutzerService.getNutzerById(nutzerId)).thenReturn(Optional.of(nutzerDTO));
        
        // Mock für AktivitaetReferenzHelper
        when(aktivitaetReferenzHelper.toString(testDTO)).thenReturn("Test-DTO [ID=2]");
        
        // Mock ProceedingJoinPoint und MethodSignature
        Method method = TestControllerErweitert.class.getMethod("testMethodeMitDTOParameter", TestDTO.class);
        ProtokolliereAktivitaet annotation = method.getAnnotation(ProtokolliereAktivitaet.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"testDTO"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{testDTO});
        
        // Mock für den Rückgabewert von joinPoint.proceed()
        when(joinPoint.proceed()).thenReturn(testDTO);
        
        // Mock für AktivitaetsProtokollierungService
        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(1L);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(
            any(NutzerDTO.class),
            eq(AktivitaetsTyp.LOGIN),
            anyString(),
            anyMap(),
            eq(true),
            eq(testDTO.getEntityTypeName()),
            eq(testDTO.getId())
        )).thenReturn(aktivitaetDTO);
        
        // Act
        Object result = aspect.protokolliereAktivitaet(joinPoint);
        
        // Assert
        assertEquals(testDTO, result, "Der Rückgabewert der Originalmethode sollte weitergegeben werden");
        
        // Capture der an den Service übergebenen Argumente
        ArgumentCaptor<String> beschreibungCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> referenzTypCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> referenzIdCaptor = ArgumentCaptor.forClass(Long.class);
        
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
            any(NutzerDTO.class),
            eq(AktivitaetsTyp.LOGIN),
            beschreibungCaptor.capture(),
            anyMap(),
            eq(true),
            referenzTypCaptor.capture(),
            referenzIdCaptor.capture()
        );
        
        // Überprüfe, dass die DTO-Referenz korrekt gesetzt wurde
        assertEquals("TestDTO", referenzTypCaptor.getValue(), "Der ReferenzTyp sollte dem EntityTypeName des DTOs entsprechen");
        assertEquals(2L, referenzIdCaptor.getValue(), "Die ReferenzId sollte der ID des DTOs entsprechen");
        assertEquals("DTO bearbeitet: Test-DTO [ID=2]", beschreibungCaptor.getValue(), "Die Beschreibung sollte den formatierten DTO-String enthalten");
        
        // Verifiziere, dass der ReferenzHelper aufgerufen wurde
        verify(aktivitaetReferenzHelper, times(2)).toString(testDTO);
    }
    
    /**
     * Testet die Protokollierung mit einem Model-Parameter, der DTOs enthält.
     * Überprüft, dass DTOs aus dem Model als Referenz extrahiert werden.
     */
    @Test
    void testProtokolliereAktivitaetMitModelParameter() throws Throwable {
        // Arrange
        Long nutzerId = 1L;
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(nutzerId);
        
        // Test-DTO erstellen, das im Model enthalten sein wird
        TestDTO testDTO = new TestDTO();
        testDTO.setId(3L);
        testDTO.setName("Model-DTO");
        
        // Mock Authentication
        KursbetreuerUserDetails userDetails = mock(KursbetreuerUserDetails.class);
        when(userDetails.getId()).thenReturn(nutzerId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        // Mock NutzerService
        when(nutzerService.getNutzerById(nutzerId)).thenReturn(Optional.of(nutzerDTO));
        
        // Model erstellen
        Model model = mock(Model.class);
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put("testDTO", testDTO);
        when(model.asMap()).thenReturn(modelMap);
        
        // Mock ProceedingJoinPoint und MethodSignature
        Method method = TestControllerErweitert.class.getMethod("testMethodeMitModelParameter", Model.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{model});
        
        // Mock für den Rückgabewert von joinPoint.proceed()
        when(joinPoint.proceed()).thenReturn("viewName");
        
        // Mock für AktivitaetsProtokollierungService
        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(1L);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(
            any(NutzerDTO.class),
            any(AktivitaetsTyp.class),
            anyString(),
            anyMap(),
            eq(true),
            eq(testDTO.getEntityTypeName()),
            eq(testDTO.getId())
        )).thenReturn(aktivitaetDTO);
        
        // Act
        Object result = aspect.protokolliereAktivitaet(joinPoint);
        
        // Assert
        assertEquals("viewName", result, "Der Rückgabewert der Originalmethode sollte weitergegeben werden");
        
        // Capture der an den Service übergebenen Argumente
        ArgumentCaptor<String> referenzTypCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> referenzIdCaptor = ArgumentCaptor.forClass(Long.class);
        
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
            any(NutzerDTO.class),
            any(AktivitaetsTyp.class),
            anyString(),
            anyMap(),
            eq(true),
            referenzTypCaptor.capture(),
            referenzIdCaptor.capture()
        );
        
        // Überprüfe, dass die DTO-Referenz aus dem Model extrahiert wurde
        assertEquals("TestDTO", referenzTypCaptor.getValue(), "Der ReferenzTyp sollte dem EntityTypeName des DTOs im Model entsprechen");
        assertEquals(3L, referenzIdCaptor.getValue(), "Die ReferenzId sollte der ID des DTOs im Model entsprechen");
    }
    
    /**
     * Testet den Fall, dass ein Nutzer im SecurityContext gefunden wird, 
     * aber dieser Nutzer nicht in der Datenbank existiert.
     */
    @Test
    void testProtokolliereAktivitaetMitNichtExistierendemNutzer() throws Throwable {
        // Arrange
        Long nutzerId = 999L; // Nicht existierende Nutzer-ID
        
        // Mock Authentication
        KursbetreuerUserDetails userDetails = mock(KursbetreuerUserDetails.class);
        when(userDetails.getId()).thenReturn(nutzerId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        // Mock NutzerService - Nutzer existiert nicht in DB
        when(nutzerService.getNutzerById(nutzerId)).thenReturn(Optional.empty());
        
        // Mock ProceedingJoinPoint und MethodSignature
        Method method = TestController.class.getMethod("testMethodeMitAnnotation", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"testParameter"});
        when(joinPoint.getArgs()).thenReturn(new Object[]{"Testdaten"});
        
        // Mock für den Rückgabewert von joinPoint.proceed()
        when(joinPoint.proceed()).thenReturn("Ergebnis");
        
        // Mock für AktivitaetsProtokollierungService
        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(1L);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(
            isNull(), 
            any(AktivitaetsTyp.class), 
            anyString(), 
            anyMap(), 
            eq(true), 
            isNull(), 
            isNull()
        )).thenReturn(aktivitaetDTO);
        
        // Act
        Object result = aspect.protokolliereAktivitaet(joinPoint);
        
        // Assert
        assertEquals("Ergebnis", result, "Der Rückgabewert der Originalmethode sollte weitergegeben werden");
        
        // Verifiziere, dass die Aktivität ohne Nutzer protokolliert wurde (nutzerDTO = null)
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
            isNull(),
            any(AktivitaetsTyp.class),
            anyString(),
            anyMap(),
            eq(true),
            isNull(),
            isNull()
        );
    }
    
    /**
     * Testet den Fall, dass ein DTO als Rückgabewert verwendet wird und dessen
     * Referenzdaten für die Aktivitätsprotokollierung verwendet werden.
     */
    @Test
    void testProtokolliereAktivitaetMitDTORueckgabewert() throws Throwable {
        // Arrange
        Long nutzerId = 1L;
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(nutzerId);
        
        // Test-DTO für Rückgabewert
        TestDTO returnDTO = new TestDTO();
        returnDTO.setId(5L);
        returnDTO.setName("Return-DTO");
        
        // Mock Authentication
        KursbetreuerUserDetails userDetails = mock(KursbetreuerUserDetails.class);
        when(userDetails.getId()).thenReturn(nutzerId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        // Mock NutzerService
        when(nutzerService.getNutzerById(nutzerId)).thenReturn(Optional.of(nutzerDTO));
        
        // Mock ProceedingJoinPoint und MethodSignature
        Method method = TestControllerErweitert.class.getMethod("testMethodeMitRueckgabeDTO");
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(joinPoint.getArgs()).thenReturn(new Object[]{});
        
        // Mock für den Rückgabewert von joinPoint.proceed()
        when(joinPoint.proceed()).thenReturn(returnDTO);
        
        // Mock für AktivitaetsProtokollierungService
        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(1L);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(
            any(NutzerDTO.class),
            any(AktivitaetsTyp.class),
            anyString(),
            anyMap(),
            eq(true),
            eq(returnDTO.getEntityTypeName()),
            eq(returnDTO.getId())
        )).thenReturn(aktivitaetDTO);
        
        // Act
        Object result = aspect.protokolliereAktivitaet(joinPoint);
        
        // Assert
        assertEquals(returnDTO, result, "Der Rückgabewert sollte das DTO sein");
        
        // Capture der an den Service übergebenen Argumente
        ArgumentCaptor<String> referenzTypCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> referenzIdCaptor = ArgumentCaptor.forClass(Long.class);
        
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
            any(NutzerDTO.class),
            any(AktivitaetsTyp.class),
            anyString(),
            anyMap(),
            eq(true),
            referenzTypCaptor.capture(),
            referenzIdCaptor.capture()
        );
        
        // Überprüfe, dass die DTO-Referenz aus dem Rückgabewert extrahiert wurde
        assertEquals("TestDTO", referenzTypCaptor.getValue(), "Der ReferenzTyp sollte dem EntityTypeName des Rückgabe-DTOs entsprechen");
        assertEquals(5L, referenzIdCaptor.getValue(), "Die ReferenzId sollte der ID des Rückgabe-DTOs entsprechen");
    }
    
    /**
     * Testet die Verarbeitung von Model-Attributen, die für die Aktivitätsprotokollierung 
     * mit dem Präfix "aktivitaetsprotokollierung-arg-" markiert wurden.
     */
    @Test
    void testProtokolliereAktivitaetMitModelAttributeFuerArgs() throws Throwable {
        // Arrange
        Long nutzerId = 1L;
        NutzerDTO nutzerDTO = new NutzerDTO();
        nutzerDTO.setId(nutzerId);
        
        // Mock Authentication
        KursbetreuerUserDetails userDetails = mock(KursbetreuerUserDetails.class);
        when(userDetails.getId()).thenReturn(nutzerId);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        // Mock NutzerService
        when(nutzerService.getNutzerById(nutzerId)).thenReturn(Optional.of(nutzerDTO));
        
        // Model mit speziellen Attributen für Aktivitätsprotokollierung erstellen
        Model model = mock(Model.class);
        Map<String, Object> modelMap = new HashMap<>();
        // Spezielle Attribute mit dem Präfix "aktivitaetsprotokollierung-arg-" hinzufügen
        String specialValue = "Spezieller Wert aus dem Model";
        modelMap.put("aktivitaetsprotokollierung-arg-0", specialValue);
        when(model.asMap()).thenReturn(modelMap);
        
        // Original-Argumente, die später ersetzt werden
        String originalArg = "Ursprünglicher Wert";
        Object[] originalArgs = new Object[]{originalArg, model};
        
        // Mock ProceedingJoinPoint und MethodSignature
        Method method = TestControllerErweitert.class.getMethod("testMethodeMitModelAttributArg", String.class, Model.class);
        ProtokolliereAktivitaet annotation = method.getAnnotation(ProtokolliereAktivitaet.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getParameterNames()).thenReturn(new String[]{"param", "model"});
        when(joinPoint.getArgs()).thenReturn(originalArgs);
        
        // Mock für den Rückgabewert von joinPoint.proceed()
        when(joinPoint.proceed()).thenReturn("Ergebnis");
        
        // Mock für AktivitaetsProtokollierungService
        AktivitaetDTO aktivitaetDTO = new AktivitaetDTO();
        aktivitaetDTO.setId(1L);
        when(aktivitaetsProtokollierungService.protokolliereAktivitaet(
            any(NutzerDTO.class),
            any(AktivitaetsTyp.class),
            anyString(),
            anyMap(),
            eq(true),
            isNull(),
            isNull()
        )).thenReturn(aktivitaetDTO);
        
        // Act
        Object result = aspect.protokolliereAktivitaet(joinPoint);
        
        // Assert
        assertEquals("Ergebnis", result, "Der Rückgabewert sollte korrekt sein");
        
        // Capture der an den Service übergebenen Argumente
        ArgumentCaptor<String> beschreibungCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(aktivitaetsProtokollierungService).protokolliereAktivitaet(
            any(NutzerDTO.class),
            any(AktivitaetsTyp.class),
            beschreibungCaptor.capture(),
            anyMap(),
            eq(true),
            isNull(),
            isNull()
        );
        
        // Überprüfe, dass der spezielle Wert aus dem Model in der Beschreibung verwendet wurde
        assertEquals("Model-Attribut verwendet: " + specialValue, beschreibungCaptor.getValue(),
                "Die Beschreibung sollte den speziellen Wert aus dem Model enthalten");
    }
    
    // Hilfsklasse für Tests mit DTOs
    private static class TestDTO extends BaseDTO {
        private String name;
        
        public TestDTO() {
            super();
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getName() {
            return name;
        }
        
        @Override
        public String getEntityTypeName() {
            return "TestDTO";
        }
        
        @Override
        public String getEntityDisplayName() {
            return name + " [ID=" + getId() + "]";
        }
    }
    
    // Erweiterte TestController-Klasse für die neuen Testmethoden
    private static class TestControllerErweitert {
        @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.LOGIN,
            beschreibung = "DTO bearbeitet: {0}",
            mitParametern = true
        )
        public TestDTO testMethodeMitDTOParameter(TestDTO testDTO) {
            return testDTO;
        }
        
        @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.LOGIN,
            beschreibung = "View angezeigt",
            mitParametern = false
        )
        public String testMethodeMitModelParameter(Model model) {
            return "viewName";
        }
        
        @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.LOGIN,
            beschreibung = "DTO abgerufen",
            mitParametern = false
        )
        public TestDTO testMethodeMitRueckgabeDTO() {
            TestDTO dto = new TestDTO();
            dto.setId(5L);
            dto.setName("Return-DTO");
            return dto;
        }
        
        @ProtokolliereAktivitaet(
            aktivitaetsTyp = AktivitaetsTyp.LOGIN,
            beschreibung = "Model-Attribut verwendet: {0}",
            mitParametern = true
        )
        public String testMethodeMitModelAttributArg(String param, Model model) {
            return "Ergebnis";
        }
    }

}
