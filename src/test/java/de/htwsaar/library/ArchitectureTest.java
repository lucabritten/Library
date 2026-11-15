package de.htwsaar.library;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "de.htwsaar.library",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTest {

    @ArchTest
    final static ArchRule servicesDoNotKnowHttp = noClasses()
            .that().haveSimpleNameEndingWith("Service")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.http..");

    @ArchTest
    final static ArchRule controllersDoNotUseRepos = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().areAssignableTo(Repository.class);

    @ArchTest
    final static ArchRule noFieldInjection = noFields()
            .should().beAnnotatedWith(Autowired.class)
            .because("Dependencies are injected via constructor.");

    @ArchTest
    static final ArchRule commonDoesNotDependOnFeatures = noClasses()
            .that().resideInAPackage("de.htwsaar.library.common..")
            .should().dependOnClassesThat(
                    resideInAPackage("de.htwsaar.library..")
                            .and(resideOutsideOfPackage("de.htwsaar.library.common..")))
            .because("common ist die Basis aller Features und darf keines davon kennen");


    @ArchTest
    static final ArchRule featuresAreFreeOfCycles = slices()
            .matching("de.htwsaar.library.(*)..")
            .should().beFreeOfCycles()
            .because("Features dürfen sich nur in eine Richtung kennen");

}
