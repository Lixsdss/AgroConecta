package pe.agroconecta.demo.aplicacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import net.datafaker.Faker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.catalogo.aplicacion.CultivoDto;
import pe.agroconecta.catalogo.aplicacion.DistritoDto;
import pe.agroconecta.catalogo.aplicacion.UnidadDto;
import pe.agroconecta.catalogo.aplicacion.VariedadDto;
import pe.agroconecta.mensajeria.aplicacion.ConversacionService;
import pe.agroconecta.mensajeria.aplicacion.NuevoMensajeDto;
import pe.agroconecta.precio.aplicacion.CargaPrecioService;
import pe.agroconecta.publicacion.aplicacion.NuevaPublicacionDto;
import pe.agroconecta.publicacion.aplicacion.PublicacionService;
import pe.agroconecta.usuario.aplicacion.GestionCuentasService;
import pe.agroconecta.usuario.aplicacion.NuevoAdministradorDto;
import pe.agroconecta.usuario.aplicacion.RegistroDto;
import pe.agroconecta.usuario.aplicacion.RegistroService;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.venta.aplicacion.AccionVenta;
import pe.agroconecta.venta.aplicacion.CalificacionService;
import pe.agroconecta.venta.aplicacion.NuevaVentaDto;
import pe.agroconecta.venta.aplicacion.ParteDto;
import pe.agroconecta.venta.aplicacion.VentaService;
import pe.agroconecta.ventaagrupada.aplicacion.NuevaPropuestaDto;
import pe.agroconecta.ventaagrupada.aplicacion.PropuestaService;

/**
 * Crea los datos de demostración (ADR-006) a través de los servicios de cada módulo, así pasan por las mismas
 * validaciones y triggers que el uso real. Datafaker inventa nombres, negocios, textos y variaciones de precio con
 * una semilla fija, de modo que una base nueva siempre recibe los mismos datos. Los distritos, cultivos, variedades y
 * unidades salen del catálogo de {@code V2}.
 */
@Service
public class CargaDatosDemoService {

    /** Celular del administrador de demostración; si ya existe, los datos ya están cargados. */
    public static final String CELULAR_ADMIN = "900000001";
    /** Contraseña de todos los usuarios de demostración. */
    public static final String CONTRASENA = "Demo2026";
    /** Celulares de los agricultores de demostración. */
    public static final List<String> CELULARES_AGRICULTORES =
            List.of("911000001", "911000002", "911000003", "911000004");
    /** Celulares de los compradores de demostración. */
    public static final List<String> CELULARES_COMPRADORES = List.of("922000001", "922000002", "922000003");

    static final long SEMILLA = 2026L;
    static final int DIAS_DE_PRECIOS = 14;

    private static final String UBIGEO_PIURA = "200101";
    private static final String UBIGEO_CASTILLA = "200104";
    private static final String UBIGEO_TAMBOGRANDE = "200114";
    private static final String UBIGEO_CHULUCANAS = "200401";
    private static final String UBIGEO_SULLANA = "200601";
    private static final List<String> ZONAS = List.of(UBIGEO_PIURA, UBIGEO_TAMBOGRANDE, UBIGEO_CHULUCANAS,
            UBIGEO_SULLANA);

    /** Precio base por kilo de cada cultivo, del que salen los precios de referencia y los de las cosechas. */
    private static final Map<String, BigDecimal> PRECIO_BASE_KG = Map.of("Mango", new BigDecimal("1.10"),
            "Limón", new BigDecimal("2.20"), "Arroz", new BigDecimal("1.30"), "Plátano", new BigDecimal("0.70"));

    private static final Logger LOG = LoggerFactory.getLogger(CargaDatosDemoService.class);

    private final CatalogoService catalogo;
    private final UsuarioConsultaService consultaUsuarios;
    private final RegistroService registro;
    private final GestionCuentasService cuentas;
    private final CargaPrecioService precios;
    private final PublicacionService publicaciones;
    private final ConversacionService conversaciones;
    private final VentaService ventas;
    private final CalificacionService calificaciones;
    private final PropuestaService propuestas;
    private final Clock reloj;

