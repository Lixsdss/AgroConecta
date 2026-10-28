package pe.agroconecta.demo.aplicacion;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class IniciadorDemoTest {

    private final CargaDatosDemoService carga = mock(CargaDatosDemoService.class);

    @Test
    void cargaLosDatosConElPerfilDemo() {
        new IniciadorDemo(carga, entorno("dev", "demo")).run(null);

        verify(carga).cargar();
    }

    @Test
    void seNiegaACargarConElPerfilProd() {
        new IniciadorDemo(carga, entorno("prod", "demo")).run(null);

        verifyNoInteractions(carga);
    }

    @Test
    void unErrorEnLaCargaNoDetieneElArranque() {
        when(carga.cargar()).thenThrow(new IllegalStateException("falla"));

        assertThatCode(() -> new IniciadorDemo(carga, entorno("dev", "demo")).run(null)).doesNotThrowAnyException();
    }

    private static MockEnvironment entorno(String... perfiles) {
        MockEnvironment entorno = new MockEnvironment();
        entorno.setActiveProfiles(perfiles);
        return entorno;
    }
}
