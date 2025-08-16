package de.fuh.kn.webapp;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.*;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Archunit Test zum prüfen von Abhängigkeiten zwischen Klassen
 */
public class ArchitectureTest {

    private static final String BASE_PACKAGE = "de.fuh.kn.webapp";
    private static final String ENTITY_PACKAGE = "de.fuh.kn.webapp.persistence.entity";
    private static final String REPOSITORY_PACKAGE = "de.fuh.kn.webapp.persistence.repository";

    @Test
    void onlyCertainClassesMayAccessEntity() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);


        // Keine anderen Klassen außer Services und Repositories dürfen von den Entity-Klassen abhängen
        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(BASE_PACKAGE + "..")
                .and().haveSimpleNameNotEndingWith("Service")
                .and().haveSimpleNameNotEndingWith("Repository")
                .and().haveSimpleNameNotEndingWith("Test")
                .and().haveSimpleNameNotEndingWith("Mapper")
                .and().haveSimpleNameNotEndingWith("MapperImpl")
                .and().resideOutsideOfPackage("..test..")
                .and().resideOutsideOfPackage("..entity..")
                .and().haveSimpleNameNotContaining("InitialDataConfig")
                .and().haveSimpleNameNotContaining("KursMaterialEntityListener")
                .should()
                .dependOnClassesThat(new DescribedPredicate<JavaClass>("are non-enum classes in entity package") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        return javaClass.getPackageName().equals(ENTITY_PACKAGE) && !javaClass.isEnum();
                    }
                })
                ;


        rule.check(importedClasses);
    }


    @Test
    void serviceMethodsShouldNotUseEntityInParametersOrReturnTypes() {
        var importedClasses = new ClassFileImporter().importPackages(BASE_PACKAGE);

        ArchCondition<JavaClass> methodsShouldNotExposeEntities = new ArchCondition<>("have no method parameter or return type from entity package, including Optional, except enums") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                for (JavaMethod method : javaClass.getMethods()) {
                    if (method.getModifiers().contains(JavaModifier.PUBLIC) || method.getModifiers().contains(JavaModifier.PROTECTED)) {

                        // Check parameters
                        for (JavaParameter param : method.getParameters()) {
                            String paramTypePackage = param.getRawType().getPackageName();
                            if (paramTypePackage.equals(ENTITY_PACKAGE) && !param.getRawType().isEnum()) {
                                String message = String.format("Method %s has parameter of entity type %s", method.getFullName(), param.getRawType().getFullName());
                                events.add(SimpleConditionEvent.violated(param, message));
                            }
                        }

                        // Check return type
                        JavaClass rawReturnType = method.getRawReturnType();
                        String returnTypePackage = rawReturnType.getPackageName();
                        if (returnTypePackage.contains(ENTITY_PACKAGE) && !rawReturnType.isEnum()) {
                            String message = String.format("Method %s has return type of entity type %s", method.getFullName(), rawReturnType.getFullName());
                            events.add(SimpleConditionEvent.violated(method, message));
                        }

                        // Check if raw return type is Optional (cannot inspect inside)
                        if (rawReturnType.getFullName().equals("java.util.Optional")) {
                            for (JavaClass allInvolvedRawType : method.getReturnType().getAllInvolvedRawTypes()) {
                                if(allInvolvedRawType.getPackageName().contains(ENTITY_PACKAGE) && !allInvolvedRawType.isEnum()) {
                                    String message = String.format("Method %s has return type of entity type Optional<%s>", method.getFullName(), allInvolvedRawType.getFullName());
                                    events.add(SimpleConditionEvent.violated(method, message));
                                }
                            }

                        }
                    }
                }
            }
        };

        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Service")
                .should(methodsShouldNotExposeEntities);

        rule.check(importedClasses);
    }




    @Test
    void onlyServicesShouldAccessRepositories() {
        var importedClasses = new ClassFileImporter().importPackages(BASE_PACKAGE);

        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(BASE_PACKAGE + "..")
                .and().haveSimpleNameNotEndingWith("Service")
                .and().haveSimpleNameNotEndingWith("Test")
                .and().resideOutsideOfPackage("..test..")
                .and().haveSimpleNameNotContaining("InitialDataConfig")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(REPOSITORY_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    void servicesShouldBeAnnotatedWithService() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages(BASE_PACKAGE);

        ArchRule serviceClassesShouldBeAnnotated = classes()
                .that().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith(Service.class);

        serviceClassesShouldBeAnnotated.check(importedClasses);
    }

    @Test
    void repositoriesShouldBeAnnotatedWithRepository() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages(BASE_PACKAGE);

        ArchRule repositoryClassesShouldBeAnnotated = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .should().beAnnotatedWith(Repository.class);

        repositoryClassesShouldBeAnnotated.check(importedClasses);
    }

    @Test
    void controllersShouldBeAnnotatedWithControllerOrRestController() {
        JavaClasses importedClasses = new ClassFileImporter().importPackages(BASE_PACKAGE);

        ArchRule controllerClassesShouldBeAnnotated = classes()
                .that().haveSimpleNameEndingWith("Controller")
                .and().haveSimpleNameNotContaining("Test")
                .should().beAnnotatedWith(Controller.class)
                .orShould().beAnnotatedWith(RestController.class);

        controllerClassesShouldBeAnnotated.check(importedClasses);
    }


    @Test
    void onlyMarkdownPackageMayUseFlexmark() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        String allowedPackage = "de.fuh.kn.webapp.common.markdown..";
        String flexmarkPackage = "com.vladsch.flexmark..";

        ArchRule rule = noClasses()
                .that()
                .resideOutsideOfPackage(allowedPackage)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(flexmarkPackage);

        rule.check(importedClasses);
    }

    @Test
    void onlyMarkdownPackageMayAccessFlexmarkExceptAllowedClasses() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        String markdownPackage = "de.fuh.kn.webapp.aufgabenverwaltung.markdown..";
        String flexmarkPackage = "de.fuh.kn.webapp.aufgabenverwaltung.markdown.flexmark";

        ArchRule rule = noClasses()
                .that()
                .resideOutsideOfPackage(markdownPackage)
                .should()
                .dependOnClassesThat(new DescribedPredicate<>("are flexmark classes except MarkdownRenderResult and InputFieldDto") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        String pkg = javaClass.getPackageName();
                        String simpleName = javaClass.getSimpleName();
                        return pkg.equals(flexmarkPackage)
                                && !(simpleName.equals("MarkdownRenderResult") || simpleName.equals("InputFieldDto"));
                    }
                });

        rule.check(importedClasses);
    }

    @Test
    void repositoriesShouldOnlyBeInRepositoryPackage() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Repository")
                .should().resideInAPackage(REPOSITORY_PACKAGE);

        rule.check(importedClasses);
    }

    @Test
    void onlyLlmPackageMayUseSpringAi() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        String springAiPackage = "org.springframework.ai..";

        ArchRule rule = noClasses()
                .that()
                .resideOutsideOfPackage("de.fuh.kn.webapp.llm..")
                .and()
                .resideOutsideOfPackage("de.fuh.kn.webapp.uebung.bewertung..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage(springAiPackage);

        rule.check(importedClasses);
    }

    @Test
    void persistenceLayerShouldNotDependOnOtherComponents() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        // Persistence layer should only depend on common and not call other business components
        // Exception: KursMaterialEntityListener is allowed to access LLM for event handling
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("de.fuh.kn.webapp.persistence..")
                .and()
                .haveSimpleNameNotContaining("KursMaterialEntityListener")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                    "de.fuh.kn.webapp.aufgabenverwaltung..",
                    "de.fuh.kn.webapp.chat..",
                    "de.fuh.kn.webapp.uebung..",
                    "de.fuh.kn.webapp.kursverwaltung..",
                    "de.fuh.kn.webapp.nutzerverwaltung..",
                    "de.fuh.kn.webapp.llm..",
                    "de.fuh.kn.webapp.student..",
                    "de.fuh.kn.webapp.kursbetreuung.."
                );

        rule.check(importedClasses);
    }

    @Test
    void coreComponentsShouldOnlyDependOnPersistenceAndEachOther() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        // Core components (aufgabenverwaltung, nutzerverwaltung, kursverwaltung) should not depend on top-layer components
        // Exception: Controllers may depend on any services, and Services may depend on other Services
        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(
                    "de.fuh.kn.webapp.aufgabenverwaltung..",
                    "de.fuh.kn.webapp.nutzerverwaltung..",
                    "de.fuh.kn.webapp.kursverwaltung.."
                )
                .and()
                .haveSimpleNameNotEndingWith("Controller")
                .and()
                .haveSimpleNameNotEndingWith("HtmxController")
                .should()
                .dependOnClassesThat(new DescribedPredicate<>("are not allowed dependencies") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        String pkg = javaClass.getPackageName();
                        String simpleName = javaClass.getSimpleName();
                        
                        // Check if the class is in one of the restricted packages
                        boolean isInRestrictedPackage = 
                            pkg.startsWith("de.fuh.kn.webapp.chat") ||
                            pkg.startsWith("de.fuh.kn.webapp.uebung") ||
                            pkg.startsWith("de.fuh.kn.webapp.student") ||
                            pkg.startsWith("de.fuh.kn.webapp.kursbetreuung");
                        
                        if (!isInRestrictedPackage) {
                            return false;
                        }
                        
                        // Allow Services and DTOs from restricted packages
                        boolean isService = simpleName.endsWith("Service");
                        boolean isDto = simpleName.endsWith("DTO") || 
                                      simpleName.endsWith("Dto") ||
                                      pkg.contains(".dto") ||
                                      pkg.contains(".dto.");
                        
                        // Return true if it's NOT a Service or DTO (i.e., we should flag it)
                        return !(isService || isDto);
                    }
                });

        rule.check(importedClasses);
    }

    @Test
    void topLayerComponentsShouldNotDependOnEachOther() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        // Top layer components (chat, uebung) should not depend on each other
        ArchRule chatRule = noClasses()
                .that()
                .resideInAPackage("de.fuh.kn.webapp.chat..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("de.fuh.kn.webapp.uebung..");

        ArchRule uebungRule = noClasses()
                .that()
                .resideInAPackage("de.fuh.kn.webapp.uebung..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("de.fuh.kn.webapp.chat..");

        chatRule.check(importedClasses);
        uebungRule.check(importedClasses);
    }

    @Test
    void componentsShouldNotDependOnOldRoleBasedControllers() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        // New components should not depend on old role-based controller structure
        ArchRule rule = noClasses()
                .that()
                .resideInAnyPackage(
                    "de.fuh.kn.webapp.aufgabenverwaltung..",
                    "de.fuh.kn.webapp.chat..",
                    "de.fuh.kn.webapp.uebung..",
                    "de.fuh.kn.webapp.kursverwaltung..",
                    "de.fuh.kn.webapp.nutzerverwaltung.."
                )
                .should()
                .dependOnClassesThat(new DescribedPredicate<>("are old role-based controllers") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        String pkg = javaClass.getPackageName();
                        String simpleName = javaClass.getSimpleName();
                        
                        return (pkg.startsWith("de.fuh.kn.webapp.student") || 
                                pkg.startsWith("de.fuh.kn.webapp.kursbetreuung")) &&
                               (simpleName.endsWith("Controller") || simpleName.endsWith("HtmxController"));
                    }
                });

        rule.check(importedClasses);
    }

    @Test
    void llmPackageShouldOnlyBeCalledNotCall() {
        var importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        // LLM package should be a service layer that doesn't call back into business components
        // Exception: LLM package may use DTOs from other packages and Services from core packages
        ArchRule rule = noClasses()
                .that()
                .resideInAPackage("de.fuh.kn.webapp.llm..")
                .should()
                .dependOnClassesThat(new DescribedPredicate<>("are not allowed dependencies") {
                    @Override
                    public boolean test(JavaClass javaClass) {
                        String pkg = javaClass.getPackageName();
                        String simpleName = javaClass.getSimpleName();
                        
                        // Define core packages where services are allowed
                        boolean isInCorePackage = 
                            pkg.startsWith("de.fuh.kn.webapp.aufgabenverwaltung") ||
                            pkg.startsWith("de.fuh.kn.webapp.kursverwaltung") ||
                            pkg.startsWith("de.fuh.kn.webapp.nutzerverwaltung");
                        
                        // Define restricted packages (role-based and other components)
                        boolean isInRestrictedPackage = 
                            pkg.startsWith("de.fuh.kn.webapp.chat") ||
                            pkg.startsWith("de.fuh.kn.webapp.uebung") ||
                            pkg.startsWith("de.fuh.kn.webapp.student") ||
                            pkg.startsWith("de.fuh.kn.webapp.kursbetreuung");
                        
                        // Check if it's a DTO (allowed everywhere)
                        boolean isDto = simpleName.endsWith("DTO") || 
                                      simpleName.endsWith("Dto") ||
                                      pkg.contains(".dto") ||
                                      pkg.contains(".dto.");
                        
                        // Check if it's a Service
                        boolean isService = simpleName.endsWith("Service");
                        
                        // Allow DTOs from any package
                        if (isDto) {
                            return false;
                        }
                        
                        // Allow Services from core packages
                        if (isInCorePackage && isService) {
                            return false;
                        }
                        
                        // Flag anything else from restricted or core packages
                        return isInCorePackage || isInRestrictedPackage;
                    }
                });

        rule.check(importedClasses);
    }

}
