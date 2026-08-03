package br.dev.andrestamatto.identityhub.integration.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class IntegrationModeArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("br.dev.andrestamatto.identityhub.integration");

    @Test
    void starterDoesNotDependOnKeycloakTypes() {
        noClasses()
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.keycloak..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }
}
