package de.fuh.kn.webapp.common.aktivitaeten;

import de.fuh.kn.webapp.common.dto.BaseDTO;
import de.fuh.kn.webapp.nutzerverwaltung.auth.KursbetreuerUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.auth.StudentUserDetails;
import de.fuh.kn.webapp.nutzerverwaltung.dto.AktivitaetDTO;
import de.fuh.kn.webapp.nutzerverwaltung.dto.NutzerDTO;
import de.fuh.kn.webapp.nutzerverwaltung.service.NutzerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * AspectJ-Aspekt zur automatischen Protokollierung von Aktivitäten.
 * Protokolliert Methoden, die mit der @ProtokolliereAktivitaet-Annotation versehen sind.
 * 
 * Berücksichtigt sowohl Entities als auch DTOs für die Referenzerkennung.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AktivitaetsProtokollierungAspect {

    private final AktivitaetReferenzHelper aktivitaetReferenzHelper;
    private final NutzerService nutzerService;
    private final AktivitaetsProtokollierungService aktivitaetsProtokollierungService;

    /**
     * Pointcut, der alle Methoden mit der @ProtokolliereAktivitaet-Annotation auswählt.
     */
    @Pointcut("@annotation(de.fuh.kn.webapp.common.aktivitaeten.ProtokolliereAktivitaet)")
    public void aktivitaetsProtokollierung() {
    }

    /**
     * Around-Advice, das vor und nach der Methodenausführung die Aktivität protokolliert.
     *
     * @param joinPoint Der Joinpoint der Methode
     * @return Das Ergebnis der ursprünglichen Methodenausführung
     * @throws Throwable Wenn ein Fehler bei der Methodenausführung auftritt
     */
    @Around("aktivitaetsProtokollierung()")
    public Object protokolliereAktivitaet(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        ProtokolliereAktivitaet annotation = method.getAnnotation(ProtokolliereAktivitaet.class);

        // Aktuellen Nutzer aus dem SecurityContext erfassen
        Long nutzerId = getNutzerIdAusSecurityContext();
        NutzerDTO nutzer = null;
        
        if (nutzerId != null) {
            Optional<NutzerDTO> nutzerOpt = nutzerService.getNutzerById(nutzerId);
            if (nutzerOpt.isPresent()) {
                nutzer = nutzerOpt.get();
            } else {
                log.warn("Nutzer mit ID {} nicht in der Datenbank gefunden", nutzerId);
            }
        } else {
            log.warn("Keine Nutzerinformation im SecurityContext für die Aktivitätsprotokollierung vorhanden");
        }

        // Parameter der Methode und referenzierte Objekte extrahieren
        Map<String, Object> detailsMap = erstelleDetailsMap(joinPoint, annotation, methodSignature);
        
        // Ermittle referenzTyp und referenzId aus den Methodenparametern, falls vorhanden
        Object[] args = joinPoint.getArgs();
        String referenzTyp = null;
        Long referenzId = null;
        
        // Suche nach DTOs in den Methodenparametern
        if (args != null) {
            for (Object arg : args) {
                // Prüfe auf DTO
                if (arg instanceof BaseDTO dto) {
                    if (dto.getId() != null && dto.getEntityTypeName() != null) {
                        referenzTyp = dto.getEntityTypeName();
                        referenzId = dto.getId();
                        break;
                    }
                }
            }
        }



        boolean erfolg = true;
        Object result = null;
        try {
            // Originalmethode ausführen
            result = joinPoint.proceed();

            // Wenn das Ergebnis ein DTO ist, nutze diese als Referenz
            if (result instanceof BaseDTO dto) {
                if (dto.getId() != null && dto.getEntityTypeName() != null) {
                    referenzTyp = dto.getEntityTypeName();
                    referenzId = dto.getId();
                }
            }

            //Falls bislang noch nichts gefunden wurde: im Model nach DTOs suchen
            if(referenzTyp == null && referenzId == null){
                if (args != null) {
                    for (Object arg : args) {
                        if(arg instanceof Model model){

                            for (Object modelAttribute : model.asMap().values()) {
                                if(modelAttribute instanceof BaseDTO dto){
                                    referenzTyp = dto.getEntityTypeName();
                                    referenzId = dto.getId();
                                }
                            }
                        }
                    }
                }
            }

            //Argumente aus Model nehmen, falls sie "aktivitaetsprotokollierung-arg-xx" heißen
            for (Object arg : args.clone()) {
                if(arg instanceof Model model){

                    String prefix = "aktivitaetsprotokollierung-arg-";

                    Map<String, Object> modelAttributes = model.asMap();
                    for (String modelAttributeKey : modelAttributes.keySet()) {
                        if(modelAttributeKey.startsWith(prefix)){
                            int index = Integer.parseInt(modelAttributeKey.substring(prefix.length()));
                            args[index] = modelAttributes.get(modelAttributeKey);
                        }
                    }
                }
            }

            return result;
        } catch (Throwable t) {
            erfolg = false;
            detailsMap.put("fehler", t.getMessage());
            throw t;
        } finally {

            // Beschreibung mit Platzhaltern ersetzen
            String beschreibung = annotation.beschreibung();
            if (beschreibung != null && !beschreibung.isEmpty() && args != null && args.length > 0) {
                beschreibung = formatiereNachricht(beschreibung, args);
            }

            // Aktivität protokollieren
            try {
                AktivitaetDTO aktivitaet = aktivitaetsProtokollierungService.protokolliereAktivitaet(
                        nutzer,
                        annotation.aktivitaetsTyp(),
                        beschreibung,
                        detailsMap,
                        erfolg,
                        referenzTyp,
                        referenzId
                );
                log.debug("Aktivität protokolliert: {}", aktivitaet.getId());
            } catch (Exception e) {
                log.error("Fehler bei der Aktivitätsprotokollierung: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Extrahiert die ID des aktuellen Nutzers aus dem SecurityContext.
     *
     * @return Die ID des aktuellen Nutzers oder null, wenn kein Nutzer im SecurityContext vorhanden ist
     */
    private Long getNutzerIdAusSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.getPrincipal() != null) {
            // Prüfe verschiedene Nutzer-Implementierungen
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof StudentUserDetails) {
                return ((StudentUserDetails) principal).getId();
            } else if (principal instanceof KursbetreuerUserDetails) {
                return ((KursbetreuerUserDetails) principal).getId();
            }
        }
        
        return null;
    }

    /**
     * Erstellt eine Map mit Details zur Aktivität basierend auf den Methodenparametern.
     *
     * @param joinPoint Der Joinpoint der Methode
     * @param annotation Die ProtokolliereAktivitaet-Annotation
     * @param methodSignature Die Methodensignatur
     * @return Eine Map mit Details zur Aktivität
     */
    private Map<String, Object> erstelleDetailsMap(ProceedingJoinPoint joinPoint,
                                                ProtokolliereAktivitaet annotation,
                                                MethodSignature methodSignature) {
        Map<String, Object> detailsMap = new HashMap<>();
        
        // Basis-Informationen zur Methode
        detailsMap.put("methode", methodSignature.getMethod().getName());
        detailsMap.put("klasse", methodSignature.getDeclaringTypeName());
        
        // Parameter in Details aufnehmen, wenn gewünscht
        if (annotation.mitParametern()) {
            Object[] args = joinPoint.getArgs();
            String[] parameterNamen = methodSignature.getParameterNames();
            
            if (args != null && args.length > 0 && parameterNamen != null && parameterNamen.length == args.length) {
                Map<String, Object> parameterMap = new HashMap<>();
                
                for (int i = 0; i < args.length; i++) {
                    Object arg = args[i];
                    String paramName = parameterNamen[i];
                    
                    // Einfache Typen direkt speichern, bei DTOs den Anzeigenamen verwenden
                    if (arg == null) {
                        parameterMap.put(paramName, null);
                    } else if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                        parameterMap.put(paramName, arg);
                    } else if (arg instanceof BaseDTO) {
                        parameterMap.put(paramName, aktivitaetReferenzHelper.toString(arg));
                    } else {
                        parameterMap.put(paramName, arg.getClass().getSimpleName());
                    }
                }
                
                detailsMap.put("parameter", parameterMap);
            }
        }
        
        return detailsMap;
    }

    /**
     * Formatiert eine Nachricht mit Platzhaltern durch Einfügen der angegebenen Argumente.
     *
     * @param nachricht Die Nachricht mit Platzhaltern
     * @param args Die Argumente, die in die Platzhalter eingesetzt werden sollen
     * @return Die formatierte Nachricht
     */
    private String formatiereNachricht(String nachricht, Object[] args) {
        try {
            for(int i = 0; i<args.length; i++) {
                Object arg = args[i];
                //DTOs durch Anzeige-Strings ersetzen
                if(arg instanceof BaseDTO) {
                    args[i] = aktivitaetReferenzHelper.toString(arg);
                }
                //Dateiuploads durch Anzeigenamen ersetzen
                if(arg instanceof MultipartFile multipartFile){
                    args[i] = multipartFile.getOriginalFilename();
                }
            }
            return MessageFormat.format(nachricht, args);
        } catch (Exception e) {
            log.warn("Fehler beim Formatieren der Aktivitätsbeschreibung: {}", e.getMessage(), e);
            return nachricht;
        }
    }
}