    /**
     * Crea el servicio.
     *
     * @param catalogo         catálogo de distritos, cultivos, variedades y unidades
     * @param consultaUsuarios consulta de usuarios (para saber si ya se cargó)
     * @param registro         registro de agricultores y compradores
     * @param cuentas          creación del administrador
     * @param precios          carga de precios de referencia
     * @param publicaciones    publicación de cosechas
     * @param conversaciones   conversaciones y mensajes
     * @param ventas           acuerdos y seguimiento de ventas
     * @param calificaciones   calificaciones de ventas completadas
     * @param propuestas       propuestas conjuntas
     * @param reloj            reloj de la aplicación
     */
    public CargaDatosDemoService(CatalogoService catalogo, UsuarioConsultaService consultaUsuarios,
            RegistroService registro, GestionCuentasService cuentas, CargaPrecioService precios,
            PublicacionService publicaciones, ConversacionService conversaciones, VentaService ventas,
            CalificacionService calificaciones, PropuestaService propuestas, Clock reloj) {
        this.catalogo = catalogo;
        this.consultaUsuarios = consultaUsuarios;
        this.registro = registro;
        this.cuentas = cuentas;
        this.precios = precios;
        this.publicaciones = publicaciones;
        this.conversaciones = conversaciones;
        this.ventas = ventas;
        this.calificaciones = calificaciones;
        this.propuestas = propuestas;
        this.reloj = reloj;
    }

    /**
     * Carga los datos de demostración en una sola transacción: o se crean todos o ninguno.
     *
     * @return cantidad de datos creados, o {@code null} si ya estaban cargados
     */
    @Transactional
    public ResumenDemoDto cargar() {
        if (consultaUsuarios.existeCelular(CELULAR_ADMIN)) {
            LOG.info("Los datos de demostración ya están cargados");
            return null;
        }
        return new Carga().ejecutar();
    }

    /** Una carga completa, con su generador de datos y el catálogo resuelto. */
    private final class Carga {

        private final Faker faker = new Faker(Locale.forLanguageTag("es"), new Random(SEMILLA));
        private final LocalDate hoy = LocalDate.now(reloj);
        private final List<CultivoDto> cultivos = catalogo.listarCultivos();
        private final List<UnidadDto> unidades = catalogo.listarUnidades();
        private int cantidadPrecios;
        private int cantidadPublicaciones;
        private int cantidadVentas;

