package com.jouney.workflow.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Test;

/**
 * T077 — impede dependência indevida entre os módulos do monólito (seção 3 do planejamento.md).
 *
 * <p>ponytail: {@code runtime} e {@code humantask} têm um acoplamento bidirecional intencional (o
 * motor cria tarefas humanas; concluir uma tarefa retoma o motor) — não é imposta uma regra "sem
 * ciclo" entre esses dois, ela quebraria por design. Se isso incomodar no futuro, o upgrade é
 * introduzir um `ExecutionResumeCommand` publicado via evento em vez de humantask chamar runtime
 * diretamente.
 */
class ModuleBoundariesTest {

  private static final com.tngtech.archunit.core.domain.JavaClasses CLASSES =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.jouney.workflow");

  @Test
  void definitionDoesNotDependOnRuntimeOrHumantaskOrConnector() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage("..definition..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..runtime..", "..humantask..", "..connector..", "..publication..")
        .check(CLASSES);
  }

  @Test
  void sharedKernelDoesNotDependOnBusinessModules() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage("..shared..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..definition..",
            "..publication..",
            "..runtime..",
            "..humantask..",
            "..connector..",
            "..audit..",
            "..identity..")
        .check(CLASSES);
  }

  @Test
  void identityDoesNotDependOnBusinessModules() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage("..identity..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..definition..", "..publication..", "..runtime..", "..humantask..", "..connector..")
        .check(CLASSES);
  }

  @Test
  void auditDoesNotDependOnBusinessModules() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage("..audit..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "..definition..", "..publication..", "..runtime..", "..humantask..", "..connector..")
        .check(CLASSES);
  }

  @Test
  void connectorDoesNotDependOnRuntimeOrHumantaskOrDefinition() {
    ArchRuleDefinition.noClasses()
        .that()
        .resideInAPackage("..connector..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..runtime..", "..humantask..", "..definition..", "..publication..")
        .check(CLASSES);
  }
}
