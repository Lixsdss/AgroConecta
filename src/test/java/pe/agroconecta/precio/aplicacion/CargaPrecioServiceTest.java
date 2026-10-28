package pe.agroconecta.precio.aplicacion;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pe.agroconecta.precio.infraestructura.PrecioMercadoRepository;

@ExtendWith(MockitoExtension.class)
class CargaPrecioServiceTest {

    @Mock private PrecioMercadoRepository precios;
    @InjectMocks private CargaPrecioService servicio;

    @Test
    void registrarConUnaFuenteDeTextoQueNoExisteNoGuardaNada() {
        assertThatThrownBy(() -> servicio.registrar(1L, 1, 1, null, BigDecimal.ONE, "RADIO_LOCAL"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(precios);
    }
}
