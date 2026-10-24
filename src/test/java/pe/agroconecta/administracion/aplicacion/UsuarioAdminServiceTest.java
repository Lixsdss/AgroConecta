package pe.agroconecta.administracion.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import pe.agroconecta.administracion.dominio.MotivoSuspension;
import pe.agroconecta.administracion.dominio.ResumenUsuario;
import pe.agroconecta.administracion.dominio.Suspension;
import pe.agroconecta.administracion.infraestructura.ResumenUsuarioRepository;
import pe.agroconecta.administracion.infraestructura.SuspensionRepository;
import pe.agroconecta.compartido.error.RecursoNoEncontradoException;
import pe.agroconecta.compartido.error.ReglaNegocioException;
import pe.agroconecta.compartido.error.SolicitudInvalidaException;
import pe.agroconecta.notificacion.aplicacion.NotificacionService;
import pe.agroconecta.notificacion.aplicacion.NuevaNotificacionDto;
import pe.agroconecta.usuario.aplicacion.CuentaDto;
import pe.agroconecta.usuario.aplicacion.GestionCuentasService;
import pe.agroconecta.usuario.aplicacion.NuevoAdministradorDto;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;
import pe.agroconecta.usuario.aplicacion.UsuarioResumenDto;
import pe.agroconecta.venta.aplicacion.VentaEnCursoDto;
import pe.agroconecta.venta.aplicacion.VentaService;

@ExtendWith(MockitoExtension.class)
class UsuarioAdminServiceTest {

        @Mock
        private ResumenUsuarioRepository resumenes;
        @Mock
        private SuspensionRepository suspensiones;
        @Mock
        private GestionCuentasService cuentas;
        @Mock
        private UsuarioConsultaService usuarios;
        @Mock
        private VentaService ventas;
        @Mock
        private NotificacionService notificaciones;
        @Mock
        private EntityManager entityManager;

        private UsuarioAdminService servicio;

        @BeforeEach
        void preparar() {
                servicio = new UsuarioAdminService(resumenes, suspensiones, cuentas, usuarios, ventas, notificaciones,
                                entityManager);
        }

        @Test
        void suspenderAvisaAlUsuarioYALaOtraParteDeLasVentasAcordadasOPropuestas() {
                when(resumenes.findById(7L)).thenReturn(Optional.of(fila(7L, "COMPRADOR", "ACTIVO", 0L)));
                when(cuentas.cuenta(7L)).thenReturn(new CuentaDto(7L, "COMPRADOR", "SUSPENDIDO", LocalDateTime.now()));
                when(ventas.enCursoDe(7L)).thenReturn(List.of(new VentaEnCursoDto(21L, "ACORDADA", 3L, "Mango Kent"),
                                new VentaEnCursoDto(22L, "EN_CAMINO", 4L, "Limón Sutil"),
                                new VentaEnCursoDto(23L, "PROPUESTA", 5L, "Mango Kent")));
                Suspension vigente = new Suspension(7L, 1L, MotivoSuspension.USO_INDEBIDO, "Detalle.");
                when(suspensiones.findFirstByIdUsuarioAndFechaFinIsNull(7L)).thenReturn(Optional.of(vigente));
                when(usuarios.buscarResumenes(anyCollection())).thenReturn(Map.of(1L,
                                new UsuarioResumenDto(1L, "Lucía Farfán", "900000009", "Piura", null, "ADMINISTRADOR",
                                                null)));

                UsuarioDetalleDto detalle = servicio.suspender(1L, 7L, MotivoSuspension.USO_INDEBIDO, "  Detalle.  ");

                assertThat(detalle.suspension().administrador()).isEqualTo("Lucía Farfán");
                assertThat(detalle.ventasEnCurso()).hasSize(3);
                ArgumentCaptor<NuevaNotificacionDto> avisos = ArgumentCaptor.forClass(NuevaNotificacionDto.class);
                verify(notificaciones, times(3)).crear(avisos.capture());
                assertThat(avisos.getAllValues()).extracting(NuevaNotificacionDto::idUsuario).containsExactly(7L, 3L,
                                5L);
                assertThat(avisos.getAllValues().getFirst().contenido()).isEqualTo("Motivo: uso indebido. Detalle.");
                assertThat(avisos.getAllValues().get(1).tipo()).isEqualTo("VENTA");
        }

