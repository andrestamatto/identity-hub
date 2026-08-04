package br.dev.andrestamatto.identityhub.architecture;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class FoundationArchitectureTest {

    private static final String BASE_PACKAGE = "br.dev.andrestamatto.identityhub";

    private static final JavaClasses PRODUCTION_CLASSES = importProductionClasses();

    private static JavaClasses importProductionClasses() {
        ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    @Test
    void codeContainsOnlyApprovedCapabilities() {
        classes()
                .should().resideInAnyPackage(
                        BASE_PACKAGE + ".bootstrap..",
                        BASE_PACKAGE + ".audit..",
                        BASE_PACKAGE + ".access.domain..",
                        BASE_PACKAGE + ".access.application..",
                        BASE_PACKAGE + ".access.adapter.in.http..",
                        BASE_PACKAGE + ".access.adapter.out.jdbc..",
                        BASE_PACKAGE + ".access.adapter.out.keycloak..",
                        BASE_PACKAGE + ".access.adapter.out.clientapplication..",
                        BASE_PACKAGE + ".clientapplication.domain..",
                        BASE_PACKAGE + ".clientapplication.application..",
                        BASE_PACKAGE + ".clientapplication.adapter.in.http..",
                        BASE_PACKAGE + ".clientapplication.adapter.out.jdbc..",
                        BASE_PACKAGE + ".clientapplication.adapter.out.keycloak..",
                        BASE_PACKAGE + ".communication.domain..",
                        BASE_PACKAGE + ".communication.application..",
                        BASE_PACKAGE + ".communication.adapter.in.http..",
                        BASE_PACKAGE + ".communication.adapter.out.jdbc..",
                        BASE_PACKAGE + ".communication.adapter.out.smtp..",
                        BASE_PACKAGE + ".communication.adapter.out.clientapplication..",
                        BASE_PACKAGE + ".identity.domain..",
                        BASE_PACKAGE + ".identity.application..",
                        BASE_PACKAGE + ".identity.adapter.in.http..",
                        BASE_PACKAGE + ".identity.adapter.out.clientapplication..",
                        BASE_PACKAGE + ".identity.adapter.out.communication..",
                        BASE_PACKAGE + ".identity.adapter.out.crypto..",
                        BASE_PACKAGE + ".identity.adapter.out.jdbc..",
                        BASE_PACKAGE + ".identity.adapter.out.keycloak..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void auditApplicationDoesNotDependOnFrameworksOrAdapters() {
        noClasses()
                .that().resideInAPackage("..audit.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "..adapter..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void domainCodeDoesNotDependOnOuterLayersOrFrameworks() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "org.keycloak..",
                        "..application..",
                        "..adapter..",
                        "..bootstrap..")
                .allowEmptyShould(true)
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void adaptersDoNotDependOnBootstrap() {
        noClasses()
                .that().resideInAPackage("..adapter..")
                .should().dependOnClassesThat()
                .resideInAPackage("..bootstrap..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void applicationCodeDoesNotDependOnFrameworksOrAdapters() {
        noClasses()
                .that().resideInAnyPackage(
                        "..clientapplication.application..",
                        "..communication.application..",
                        "..access.application..",
                        "..identity.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta..",
                        "org.keycloak..",
                        "..adapter..",
                        "..bootstrap..")
                .check(PRODUCTION_CLASSES);
    }

    @Test
    void keycloakTypesCannotEscapeTheirDedicatedAdapter() {
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

    @Test
    void directFrameworkDependencyRemainsVisibleWithoutClasspathResolution() {
        JavaClasses fixtureClasses = new ClassFileImporter().importClasses(FrameworkDependentFixture.class);

        assertThrows(AssertionError.class, () -> noClasses()
                .that().resideInAPackage("..architecture..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(fixtureClasses));
    }

    private static final class FrameworkDependentFixture {

        private ApplicationContext applicationContext;
    }
}
