package pe.agroconecta.demo.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.agroconecta.catalogo.aplicacion.CatalogoService;
import pe.agroconecta.usuario.aplicacion.GestionCuentasService;
import pe.agroconecta.usuario.aplicacion.RegistroService;
import pe.agroconecta.usuario.aplicacion.UsuarioConsultaService;

/** La carga completa se prueba contra MariaDB en {@code DatosDemoIntegracionTest}. */
@ExtendWith(MockitoExtension.class)
class CargaDatosDemoServiceTest {

    @Mock private CatalogoService catalogo;
    @Mock private UsuarioConsultaService consultaUsuarios;
    @Mock private RegistroService registro;
    @Mock private GestionCuentasService cuentas;
    @InjectMocks private CargaDatosDemoService servicio;

    @Test
    void siElAdministradorDeDemostracionYaExisteNoCargaNada() {
        when(consultaUsuarios.existeCelular(CargaDatosDemoService.CELULAR_ADMIN)).thenReturn(true);

        assertThat(servicio.cargar()).isNull();
        verifyNoInteractions(catalogo, registro, cuentas);
    }
}