        ResumenDemoDto ejecutar() {
            long admin = cuentas.crearAdministrador(new NuevoAdministradorDto("Administrador Demo", CELULAR_ADMIN,
                    distrito(UBIGEO_PIURA).id(), CONTRASENA));
            cargarPrecios(admin);

            long a1 = agricultor(0, UBIGEO_TAMBOGRANDE);
            long a2 = agricultor(1, UBIGEO_TAMBOGRANDE);
            long a3 = agricultor(2, UBIGEO_CHULUCANAS);
            long a4 = agricultor(3, UBIGEO_SULLANA);
            long mayorista = comprador(0, UBIGEO_PIURA, "MERCADO_MAYORISTA", "Mercado Mayorista");
            long restaurante = comprador(1, UBIGEO_CASTILLA, "RESTAURANTE", "Restaurante");
            long procesadora = comprador(2, UBIGEO_SULLANA, "PROCESADORA", "Procesadora");

            // La primera cosecha de cada agricultor entra a la cola de moderación (PRIMERA_PUBLICACION)
            long kentA1 = publicar(a1, "Mango", "Kent", "Jaba", 120, "20", UBIGEO_TAMBOGRANDE, 1.0, 2);
            long edwardA1 = publicar(a1, "Mango", "Edward", "Kilo", 800, null, UBIGEO_TAMBOGRANDE, 1.0, 1);
            long limonA1 = publicar(a1, "Limón", "Sutil", "Saco", 30, "45", UBIGEO_TAMBOGRANDE, 1.0, 3);
            publicar(a2, "Mango", "Kent", "Jaba", 80, "22", UBIGEO_TAMBOGRANDE, 1.0, 1);
            publicar(a2, "Plátano", "Seda", "Tonelada", 3, null, UBIGEO_TAMBOGRANDE, 1.0, 2);
            long limonA3 = publicar(a3, "Limón", "Sutil", "Saco", 60, "45", UBIGEO_CHULUCANAS, 1.0, 2);
            long tahitiA3 = publicar(a3, "Limón", "Tahití", "Kilo", 1500, null, UBIGEO_CHULUCANAS, 1.0, 1);
            long hadenA3 = publicar(a3, "Mango", "Haden", "Jaba", 50, "20", UBIGEO_CHULUCANAS, 1.0, 3);
            long bellacoA4 = publicar(a4, "Plátano", "Bellaco", "Tonelada", 5, null, UBIGEO_SULLANA, 1.0, 1);
            long arrozA4 = publicar(a4, "Arroz", "NIR", "Saco", 100, "50", UBIGEO_SULLANA, 1.0, 5);
            publicar(a4, "Arroz", "IR-43", "Quintal", 40, null, UBIGEO_SULLANA, 1.0, 4);
            // Precio 40 % sobre la referencia de Sullana: queda marcada PRECIO_FUERA_RANGO para el administrador
            publicar(a4, "Mango", "Kent", "Jaba", 40, "20", UBIGEO_SULLANA, 1.4, 1);

            conversacionConOferta(restaurante, a3, tahitiA3);

            // Una venta en cada punto del ciclo (RF-06, RF-11)
            acordar(mayorista, a1, edwardA1, 200, 2);
            long recogida = acordar(restaurante, a3, limonA3, 10, 1);
            ventas.cambiarEstado(a3, recogida, AccionVenta.RECOGIDO);
            long enCamino = acordar(procesadora, a4, arrozA4, 20, 1);
            ventas.cambiarEstado(a4, enCamino, AccionVenta.RECOGIDO);
            ventas.cambiarEstado(a4, enCamino, AccionVenta.EN_CAMINO);
            long enFinca = acordar(mayorista, a4, bellacoA4, 1, 0);
            ventas.cambiarEstado(mayorista, enFinca, AccionVenta.CONFIRMAR_RECEPCION);
            calificar(mayorista, enFinca);
            long completada = acordar(restaurante, a3, hadenA3, 15, 1);
            ventas.cambiarEstado(a3, completada, AccionVenta.RECOGIDO);
            ventas.cambiarEstado(a3, completada, AccionVenta.EN_CAMINO);
            ventas.cambiarEstado(restaurante, completada, AccionVenta.CONFIRMAR_RECEPCION);
            calificar(restaurante, completada);
            calificar(a3, completada);
            long cancelada = acordar(procesadora, a1, limonA1, 5, 2);
            ventas.cambiarEstado(procesadora, cancelada, AccionVenta.CANCELAR);

            propuestaConjunta(mayorista, a1, kentA1);

            ResumenDemoDto resumen = new ResumenDemoDto(1 + CELULARES_AGRICULTORES.size()
                    + CELULARES_COMPRADORES.size(), cantidadPrecios, cantidadPublicaciones, cantidadVentas);
            LOG.info("Datos de demostración cargados: {}", resumen);
            return resumen;
        }

