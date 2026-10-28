package pe.agroconecta;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import java.util.List;
import java.util.Optional;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Reglas de la arquitectura modular (CLAUDE.md, sección "Arquitectura modular"), verificadas con ArchUnit.
 *
 * <p>Todas las reglas usan {@code allowEmptyShould(true)}: mientras un módulo o una capa no tenga clases,
 * la regla pasa en lugar de fallar por no encontrar nada que verificar.</p>
 */
@AnalyzeClasses(packages = "pe.agroconecta", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTest {

    private static final String RAIZ = "pe.agroconecta";

    /** Módulos de negocio; deben coincidir con el mapa de módulos de CLAUDE.md. */
    static final List<String> MODULOS = List.of("catalogo", "usuario", "publicacion", "precio", "mensajeria",
            "venta", "ventaagrupada", "notificacion", "administracion", "demo");

    private static final String COMPARTIDO = "compartido";

    /** Regla 1: un módulo usa a otro solo a través de la capa {@code aplicacion} de ese otro módulo. */
    @ArchTest
    static final ArchRule modulosSeComunicanSoloPorAplicacion = classes()
            .that().resideInAnyPackage(paquetesDeModulos())
            .should(accederAOtrosModulosSoloPorAplicacion())
            .allowEmptyShould(true)
            .because("otro módulo solo expone su capa aplicacion; api, dominio e infraestructura son internos");

    /** Regla 2: los repositorios (infraestructura) solo los usa la capa aplicacion de su propio módulo. */
    @ArchTest
    static final ArchRule infraestructuraSoloDesdeSuAplicacion = CompositeArchRule.of(
            MODULOS.stream().map(ArquitecturaTest::infraestructuraSoloDesdeAplicacion).toList())
            .allowEmptyShould(true);

    /** Regla 3: los controladores (api) dependen de aplicacion y nunca directamente de infraestructura. */
    @ArchTest
    static final ArchRule apiNoDependeDeInfraestructura = noClasses()
            .that().resideInAPackage(RAIZ + ".*.api..")
            .should().dependOnClassesThat().resideInAPackage(RAIZ + ".*.infraestructura..")
            .allowEmptyShould(true)
            .because("los controladores llaman a los servicios de aplicacion, no a los repositorios");

    /** Regla 4: compartido no depende de ningún módulo de negocio. */
    @ArchTest
    static final ArchRule compartidoNoDependeDeModulos = noClasses()
            .that().resideInAPackage(RAIZ + "." + COMPARTIDO + "..")
            .should().dependOnClassesThat().resideInAnyPackage(paquetesDeModulos())
            .allowEmptyShould(true)
            .because("compartido es transversal; los módulos implementan sus interfaces, no al revés");

    /** Regla 5: no hay ciclos de dependencias entre módulos. */
    @ArchTest
    static final ArchRule sinCiclosEntreModulos = slices()
            .matching(RAIZ + ".(*)..")
            .should().beFreeOfCycles()
            .allowEmptyShould(true);

    private static String[] paquetesDeModulos() {
        return MODULOS.stream().map(m -> RAIZ + "." + m + "..").toArray(String[]::new);
    }

    private static ArchRule infraestructuraSoloDesdeAplicacion(String modulo) {
        String base = RAIZ + "." + modulo;
        return classes()
                .that().resideInAPackage(base + ".infraestructura..")
                .should().onlyHaveDependentClassesThat()
                .resideInAnyPackage(base + ".aplicacion..", base + ".infraestructura..")
                .allowEmptyShould(true)
                .because("la infraestructura de " + modulo + " es interna y solo la usa su capa aplicacion");
    }

    /** Módulo de negocio al que pertenece una clase, si pertenece a alguno. */
    private static Optional<String> moduloDe(JavaClass clase) {
        String paquete = clase.getPackageName();
        if (!paquete.startsWith(RAIZ + ".")) {
            return Optional.empty();
        }
        String modulo = paquete.substring(RAIZ.length() + 1).split("\\.")[0];
        return MODULOS.contains(modulo) ? Optional.of(modulo) : Optional.empty();
    }

    private static ArchCondition<JavaClass> accederAOtrosModulosSoloPorAplicacion() {
        return new ArchCondition<>("acceder a otros módulos solo a través de su capa aplicacion") {
            @Override
            public void check(JavaClass origen, ConditionEvents eventos) {
                String moduloOrigen = moduloDe(origen).orElseThrow();
                for (Dependency dependencia : origen.getDirectDependenciesFromSelf()) {
                    JavaClass destino = dependencia.getTargetClass();
                    Optional<String> moduloDestino = moduloDe(destino);
                    if (moduloDestino.isEmpty() || moduloDestino.get().equals(moduloOrigen)) {
                        continue;
                    }
                    String aplicacionDestino = RAIZ + "." + moduloDestino.get() + ".aplicacion";
                    if (!esPaqueteOSubpaquete(destino.getPackageName(), aplicacionDestino)) {
                        eventos.add(SimpleConditionEvent.violated(dependencia, dependencia.getDescription()
                                + " (el módulo " + moduloOrigen + " solo puede usar " + aplicacionDestino + ")"));
                    }
                }
            }
        };
    }

    private static boolean esPaqueteOSubpaquete(String paquete, String base) {
        return paquete.equals(base) || paquete.startsWith(base + ".");
    }
}
