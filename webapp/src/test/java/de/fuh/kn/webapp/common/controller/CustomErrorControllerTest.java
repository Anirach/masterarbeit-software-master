package de.fuh.kn.webapp.common.controller;

import de.fuh.kn.webapp.nutzerverwaltung.auth.NutzerDetailsService;
import de.fuh.kn.webapp.security.SecurityConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit-Tests für den CustomErrorController.
 * Diese Tests decken verschiedene Fehlersituationen ab und prüfen,
 * ob der Controller die entsprechenden Fehlermeldungen korrekt zurückgibt.
 */
@WebMvcTest(CustomErrorController.class)
@Import(SecurityConfig.class)
class CustomErrorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomErrorController errorController;
    
    @MockitoBean
    private NutzerDetailsService nutzerDetailsService;

    @Test
    @DisplayName("handleError sollte die error.html Seite zurückgeben")
    void handleError_ShouldReturnErrorView() throws Exception {
        // Da /error in SecurityConfig.java als .permitAll() konfiguriert ist, kein @WithMockUser nötig
        // Wir testen hier direkten Methodenaufruf statt MockMvc, da SecurityConfig die Anfrage blockt
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Model mockModel = mock(Model.class);
        
        String viewName = errorController.handleError(mockRequest, mockModel);
        
        assertThat(viewName).isEqualTo("error");
        verify(mockModel).addAttribute(eq("error"), eq("Unbekannter Fehler"));
        verify(mockModel).addAttribute(eq("message"), eq("Ein unerwarteter Fehler ist aufgetreten."));
    }

    @Test
    @DisplayName("handleError sollte für 404 Not Found die korrekten Attribute setzen")
    void handleError_WithStatusNotFound_ShouldSetCorrectAttributes() throws Exception {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Model mockModel = mock(Model.class);
        
        when(mockRequest.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(HttpStatus.NOT_FOUND.value());
        
        String viewName = errorController.handleError(mockRequest, mockModel);
        
        assertThat(viewName).isEqualTo("error");
        verify(mockModel).addAttribute("status", HttpStatus.NOT_FOUND.value());
        verify(mockModel).addAttribute("error", "Seite nicht gefunden");
        verify(mockModel).addAttribute("message", "Die angeforderte Seite existiert nicht.");
    }

    @Test
    @DisplayName("handleError sollte für 403 Forbidden die korrekten Attribute setzen")
    void handleError_WithStatusForbidden_ShouldSetCorrectAttributes() throws Exception {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Model mockModel = mock(Model.class);
        
        when(mockRequest.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(HttpStatus.FORBIDDEN.value());
        
        String viewName = errorController.handleError(mockRequest, mockModel);
        
        assertThat(viewName).isEqualTo("error");
        verify(mockModel).addAttribute("status", HttpStatus.FORBIDDEN.value());
        verify(mockModel).addAttribute("error", "Zugriff verweigert");
        verify(mockModel).addAttribute("message", "Sie haben keine Berechtigung, auf diese Ressource zuzugreifen.");
    }

    @Test
    @DisplayName("handleError sollte für 500 Internal Server Error die korrekten Attribute setzen")
    void handleError_WithStatusInternalServerError_ShouldSetCorrectAttributes() throws Exception {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Model mockModel = mock(Model.class);
        
        when(mockRequest.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        String viewName = errorController.handleError(mockRequest, mockModel);
        
        assertThat(viewName).isEqualTo("error");
        verify(mockModel).addAttribute("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        verify(mockModel).addAttribute("error", "Interner Serverfehler");
        verify(mockModel).addAttribute("message", "Bei der Verarbeitung Ihrer Anfrage ist ein Fehler aufgetreten.");
    }

    @Test
    @DisplayName("handleError sollte für andere Status-Codes generische Attribute setzen")
    void handleError_WithOtherStatus_ShouldSetGenericAttributes() throws Exception {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        Model mockModel = mock(Model.class);
        
        int customStatus = 418; // I'm a teapot
        String customErrorMessage = "Ich bin eine Teekanne";
        
        when(mockRequest.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)).thenReturn(customStatus);
        when(mockRequest.getAttribute(RequestDispatcher.ERROR_MESSAGE)).thenReturn(customErrorMessage);
        
        String viewName = errorController.handleError(mockRequest, mockModel);
        
        assertThat(viewName).isEqualTo("error");
        verify(mockModel).addAttribute("status", customStatus);
        verify(mockModel).addAttribute("error", "Fehler " + customStatus);
        verify(mockModel).addAttribute("message", customErrorMessage);
    }
}