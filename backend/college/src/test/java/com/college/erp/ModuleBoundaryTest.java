package com.college.erp;

import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * 07-coding-standards.md §4.4/§13.3/§15/§16.1: "no cross-module repository access" — a module's
 * service/controller layer must reach another module's data through that module's *service*, not
 * its repository. Mechanically enforces the pattern this codebase has followed by hand/convention
 * across every module built so far.
 *
 * Two deliberate, documented exceptions (see CLAUDE.md's per-module deviations notes):
 * - module.admin.service.UserManagementService → module.auth's UserRepository/
 *   RoleAssignmentRepository — 08-system-architecture.md's own file tree places
 *   UserManagementService under module.admin operating on auth's entities directly; no fuller
 *   UserService exists in module.auth to route through.
 * - Everything under com.college.erp.scheduler (kernel, not a module.* package) is naturally
 *   exempt — a scheduler.jobs handler calling INTO a module's service is the sanctioned
 *   direction; scheduler.jobs itself doesn't own a repository for another module to reach into.
 */
@AnalyzeClasses(packages = "com.college.erp", importOptions = ImportOption.DoNotIncludeTests.class)
class ModuleBoundaryTest {

    private static final String MODULE_MARKER = "com.college.erp.module.";

    /** {ownModule -> otherModule} pairs explicitly allowed to violate the rule below, each with
     *  a reason recorded in CLAUDE.md's deviations notes rather than here. */
    private static final Set<String> ALLOWED_EXCEPTIONS = Set.of("admin->auth");

    @ArchTest
    static final ArchRule no_cross_module_repository_access = classes()
            .that(new DescribedPredicate<JavaClass>("reside in a module's service or controller package") {
                @Override
                public boolean test(JavaClass input) {
                    return moduleNameOf(input) != null && isServiceOrController(input);
                }
            })
            .should(new ArchCondition<JavaClass>("depend on another module's *Repository class") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    String ownModule = moduleNameOf(item);
                    item.getDirectDependenciesFromSelf().forEach(dependency -> {
                        JavaClass target = dependency.getTargetClass();
                        String targetModule = moduleNameOf(target);
                        if (targetModule == null || targetModule.equals(ownModule)) {
                            return;
                        }
                        if (!target.getSimpleName().endsWith("Repository")) {
                            return;
                        }
                        if (ALLOWED_EXCEPTIONS.contains(ownModule + "->" + targetModule)) {
                            return;
                        }
                        events.add(SimpleConditionEvent.violated(item, item.getFullName()
                                + " depends on " + target.getFullName()
                                + " — cross-module repository access is forbidden "
                                + "(07-coding-standards.md §4.4); go through " + targetModule
                                + "'s service instead"));
                    });
                }
            });

    /** 07-coding-standards.md §15's "No business logic in controllers" checklist item, narrowed
     *  to what's mechanically checkable: a controller must never depend on a *Repository directly
     *  — not even its own module's. Routing through the service layer is the whole point of
     *  having one. */
    @ArchTest
    static final ArchRule controllers_do_not_access_repositories_directly = classes()
            .that().resideInAPackage("..controller..")
            .should(new ArchCondition<JavaClass>("depend on any *Repository class") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    item.getDirectDependenciesFromSelf().forEach(dependency -> {
                        JavaClass target = dependency.getTargetClass();
                        if (target.getSimpleName().endsWith("Repository")) {
                            events.add(SimpleConditionEvent.violated(item, item.getFullName()
                                    + " depends on " + target.getFullName()
                                    + " — controllers must go through a service, never a repository"));
                        }
                    });
                }
            });

    /** 07-coding-standards.md's constructor-injection convention (@RequiredArgsConstructor over
     *  field @Autowired) — checked directly on fields rather than via a dependency search, since
     *  field-level @Autowired doesn't show up as a class dependency the way method calls do. */
    @ArchTest
    static final ArchRule no_field_injection = classes()
            .that().resideInAPackage("com.college.erp..")
            .should(new ArchCondition<JavaClass>("not use field-level @Autowired") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    item.getFields().forEach(field -> {
                        boolean autowired = field.getAnnotations().stream()
                                .anyMatch(a -> a.getRawType().getSimpleName().equals("Autowired"));
                        if (autowired) {
                            events.add(SimpleConditionEvent.violated(item,
                                    field.getFullName() + " uses field-level @Autowired — "
                                            + "use constructor injection (@RequiredArgsConstructor) instead"));
                        }
                    });
                }
            });

    /** Extracts the module name from a fully-qualified class name, e.g.
     *  "com.college.erp.module.finance.service.FeeCollectionService" -> "finance". Returns null
     *  for anything outside com.college.erp.module.* (kernel packages, scheduler, notification,
     *  storage, security, ...) — those aren't "a module" for this rule's purposes. */
    private static String moduleNameOf(JavaClass clazz) {
        String pkg = clazz.getPackageName();
        if (!pkg.startsWith(MODULE_MARKER)) {
            return null;
        }
        String rest = pkg.substring(MODULE_MARKER.length());
        int dot = rest.indexOf('.');
        return dot >= 0 ? rest.substring(0, dot) : rest;
    }

    private static boolean isServiceOrController(JavaClass clazz) {
        String pkg = clazz.getPackageName();
        return pkg.contains(".service") || pkg.contains(".controller");
    }
}
