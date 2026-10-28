package pe.agroconecta.mensajeria.api;

import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pe.agroconecta.mensajeria.aplicacion.AperturaDto;
import pe.agroconecta.mensajeria.aplicacion.ConversacionDto;
import pe.agroconecta.mensajeria.aplicacion.ConversacionService;
import pe.agroconecta.mensajeria.aplicacion.EnlaceWhatsappDto;
import pe.agroconecta.mensajeria.aplicacion.MensajeDto;

/**
 * Chat entre comprador y agricultor ({@code /api/v1/conversaciones}, RF-04). Quien no participa en una
 * conversación recibe 403.
 */
@RestController
@RequestMapping("/api/v1/conversaciones")
public class ConversacionController {

    private static final String COMPRADOR = "COMPRADOR";

    private final ConversacionService conversaciones;

    /**
     * Crea el controlador.
     *
     * @param conversaciones servicio del chat
     */
    public ConversacionController(ConversacionService conversaciones) {
        this.conversaciones = conversaciones;
    }

    /**
     * "Escribir": abre la conversación con una publicación, o devuelve la que ya existía.
     *
     * @param jwt     token del comprador
     * @param request publicación
     * @return 201 con {@code Location} si se creó; 200 si ya existía
     */
    @PostMapping
    @PreAuthorize("hasRole('COMPRADOR')")
    public ResponseEntity<ConversacionDto> abrir(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AbrirConversacionRequest request) {
        AperturaDto apertura = conversaciones.abrir(usuario(jwt), request.idPublicacion());
        ConversacionDto c = apertura.conversacion();
        return apertura.creada()
                ? ResponseEntity.created(URI.create("/api/v1/conversaciones/" + c.id())).body(c)
                : ResponseEntity.ok(c);
    }

    /**
     * "WhatsApp": crea la conversación si no existía, suma un clic y devuelve el enlace {@code wa.me}.
     *
     * @param jwt     token del comprador
     * @param request publicación
     * @return conversación y enlace
     */
    @PostMapping("/whatsapp")
    @PreAuthorize("hasRole('COMPRADOR')")
    public EnlaceWhatsappDto whatsapp(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AbrirConversacionRequest request) {
        return conversaciones.whatsapp(usuario(jwt), request.idPublicacion());
    }

    /**
     * "Mensajes": conversaciones del usuario, con actividad más reciente primero (20 por página).
     *
     * @param jwt  token de la sesión
     * @param page página desde 0
     * @return página de conversaciones
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('AGRICULTOR', 'COMPRADOR')")
    public PagedModel<ConversacionDto> listar(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page) {
        return new PagedModel<>(conversaciones.listar(usuario(jwt), rol(jwt), page));
    }

    /**
     * Total de mensajes no leídos (aviso del inicio).
     *
     * @param jwt token de la sesión
     * @return {@code {"total": n}}
     */
    @GetMapping("/no-leidos")
    @PreAuthorize("hasAnyRole('AGRICULTOR', 'COMPRADOR')")
    public Map<String, Long> noLeidos(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("total", conversaciones.noLeidos(usuario(jwt), rol(jwt)));
    }

    /**
     * Cabecera del chat.
     *
     * @param jwt token de la sesión
     * @param id  id de la conversación
     * @return la conversación
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGRICULTOR', 'COMPRADOR')")
    public ConversacionDto obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return conversaciones.obtener(usuario(jwt), id);
    }

    /**
     * Mensajes de la conversación (y marca como leídos los de la otra parte).
     *
     * @param jwt       token de la sesión
     * @param id        id de la conversación
     * @param despuesDe id del último mensaje que ya se tiene (consulta periódica)
     * @return mensajes del más antiguo al más reciente
     */
    @GetMapping("/{id}/mensajes")
    @PreAuthorize("hasAnyRole('AGRICULTOR', 'COMPRADOR')")
    public List<MensajeDto> mensajes(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @RequestParam(required = false) Long despuesDe) {
        return conversaciones.mensajes(usuario(jwt), id, despuesDe);
    }

    /**
     * Envía un mensaje de texto o una oferta.
     *
     * @param jwt     token de la sesión
     * @param id      id de la conversación
     * @param request texto u oferta
     * @return 201 con el mensaje
     */
    @PostMapping("/{id}/mensajes")
    @PreAuthorize("hasAnyRole('AGRICULTOR', 'COMPRADOR')")
    public ResponseEntity<MensajeDto> enviar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
            @Valid @RequestBody EnviarMensajeRequest request) {
        MensajeDto mensaje = conversaciones.enviar(usuario(jwt), id, request.aDto());
        return ResponseEntity.created(URI.create("/api/v1/conversaciones/" + id + "/mensajes")).body(mensaje);
    }

    private static long usuario(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    private static String rol(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains(COMPRADOR) ? COMPRADOR : "AGRICULTOR";
    }
}
