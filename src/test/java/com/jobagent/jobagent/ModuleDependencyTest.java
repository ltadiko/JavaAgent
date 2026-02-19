package com.jobagent.jobagent;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit tests enforcing module dependency rules.
 * (Gap #13 from design audit)
 *
 * Allowed dependency: any module → common
 * Forbidden: cross-module dependencies (e.g., cv → motivation)
 */
@AnalyzeClasses(
        packages = "com.jobagent.jobagent",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleDependencyTest {

    private static final String AUTH = "..auth..";
    private static final String CV = "..cv..";
    private static final String JOBSEARCH = "..jobsearch..";
    private static final String MOTIVATION = "..motivation..";
    private static final String APPLY = "..apply..";
    private static final String APPLICATION = "..application..";

    @ArchTest
    static final ArchRule cv_should_not_depend_on_motivation =
            noClasses().that().resideInAPackage(CV)
                    .should().dependOnClassesThat().resideInAPackage(MOTIVATION)
                    .because("CV module must not depend on Motivation module");

    @ArchTest
    static final ArchRule cv_should_not_depend_on_apply =
            noClasses().that().resideInAPackage(CV)
                    .should().dependOnClassesThat().resideInAPackage(APPLY)
                    .because("CV module must not depend on Apply module");

    @ArchTest
    static final ArchRule auth_should_not_depend_on_business_modules =
            noClasses().that().resideInAPackage(AUTH)
                    .should().dependOnClassesThat().resideInAnyPackage(CV, JOBSEARCH, MOTIVATION, APPLY, APPLICATION)
                    .because("Auth module must not depend on business modules");

    @ArchTest
    static final ArchRule motivation_should_not_depend_on_apply =
            noClasses().that().resideInAPackage(MOTIVATION)
                    .should().dependOnClassesThat().resideInAPackage(APPLY)
                    .because("Motivation module must not depend on Apply module");

    @ArchTest
    static final ArchRule application_view_should_not_depend_on_apply =
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAPackage(APPLY)
                    .because("Application view module must not depend on Apply module");
}