        /** Precios de los últimos días en las 4 zonas, con una variación diaria de ±5 % sobre el precio base. */
        private void cargarPrecios(long admin) {
            for (String ubigeo : ZONAS) {
                DistritoDto zona = distrito(ubigeo);
                String fuente = switch (ubigeo) {
                    case UBIGEO_PIURA -> "MERCADO_MAYORISTA_PIURA";
                    case UBIGEO_SULLANA -> "MERCADO_SULLANA";
                    default -> "ENCUESTA_ACOPIADORES";
                };
                for (CultivoDto cultivo : cultivos) {
                    for (int dia = DIAS_DE_PRECIOS - 1; dia >= 0; dia--) {
                        precios.registrar(admin, cultivo.id(), zona.id(), hoy.minusDays(dia),
                                precioKg(cultivo.nombre(), variacion(5)).setScale(2, RoundingMode.HALF_UP), fuente);
                        cantidadPrecios++;
                    }
                }
            }
        }

        private long agricultor(int indice, String ubigeo) {
            String nombre = nombrePeruano();
            String chacra = "Fundo " + faker.options().option("El Algarrobo", "Santa Rosa", "La Esperanza",
                    "San Martín", "Los Tamarindos", "El Porvenir");
            return registro.registrar(RegistroDto.deTexto("AGRICULTOR", nombre, CELULARES_AGRICULTORES.get(indice),
                    distrito(ubigeo).id(), CONTRASENA, chacra, null, null, null)).idUsuario();
        }

        private long comprador(int indice, String ubigeo, String tipo, String prefijoNegocio) {
            String negocio = prefijoNegocio + " " + faker.name().lastName();
            String ruc = faker.numerify("20#########");
            return registro.registrar(RegistroDto.deTexto("COMPRADOR", nombrePeruano(),
                    CELULARES_COMPRADORES.get(indice), distrito(ubigeo).id(), CONTRASENA, null, negocio, tipo, ruc))
                    .idUsuario();
        }

        /** Nombre con dos apellidos, como se usa en Perú. */
        private String nombrePeruano() {
            return faker.name().firstName() + " " + faker.name().lastName() + " " + faker.name().lastName();
        }

        /**
         * Publica una cosecha con un precio cercano a la referencia (±3 %), multiplicado por {@code factorPrecio}.
         */
        private long publicar(long idAgricultor, String cultivo, String variedad, String unidad, int cantidad,
                String pesoVariableKg, String ubigeo, double factorPrecio, int diasDesdeCosecha) {
            UnidadDto u = unidad(unidad);
            BigDecimal peso = pesoVariableKg == null ? u.factorKg() : new BigDecimal(pesoVariableKg);
            BigDecimal precioKg = precioKg(cultivo, variacion(3)).multiply(BigDecimal.valueOf(factorPrecio));
            BigDecimal precioUnitario = precioKg.multiply(peso).setScale(2, RoundingMode.HALF_UP);
            long id = publicaciones.publicar(idAgricultor, new NuevaPublicacionDto(variedad(cultivo, variedad).id(),
                    u.id(), BigDecimal.valueOf(cantidad), pesoVariableKg == null ? null : peso, precioUnitario,
                    distrito(ubigeo).id(), hoy.minusDays(diasDesdeCosecha), null)).id();
            cantidadPublicaciones++;
            return id;
        }

        /** Conversación sin venta: preguntas y una oferta que el agricultor aún no responde (RF-04). */
        private void conversacionConOferta(long comprador, long agricultor, long idPublicacion) {
            long conversacion = conversaciones.abrir(comprador, idPublicacion).conversacion().id();
            texto(comprador, conversacion, faker.options().option("Buenas, ¿todavía tiene el limón disponible?",
                    "Hola, ¿el limón sigue disponible?"));
            texto(agricultor, conversacion, "Sí, está recién cosechado. ¿Cuánto necesita?");
            conversaciones.enviar(comprador, conversacion, new NuevoMensajeDto("OFERTA",
                    "Le propongo 300 kilos a S/ 2.10 el kilo.", new BigDecimal("300"), new BigDecimal("2.10")));
        }