        @Test
        void validaLasReglasDeSuspensionAntesDeInsertar() {
                when(resumenes.findById(1L)).thenReturn(Optional.of(fila(1L, "ADMINISTRADOR", "ACTIVO", 0L)));
                when(resumenes.findById(3L)).thenReturn(Optional.of(fila(3L, "AGRICULTOR", "ACTIVO", 2L)));
                when(resumenes.findById(4L)).thenReturn(Optional.of(fila(4L, "AGRICULTOR", "SUSPENDIDO", 0L)));
                when(resumenes.findById(5L)).thenReturn(Optional.of(fila(5L, "AGRICULTOR", "INACTIVO", 0L)));

                assertThatThrownBy(() -> servicio.suspender(1L, 1L, MotivoSuspension.USO_INDEBIDO, "x"))
                                .hasMessage(UsuarioAdminService.MENSAJE_SUSPENDER_ADMIN);
                assertThatThrownBy(() -> servicio.suspender(1L, 3L, MotivoSuspension.RECLAMOS_FUNDADOS, "x"))
                                .isInstanceOf(ReglaNegocioException.class)
                                .hasMessage("Se requieren al menos 3 reclamos fundados (tiene 2).");
                assertThatThrownBy(() -> servicio.suspender(1L, 4L, MotivoSuspension.USO_INDEBIDO, "x"))
                                .hasMessage(UsuarioAdminService.MENSAJE_YA_SUSPENDIDO);
                assertThatThrownBy(() -> servicio.suspender(1L, 5L, MotivoSuspension.USO_INDEBIDO, "x"))
                                .hasMessage(UsuarioAdminService.MENSAJE_SOLO_ACTIVA);
                assertThatThrownBy(() -> servicio.suspender(1L, 9L, MotivoSuspension.USO_INDEBIDO, "x"))
                                .isInstanceOf(RecursoNoEncontradoException.class);
                verify(suspensiones, never()).saveAndFlush(any());
        }

        @Test
        void reactivarLevantaLaSuspensionVigenteYAvisa() {
                when(resumenes.findById(7L)).thenReturn(Optional.of(fila(7L, "COMPRADOR", "SUSPENDIDO", 0L)));
                Suspension vigente = new Suspension(7L, 1L, MotivoSuspension.USO_INDEBIDO, "Detalle.");
                ReflectionTestUtils.setField(vigente, "id", 2L);
                when(suspensiones.findFirstByIdUsuarioAndFechaFinIsNull(7L)).thenReturn(Optional.of(vigente),
                                Optional.empty());
                when(cuentas.cuenta(7L)).thenReturn(new CuentaDto(7L, "COMPRADOR", "ACTIVO", null));

                UsuarioDetalleDto detalle = servicio.reactivar(7L);

                verify(suspensiones).levantar(2L);
                assertThat(detalle.suspension()).isNull();
                verify(notificaciones).crear(any(NuevaNotificacionDto.class));
                when(suspensiones.findFirstByIdUsuarioAndFechaFinIsNull(7L)).thenReturn(Optional.empty());
                assertThatThrownBy(() -> servicio.reactivar(7L)).hasMessage(UsuarioAdminService.MENSAJE_NO_SUSPENDIDO);
        }

        @Test
        void listaConFiltrosValidadosYDelegaLasAccionesDeCuenta() {
                when(resumenes.buscar(eq("AGRICULTOR"), isNull(), eq("%juan%"), any(Pageable.class)))
                                .thenReturn(Page.empty());
                assertThat(servicio.listar(" AGRICULTOR ", "", "  Juan ", 0)).isEmpty();
                assertThatThrownBy(() -> servicio.listar("JEFE", null, null, 0))
                                .isInstanceOf(SolicitudInvalidaException.class);
                assertThatThrownBy(() -> servicio.listar(null, "BORRADO", null, 0))
                                .isInstanceOf(SolicitudInvalidaException.class);
                assertThatThrownBy(() -> servicio.listar(null, null, null, -1))
                                .isInstanceOf(SolicitudInvalidaException.class);

                when(cuentas.reiniciarContrasena(3L)).thenReturn("abc123defg");
                assertThat(servicio.contrasenaTemporal(3L)).isEqualTo("abc123defg");

                when(resumenes.findById(3L)).thenReturn(Optional.of(fila(3L, "AGRICULTOR", "INACTIVO", 0L)));
                when(cuentas.cuenta(3L)).thenReturn(new CuentaDto(3L, "AGRICULTOR", "INACTIVO", null));
                when(entityManager.find(ResumenUsuario.class, 3L)).thenReturn(null);
                assertThat(servicio.desactivar(3L).estado()).isEqualTo("INACTIVO");
                verify(cuentas).desactivar(3L);
                servicio.activar(3L);
                verify(cuentas).activar(3L);

                NuevoAdministradorDto datos = new NuevoAdministradorDto("Carlos Ruiz", "900000029", 1, "admin2027");
                when(cuentas.crearAdministrador(datos)).thenReturn(12L);
                when(resumenes.findById(12L)).thenReturn(Optional.of(fila(12L, "ADMINISTRADOR", "ACTIVO", 0L)));
                when(cuentas.cuenta(12L)).thenReturn(new CuentaDto(12L, "ADMINISTRADOR", "ACTIVO", null));
                UsuarioDetalleDto admin = servicio.crearAdministrador(datos);
                assertThat(admin.rol()).isEqualTo("ADMINISTRADOR");
                assertThat(admin.ventasEnCurso()).isEmpty();
                verify(ventas, never()).enCursoDe(12L);
        }

        private static ResumenUsuario fila(long id, String rol, String estado, long fundados) {
                ResumenUsuario r = org.springframework.beans.BeanUtils.instantiateClass(ResumenUsuario.class);
                ReflectionTestUtils.setField(r, "id", id);
                ReflectionTestUtils.setField(r, "rol", rol);
                ReflectionTestUtils.setField(r, "estado", estado);
                ReflectionTestUtils.setField(r, "nombreCompleto", "Nombre " + id);
                ReflectionTestUtils.setField(r, "reclamosFundados", fundados);
                return r;
        }
}
