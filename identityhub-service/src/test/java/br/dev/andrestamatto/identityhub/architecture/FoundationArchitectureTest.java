package br.dev.andrestamatto.identityhub.architecture;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class FoundationArchitectureTest {

    private static final String BASE_PACKAGE = "br.dev.andrestamatto.identityhub";

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE_PACKAGE);

    @Test
    void foundationContainsOnlyBootstrapCode() {
        classes()
                .should().resideInAPackage(BASE_PACKAGE + ".bootstrap..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void futureDomainCodeCannotDependOnFrameworks() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "org.keycloak..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keycloakTypesCannotEscapeTheirFutureAdapter() {
        noClasses()
                .that().resideOutsideOfPackage("..adapter.out.keycloak..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.keycloak..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void topLevelPackagesCannotFormCycles() {
        slices()
                .matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .check(PRODUCTION_CLASSES);
    }
}