        /**
         * Conversa, y el comprador registra el acuerdo en la unidad de la publicación (RF-06): queda ACORDADA.
         *
         * @return id de la venta
         */
        private long acordar(long comprador, long agricultor, long idPublicacion, int cantidad, int diasHastaRecojo) {
            long conversacion = conversaciones.abrir(comprador, idPublicacion).conversacion().id();
            texto(comprador, conversacion, faker.options().option("Buenos días, me interesa su cosecha.",
                    "Hola, ¿a cuánto me deja si le compro una buena cantidad?", "Buenas tardes, ¿cuándo puedo recoger?"));
            texto(agricultor, conversacion, faker.options().option("Claro, conversemos.",
                    "Puede pasar por la chacra cuando guste.", "Le hago un buen precio."));
            var publicacion = publicaciones.obtener(idPublicacion, agricultor);
            long id = ventas.registrar(comprador, new NuevaVentaDto(conversacion, publicacion.idUnidad(),
                    BigDecimal.valueOf(cantidad), null, publicacion.precioUnitario(), hoy.plusDays(diasHastaRecojo)))
                    .id();
            cantidadVentas++;
            return id;
        }

        private void texto(long remitente, long conversacion, String contenido) {
            conversaciones.enviar(remitente, conversacion, new NuevoMensajeDto("TEXTO", contenido, null, null));
        }

        private void calificar(long evaluador, long idVenta) {
            calificaciones.calificar(evaluador, idVenta, faker.number().numberBetween(4, 6),
                    faker.options().option("Todo conforme, muy puntual.", "Buena calidad y buen trato.",
                            "Cumplió con lo acordado.", "Recomendado."));
        }

        /**
         * Propuesta conjunta del comprador al lote de mango Kent de Tambogrande (RF-10). El primer agricultor acepta
         * su parte y la del otro queda pendiente.
         */
        private void propuestaConjunta(long comprador, long agricultorQueAcepta, long idPublicacion) {
            var cosecha = publicaciones.obtener(idPublicacion, agricultorQueAcepta);
            long idPropuesta = propuestas.crear(comprador, new NuevaPropuestaDto(cosecha.idVariedad(),
                    cosecha.idDistrito(), new BigDecimal("1000"), precioKg("Mango", 0.95).setScale(2,
                            RoundingMode.HALF_UP), hoy.plusDays(3))).id();
            String nombre = cosecha.agricultor();
            for (ParteDto parte : ventas.partesDePropuesta(idPropuesta)) {
                cantidadVentas++;
                if (parte.agricultor().equals(nombre)) {
                    ventas.cambiarEstado(agricultorQueAcepta, parte.idVenta(), AccionVenta.ACEPTAR);
                }
            }
        }

        /** Factor de variación al azar entre -{@code porcentaje} % y +{@code porcentaje} %. */
        private double variacion(int porcentaje) {
            return 1 + faker.number().numberBetween(-porcentaje, porcentaje + 1) / 100.0;
        }

        private BigDecimal precioKg(String cultivo, double factor) {
            return PRECIO_BASE_KG.get(cultivo).multiply(BigDecimal.valueOf(factor)).setScale(4, RoundingMode.HALF_UP);
        }

        private DistritoDto distrito(String ubigeo) {
            return catalogo.buscarDistritoPorUbigeo(ubigeo)
                    .orElseThrow(() -> new IllegalStateException("No existe el distrito " + ubigeo + " (V2)"));
        }

        private UnidadDto unidad(String nombre) {
            return unidades.stream().filter(u -> u.nombre().equals(nombre)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No existe la unidad " + nombre + " (V2)"));
        }

        private VariedadDto variedad(String cultivo, String variedad) {
            CultivoDto c = cultivos.stream().filter(x -> x.nombre().equals(cultivo)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No existe el cultivo " + cultivo + " (V2)"));
            return catalogo.listarVariedades(c.id()).stream().filter(v -> v.nombre().equals(variedad)).findFirst()
                    .orElseThrow(() -> new IllegalStateException("No existe la variedad " + variedad + " (V2)"));
        }
    }
}
