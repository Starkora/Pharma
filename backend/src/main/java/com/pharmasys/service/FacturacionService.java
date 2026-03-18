package com.pharmasys.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.pharmasys.model.*;
import com.pharmasys.repository.ComprobanteRepository;
import com.pharmasys.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class FacturacionService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String ESTADO_ERROR = "ERROR";
    private static final String ESTADO_EMITIDO = "EMITIDO";
    private static final String TIPO_COMPROBANTE_BOLETA = "BOLETA";
    private static final String RAZON_SOCIAL_CONSUMIDOR_FINAL = "CONSUMIDOR FINAL";
    private static final String JSON_FIELD_ERRORS = "errors";
    private static final String JSON_FIELD_MESSAGE = "message";
    private static final String URL_PATH_SEPARATOR = "/";

    private final ComprobanteRepository comprobanteRepository;
    private final VentaRepository ventaRepository;
    private final RestTemplate restTemplate;

    @Value("${apisunat.token:}")
    private String apisunatToken;

    @Value("${facturacion.proveedor:nubefact}")
    private String proveedorFacturacion;

    @Value("${nubefact.url:}")
    private String nubefactUrl;

    @Value("${nubefact.token:}")
    private String nubefactToken;

    @Value("${nubefact.authorization-scheme:Token}")
    private String nubefactAuthorizationScheme;

    @Value("${nubefact.serie.factura:F001}")
    private String nubefactSerieFactura;

    @Value("${nubefact.serie.boleta:B001}")
    private String nubefactSerieBoleta;

    @Value("${apisunat.base-url:https://sandbox.apisunat.pe}")
    private String apisunatBaseUrl;

    @Value("${apisunat.endpoint.factura:/api/v3/documents}")
    private String apisunatEndpointFactura;

    @Value("${apisunat.endpoint.boleta:/api/v3/documents}")
    private String apisunatEndpointBoleta;

    @Value("${apisunat.endpoint.factura.candidates:/api/v3/documents,/v3/documents,/api/v1/persona/facturas,/v1/persona/facturas}")
    private String apisunatEndpointFacturaCandidates;

    @Value("${apisunat.endpoint.boleta.candidates:/api/v3/documents,/v3/documents,/api/v1/persona/boletas,/v1/persona/boletas}")
    private String apisunatEndpointBoletaCandidates;

    @Value("${apisunat.ruc:}")
    private String ruc;

    @Value("${apisunat.razon-social:}")
    private String razonSocial;

    @Value("${apisunat.direccion:LIMA}")
    private String direccion;

    @Value("${apisunat.ubigeo:150101}")
    private String ubigeo;

    @Value("${apisunat.serie-boleta:B001}")
    private String serieBoleta;

    @Value("${apisunat.serie-factura:F001}")
    private String serieFactura;

    @Transactional(readOnly = true)
    public List<Comprobante> listarTodos() {
        return comprobanteRepository.findAllByOrderByFechaEmisionDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Comprobante> buscarPorVenta(Long ventaId) {
        return comprobanteRepository.findByVenta_Id(ventaId);
    }

    @Transactional(readOnly = true)
    public Optional<Comprobante> buscarPorId(Long id) {
        return comprobanteRepository.findById(id);
    }

    public Comprobante emitir(EmitirComprobanteDTO dto) {
        Venta venta = validarYObtenerVenta(dto);
        validarDatosComprador(dto);
        limpiarComprobantesPrevios(dto.getVentaId());

        String serie = resolverSerie(dto.getTipoComprobante());
        Integer numero = obtenerSiguienteNumero(serie);
        Comprobante comprobante = crearComprobanteBase(dto, venta, serie, numero);

        try {
            procesarEmisionPorProveedor(comprobante, dto, venta, serie, numero);
        } catch (RuntimeException e) {
            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError("No se pudo emitir el comprobante con el proveedor externo: " + e.getMessage());
        }

        return comprobanteRepository.save(comprobante);
    }

    private Venta validarYObtenerVenta(EmitirComprobanteDTO dto) {
        if (dto.getVentaId() == null) {
            throw new IllegalArgumentException("Debe indicar la venta");
        }

        Venta venta = ventaRepository.findById(dto.getVentaId())
                .orElseThrow(() -> new NoSuchElementException("Venta no encontrada: " + dto.getVentaId()));

        if (venta.getEstado() != Venta.EstadoVenta.COMPLETADA) {
            throw new IllegalStateException("Solo se pueden facturar ventas COMPLETADAS");
        }

        return venta;
    }

    private void limpiarComprobantesPrevios(Long ventaId) {
        List<Comprobante> comprobantesPrevios = comprobanteRepository.findAllByVenta_IdOrderByFechaEmisionDesc(ventaId);
        if (comprobantesPrevios.isEmpty()) {
            return;
        }

        boolean tieneNoError = comprobantesPrevios.stream()
            .anyMatch(c -> c.getEstado() != null && !ESTADO_ERROR.equalsIgnoreCase(c.getEstado()));

        if (tieneNoError) {
            throw new IllegalStateException("Esta venta ya tiene un comprobante emitido");
        }

        comprobanteRepository.deleteAll(comprobantesPrevios);
    }

    private Integer obtenerSiguienteNumero(String serie) {
        Integer ultimoNumero = comprobanteRepository.findMaxNumeroBySerie(serie);
        return (ultimoNumero == null ? 0 : ultimoNumero) + 1;
    }

    private Comprobante crearComprobanteBase(EmitirComprobanteDTO dto, Venta venta, String serie, Integer numero) {
        Comprobante comprobante = new Comprobante();
        comprobante.setTipoComprobante(dto.getTipoComprobante());
        comprobante.setSerie(serie);
        comprobante.setNumero(numero);
        comprobante.setNumeroCompleto(serie + "-" + String.format("%08d", numero));
        comprobante.setVenta(venta);
        comprobante.setTipoDocComprador(dto.getTipoDocComprador() != null ? dto.getTipoDocComprador() : "0");
        comprobante.setNumDocComprador(dto.getNumDocComprador() != null ? dto.getNumDocComprador() : "-");
        comprobante.setRazonSocialComprador(
            dto.getRazonSocialComprador() != null ? dto.getRazonSocialComprador() : RAZON_SOCIAL_CONSUMIDOR_FINAL);
        comprobante.setTotal(venta.getTotal());
        comprobante.setFechaEmision(LocalDateTime.now());
        return comprobante;
    }

    private void procesarEmisionPorProveedor(Comprobante comprobante,
                                             EmitirComprobanteDTO dto,
                                             Venta venta,
                                             String serie,
                                             Integer numero) {
        if ("nubefact".equalsIgnoreCase(proveedorFacturacion)) {
            emitirConNubefact(comprobante, dto, venta, serie, numero);
            return;
        }

        procesarEmisionApiSunat(comprobante, dto, venta, serie, numero);
    }

    private void procesarEmisionApiSunat(Comprobante comprobante,
                                         EmitirComprobanteDTO dto,
                                         Venta venta,
                                         String serie,
                                         Integer numero) {
        if (apisunatToken.isBlank()) {
            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError("Token de APISUNAT no configurado. Configure apisunat.token en application.properties.");
            return;
        }

        HttpEntity<Map<String, Object>> entity = crearEntityApiSunat(dto, venta, serie, numero);
        List<String> endpoints = obtenerEndpointsApiSunat(dto.getTipoComprobante());
        String ultimoDetalleRuta = intentarEmitirEnApiSunat(comprobante, entity, endpoints);

        if (ESTADO_EMITIDO.equalsIgnoreCase(comprobante.getEstado()) || ESTADO_ERROR.equalsIgnoreCase(comprobante.getEstado())) {
            return;
        }

        comprobante.setEstado(ESTADO_ERROR);
        comprobante.setMensajeError(
                "No se encontro una ruta valida de emision en APISUNAT. Revise apisunat.endpoint.factura/boleta. "
                        + (ultimoDetalleRuta != null ? "Detalle: " + ultimoDetalleRuta : "")
        );
    }

    private HttpEntity<Map<String, Object>> crearEntityApiSunat(EmitirComprobanteDTO dto,
                                                                 Venta venta,
                                                                 String serie,
                                                                 Integer numero) {
        Map<String, Object> request = construirRequestApiSunat(dto, venta, serie, numero);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apisunatToken);
        return new HttpEntity<>(request, headers);
    }

    private List<String> obtenerEndpointsApiSunat(String tipoComprobante) {
        return TIPO_COMPROBANTE_BOLETA.equals(tipoComprobante)
                ? construirEndpoints(apisunatBaseUrl, apisunatEndpointBoleta, apisunatEndpointBoletaCandidates)
                : construirEndpoints(apisunatBaseUrl, apisunatEndpointFactura, apisunatEndpointFacturaCandidates);
    }

    private String intentarEmitirEnApiSunat(Comprobante comprobante,
                                            HttpEntity<Map<String, Object>> entity,
                                            List<String> endpoints) {
        String ultimoDetalleRuta = null;
        for (String endpoint : endpoints) {
            ResultadoEndpoint resultado = intentarEmitirEnEndpoint(comprobante, entity, endpoint);
            if (resultado.finalizado()) {
                return resultado.detalleRuta();
            }
            if (resultado.detalleRuta() != null) {
                ultimoDetalleRuta = resultado.detalleRuta();
            }
        }
        return ultimoDetalleRuta;
    }

    private ResultadoEndpoint intentarEmitirEnEndpoint(Comprobante comprobante,
                                                       HttpEntity<Map<String, Object>> entity,
                                                       String endpoint) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
            String responseBody = response.getBody() != null ? response.getBody() : "{}";

            if (esHtml(responseBody)) {
                return ResultadoEndpoint.pendiente("Endpoint devolvio HTML: " + endpoint);
            }

            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            String mensajeRaiz = extraerMensajeProveedor(root);
            if (esMensajeRutaNoExiste(mensajeRaiz)) {
                return ResultadoEndpoint.pendiente("Ruta no encontrada: " + endpoint);
            }

            aplicarResultadoApiSunat(comprobante, root, mensajeRaiz);
            return ResultadoEndpoint.completado();
        } catch (JsonProcessingException ex) {
            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError("Respuesta JSON inválida del proveedor externo");
            return ResultadoEndpoint.completado();
        } catch (HttpStatusCodeException ex) {
            String detalle = extraerMensajeProveedor(ex.getResponseBodyAsString());
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND || esMensajeRutaNoExiste(detalle)) {
                return ResultadoEndpoint.pendiente("Ruta no encontrada: " + endpoint);
            }

            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError(detalle != null ? detalle : "Error del proveedor externo: " + ex.getStatusCode());
            return ResultadoEndpoint.completado();
        }
    }

    private void aplicarResultadoApiSunat(Comprobante comprobante, JsonNode root, String mensajeRaiz) {
        // APISUNAT puede devolver errores dentro del body con HTTP 200
        if (root.has(JSON_FIELD_ERRORS) && !root.get(JSON_FIELD_ERRORS).isEmpty()) {
            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError(mensajeRaiz);
            return;
        }

        JsonNode data = root.path("data");
        comprobante.setEstado(ESTADO_EMITIDO);
        String linkPdf = firstNonBlank(data.path("linkPdf").asText(null), data.path("link_pdf").asText(null));
        String linkXml = firstNonBlank(data.path("linkXml").asText(null), data.path("link_xml").asText(null));
        String linkCdr = firstNonBlank(data.path("linkCdr").asText(null), data.path("link_cdr").asText(null));
        String hash = data.path("codigoHash").asText(null);

        comprobante.setLinkPdf(linkPdf);
        comprobante.setLinkXml(linkXml);
        comprobante.setLinkCdr(linkCdr);
        comprobante.setCodigoHash(hash);

        // Respuesta exitosa sin enlaces ni hash suele indicar rechazo silencioso.
        if ((linkPdf == null || linkPdf.isBlank())
                && (linkXml == null || linkXml.isBlank())
                && (linkCdr == null || linkCdr.isBlank())
                && (hash == null || hash.isBlank())) {
            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError("Proveedor externo respondio sin datos del comprobante emitido.");
        }
    }

    private record ResultadoEndpoint(boolean finalizado, String detalleRuta) {
        private static ResultadoEndpoint completado() {
            return new ResultadoEndpoint(true, null);
        }

        private static ResultadoEndpoint pendiente(String detalleRuta) {
            return new ResultadoEndpoint(false, detalleRuta);
        }
    }

    private void validarDatosComprador(EmitirComprobanteDTO dto) {
        String tipoComprobante = dto.getTipoComprobante() != null ? dto.getTipoComprobante().trim().toUpperCase(Locale.ROOT) : "";
        String tipoDoc = dto.getTipoDocComprador() != null ? dto.getTipoDocComprador().trim() : "0";
        String numDoc = dto.getNumDocComprador() != null ? dto.getNumDocComprador().trim() : "";

        if ("FACTURA".equals(tipoComprobante)) {
            if (!"6".equals(tipoDoc)) {
                throw new IllegalArgumentException("Para FACTURA el tipo de documento del comprador debe ser RUC (6)");
            }
            if (!esRucValido(numDoc)) {
                throw new IllegalArgumentException("RUC del comprador invalido. Debe tener 11 digitos y digito verificador correcto.");
            }
            return;
        }

        if ("1".equals(tipoDoc) && !esDniValido(numDoc)) {
            throw new IllegalArgumentException("DNI del comprador invalido. Debe tener 8 digitos.");
        }
    }

    // ---------------------------------------------------------------

    private Map<String, Object> construirRequestApiSunat(EmitirComprobanteDTO dto, Venta venta,
                                                         String serie, Integer numero) {
        Map<String, Object> req = new LinkedHashMap<>();

        // Credenciales
        Map<String, String> credentials = new HashMap<>();
        credentials.put("token", apisunatToken);
        req.put("credentials", credentials);

        LocalDateTime now = LocalDateTime.now();
        req.put("serie", serie);
        req.put("numero", numero.toString());
        req.put("tipo_emision", "01");
        req.put("fecha_emision", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        req.put("hora_emision", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        // Vendedor / emisor
        req.put("tipo_vendedor_doc", "6");
        req.put("num_doc_vendedor", ruc);
        req.put("rzn_soc_vendedor", razonSocial);
        req.put("ubg_vendedor", ubigeo);
        req.put("dir_vendedor", direccion);

        // Comprador
        String tipoDoc = dto.getTipoDocComprador() != null ? dto.getTipoDocComprador() : "0";
        String numDoc  = dto.getNumDocComprador()   != null ? dto.getNumDocComprador()  : "-";
        String razon   = dto.getRazonSocialComprador() != null ? dto.getRazonSocialComprador() : RAZON_SOCIAL_CONSUMIDOR_FINAL;
        req.put("tipo_comprador_doc", tipoDoc);
        req.put("num_doc_comprador",  numDoc);
        req.put("rzn_soc_comprador",  razon);

        req.put("tipo_moneda", "PEN");
        req.put("observaciones", "");
        req.put("igv", 18);

        // Ítems (detalle de la venta)
        List<Map<String, Object>> items = new ArrayList<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            Map<String, Object> item = new LinkedHashMap<>();
            BigDecimal precioConIgv  = detalle.getPrecioUnitario();
            BigDecimal valorSinIgv   = precioConIgv.divide(BigDecimal.valueOf(1.18), 10, RoundingMode.HALF_UP);

            item.put("unidad_medida", "NIU");
            item.put("codigo", detalle.getProducto().getCodigo() != null
                    ? detalle.getProducto().getCodigo() : "PROD");
            item.put("descripcion", detalle.getProducto().getNombre());
            item.put("cantidad",    detalle.getCantidad());
            item.put("mto_valor_unitario",  valorSinIgv.setScale(6, RoundingMode.HALF_UP));
            item.put("mto_precio_unitario", precioConIgv.setScale(2, RoundingMode.HALF_UP));
            items.add(item);
        }
        req.put("items", items);

        return req;
    }

    private void emitirConNubefact(Comprobante comprobante,
                                   EmitirComprobanteDTO dto,
                                   Venta venta,
                                   String serie,
                                   Integer numero) {
        if (nubefactUrl == null || nubefactUrl.isBlank()) {
            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError("Nubefact no configurado: falta nubefact.url");
            return;
        }
        if (nubefactToken == null || nubefactToken.isBlank()) {
            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError("Nubefact no configurado: falta nubefact.token");
            return;
        }

        Map<String, Object> request = construirRequestNubefact(dto, venta, serie, numero);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        setNubefactAuthorization(headers, nubefactToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(nubefactUrl, entity, String.class);
        String responseBody = response.getBody() != null ? response.getBody() : "{}";

        if (esHtml(responseBody)) {
            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError("Nubefact devolvio HTML. Verifique la RUTA (nubefact.url).");
            return;
        }

        JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(responseBody);
        } catch (JsonProcessingException ex) {
            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError("Respuesta JSON inválida de Nubefact");
            return;
        }
        String detalle = extraerMensajeProveedor(root);

        if (root.has(JSON_FIELD_ERRORS) && !root.get(JSON_FIELD_ERRORS).isEmpty()) {
            comprobante.setEstado(ESTADO_ERROR);
            comprobante.setMensajeError(detalle != null ? detalle : "Nubefact rechazo la emision");
            return;
        }

        comprobante.setEstado(ESTADO_EMITIDO);

        String linkPdf = firstNonBlank(
                root.path("enlace_del_pdf").asText(null),
                root.path("pdf").asText(null),
                root.path("enlace").asText(null)
        );
        String linkXml = firstNonBlank(root.path("enlace_del_xml").asText(null), root.path("xml").asText(null));
        String linkCdr = firstNonBlank(root.path("enlace_del_cdr").asText(null), root.path("cdr").asText(null));
        String hash = firstNonBlank(
                root.path("codigo_hash").asText(null),
                root.path("hash").asText(null),
                root.path("cadena_para_codigo_qr").asText(null)
        );

        comprobante.setLinkPdf(linkPdf);
        comprobante.setLinkXml(linkXml);
        comprobante.setLinkCdr(linkCdr);
        comprobante.setCodigoHash(hash);

        String sunatDescription = root.path("sunat_description").asText(null);
        actualizarEstadoDesdeDescripcionSunat(comprobante, sunatDescription);
    }

    private void actualizarEstadoDesdeDescripcionSunat(Comprobante comprobante, String sunatDescription) {
        if (sunatDescription == null || sunatDescription.isBlank()) {
            return;
        }

        if (esSunatAceptada(sunatDescription)) {
            // "ACEPTADA" y "ACEPTADA CON OBSERVACIONES" son válidas.
            comprobante.setEstado(ESTADO_EMITIDO);
            comprobante.setMensajeError(null);
            return;
        }

        comprobante.setEstado(ESTADO_ERROR);
        comprobante.setMensajeError(sunatDescription);
    }

    private Map<String, Object> construirRequestNubefact(EmitirComprobanteDTO dto,
                                                          Venta venta,
                                                          String serie,
                                                          Integer numero) {
        Map<String, Object> req = new LinkedHashMap<>();

        String tipoComprobante = TIPO_COMPROBANTE_BOLETA.equalsIgnoreCase(dto.getTipoComprobante()) ? "2" : "1";
        String tipoDoc = dto.getTipoDocComprador() != null ? dto.getTipoDocComprador() : "0";
        String numDoc = dto.getNumDocComprador() != null ? dto.getNumDocComprador() : "-";
        String razon = dto.getRazonSocialComprador() != null ? dto.getRazonSocialComprador() : RAZON_SOCIAL_CONSUMIDOR_FINAL;

        TotalesNubefact totales = construirTotalesNubefact(venta);

        req.put("operacion", "generar_comprobante");
        req.put("tipo_de_comprobante", tipoComprobante);
        req.put("serie", serie);
        req.put("numero", numero.toString());
        req.put("sunat_transaction", "1");
        req.put("cliente_tipo_de_documento", tipoDoc);
        req.put("cliente_numero_de_documento", numDoc);
        req.put("cliente_denominacion", razon);
        req.put("cliente_direccion", venta.getCliente() != null && venta.getCliente().getDireccion() != null ? venta.getCliente().getDireccion() : "-");
        req.put("cliente_email", venta.getCliente() != null && venta.getCliente().getEmail() != null ? venta.getCliente().getEmail() : "");
        req.put("fecha_de_emision", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        req.put("moneda", "1");
        req.put("porcentaje_de_igv", "18.00");
        req.put("descuento_global", totales.descuento().setScale(2, RoundingMode.HALF_UP).toPlainString());
        req.put("total_descuento", totales.descuento().setScale(2, RoundingMode.HALF_UP).toPlainString());
        req.put("total_gravada", totales.subtotal().setScale(2, RoundingMode.HALF_UP).toPlainString());
        req.put("total_igv", totales.igv().setScale(2, RoundingMode.HALF_UP).toPlainString());
        req.put("total", totales.total().setScale(2, RoundingMode.HALF_UP).toPlainString());
        req.put("detraccion", false);
        req.put("enviar_automaticamente_a_la_sunat", true);
        req.put("enviar_automaticamente_al_cliente", false);
        req.put("codigo_unico", venta.getNumeroVenta() != null ? venta.getNumeroVenta() : "");
        req.put("formato_de_pdf", "A4");

        req.put("items", construirItemsNubefact(venta));

        return req;
    }

    private TotalesNubefact construirTotalesNubefact(Venta venta) {
        BigDecimal subtotal = venta.getSubtotal() != null ? venta.getSubtotal() : BigDecimal.ZERO;
        BigDecimal igv = venta.getImpuesto() != null ? venta.getImpuesto() : BigDecimal.ZERO;
        BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
        BigDecimal descuento = venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO;
        return new TotalesNubefact(subtotal, igv, total, descuento);
    }

    private List<Map<String, Object>> construirItemsNubefact(Venta venta) {
        List<DetalleVenta> detalles = venta.getDetalles() != null ? venta.getDetalles() : List.of();
        return detalles.stream().map(this::construirItemNubefact).toList();
    }

    private Map<String, Object> construirItemNubefact(DetalleVenta detalle) {
        Map<String, Object> item = new LinkedHashMap<>();

        // En PharmaSys el precio unitario se maneja como valor base (sin IGV).
        BigDecimal valorUnitario = detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : BigDecimal.ZERO;
        BigDecimal precioUnitario = valorUnitario.multiply(BigDecimal.valueOf(1.18));
        BigDecimal cantidad = BigDecimal.valueOf(detalle.getCantidad() != null ? detalle.getCantidad() : 0);
        BigDecimal descuentoItem = detalle.getDescuento() != null ? detalle.getDescuento() : BigDecimal.ZERO;
        BigDecimal subtotalItem = valorUnitario.multiply(cantidad).subtract(descuentoItem).setScale(2, RoundingMode.HALF_UP);
        BigDecimal igvItem = subtotalItem.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalItem = calcularTotalItemNubefact(precioUnitario, cantidad, descuentoItem, subtotalItem, igvItem);

        item.put("unidad_de_medida", "NIU");
        item.put("codigo", detalle.getProducto() != null && detalle.getProducto().getCodigo() != null ? detalle.getProducto().getCodigo() : "PROD");
        item.put("descripcion", detalle.getProducto() != null && detalle.getProducto().getNombre() != null ? detalle.getProducto().getNombre() : "Producto");
        item.put("cantidad", cantidad.toPlainString());
        item.put("valor_unitario", valorUnitario.setScale(6, RoundingMode.HALF_UP).toPlainString());
        item.put("precio_unitario", precioUnitario.setScale(2, RoundingMode.HALF_UP).toPlainString());
        item.put("descuento", descuentoItem.setScale(2, RoundingMode.HALF_UP).toPlainString());
        item.put("subtotal", subtotalItem.toPlainString());
        item.put("tipo_de_igv", "1");
        item.put("igv", igvItem.toPlainString());
        item.put("total", totalItem.toPlainString());
        item.put("anticipo_regularizacion", false);
        item.put("anticipo_documento_serie", "");
        item.put("anticipo_documento_numero", "");

        return item;
    }

    private BigDecimal calcularTotalItemNubefact(BigDecimal precioUnitario,
                                                 BigDecimal cantidad,
                                                 BigDecimal descuentoItem,
                                                 BigDecimal subtotalItem,
                                                 BigDecimal igvItem) {
        if (descuentoItem.compareTo(BigDecimal.ZERO) > 0) {
            return subtotalItem.add(igvItem).setScale(2, RoundingMode.HALF_UP);
        }
        return precioUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
    }

    private record TotalesNubefact(BigDecimal subtotal, BigDecimal igv, BigDecimal total, BigDecimal descuento) {
    }

    private void setNubefactAuthorization(HttpHeaders headers, String token) {
        if ("Bearer".equalsIgnoreCase(nubefactAuthorizationScheme)) {
            headers.setBearerAuth(token);
            return;
        }
        headers.set("Authorization", "Token token=" + token);
    }

    private String extraerMensajeProveedor(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }

        String lowered = rawBody.toLowerCase(Locale.ROOT);
        if (lowered.contains("<!doctype html") || lowered.contains("<html")) {
            return "APISUNAT devolvio una pagina HTML (404). Verifique apisunat.base-url y rutas de emision (factura/boleta) en application.properties.";
        }

        try {
            JsonNode root = OBJECT_MAPPER.readTree(rawBody);
            return extraerMensajeProveedor(root);
        } catch (JsonProcessingException ignored) {
            return rawBody;
        }
    }

    private String extraerMensajeProveedor(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return "No se pudo emitir el comprobante. Verifique los datos enviados.";
        }

        String messageFromErrors = extraerMensajeDesdeErrors(root.path(JSON_FIELD_ERRORS));
        if (messageFromErrors != null) {
            return messageFromErrors;
        }

        String message = normalizarMensajeRuta(root.path(JSON_FIELD_MESSAGE).asText(null));
        if (message != null) {
            return message;
        }

        String nestedMessage = normalizarMensajeRuta(root.path("error").path(JSON_FIELD_MESSAGE).asText(null));
        if (nestedMessage != null) {
            return nestedMessage;
        }

        String providerMessage = firstNonBlank(
            blankToNull(root.path("data").path(JSON_FIELD_MESSAGE).asText(null)),
                blankToNull(root.path("sunat_description").asText(null))
        );
        if (providerMessage != null) {
            return providerMessage;
        }

        return "No se pudo emitir el comprobante. Verifique los datos enviados.";
    }

    private String extraerMensajeDesdeErrors(JsonNode errors) {
        if (errors == null || errors.isMissingNode() || errors.isNull()) {
            return null;
        }

        if (errors.isTextual()) {
            return blankToNull(errors.asText(null));
        }

        if (errors.isObject()) {
            return firstNonBlank(
                    blankToNull(errors.path(JSON_FIELD_MESSAGE).asText(null)),
                    blankToNull(errors.toString())
            );
        }

        if (errors.isArray() && !errors.isEmpty()) {
            JsonNode first = errors.get(0);
            return firstNonBlank(
                    blankToNull(first.path(JSON_FIELD_MESSAGE).asText(null)),
                    blankToNull(first.path("error").asText(null)),
                    blankToNull(first.asText(null))
            );
        }

        return null;
    }

    private String normalizarMensajeRuta(String message) {
        String cleanMessage = blankToNull(message);
        if (cleanMessage == null) {
            return null;
        }

        if (esMensajeRutaNoExiste(cleanMessage)) {
            return "La ruta configurada en APISUNAT no existe: " + cleanMessage;
        }
        return cleanMessage;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String construirEndpoint(String baseUrl, String path) {
        String normalizedBase = baseUrl != null ? baseUrl.trim() : "";
        String normalizedPath = path != null ? path.trim() : "";

        if (normalizedBase.endsWith(URL_PATH_SEPARATOR)) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        if (!normalizedPath.startsWith(URL_PATH_SEPARATOR)) {
            normalizedPath = URL_PATH_SEPARATOR + normalizedPath;
        }

        return normalizedBase + normalizedPath;
    }

    private List<String> construirEndpoints(String baseUrl, String principalPath, String candidatesCsv) {
        LinkedHashSet<String> endpoints = new LinkedHashSet<>();
        endpoints.add(construirEndpoint(baseUrl, principalPath));

        if (candidatesCsv != null && !candidatesCsv.isBlank()) {
            for (String path : candidatesCsv.split(",")) {
                String trimmed = path != null ? path.trim() : "";
                if (!trimmed.isBlank()) {
                    endpoints.add(construirEndpoint(baseUrl, trimmed));
                }
            }
        }

        return new ArrayList<>(endpoints);
    }

    private boolean esMensajeRutaNoExiste(String mensaje) {
        if (mensaje == null) {
            return false;
        }
        String lower = mensaje.toLowerCase(Locale.ROOT);
        return lower.contains("there is no method to handle post")
                || lower.contains("404")
                || lower.contains("ruta") && lower.contains("no existe");
    }

    private boolean esHtml(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }
        String lowered = body.toLowerCase(Locale.ROOT);
        return lowered.contains("<!doctype html") || lowered.contains("<html");
    }

    private boolean esSunatAceptada(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return false;
        }

        String normalizada = descripcion.toLowerCase(Locale.ROOT);
        return normalizada.contains("aceptada") || normalizada.contains("aceptado");
    }

    private boolean esDniValido(String dni) {
        return dni != null && dni.matches("\\d{8}");
    }

    private boolean esRucValido(String ruc) {
        if (ruc == null || !ruc.matches("\\d{11}")) {
            return false;
        }

        int[] pesos = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += Character.getNumericValue(ruc.charAt(i)) * pesos[i];
        }

        int resto = 11 - (suma % 11);
        int dvEsperado = switch (resto) {
            case 10 -> 0;
            case 11 -> 1;
            default -> resto;
        };

        int dvReal = Character.getNumericValue(ruc.charAt(10));
        return dvReal == dvEsperado;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String resolverSerie(String tipoComprobante) {
        boolean esBoleta = TIPO_COMPROBANTE_BOLETA.equalsIgnoreCase(tipoComprobante);

        if ("nubefact".equalsIgnoreCase(proveedorFacturacion)) {
            return esBoleta ? nubefactSerieBoleta : nubefactSerieFactura;
        }

        return esBoleta ? serieBoleta : serieFactura;
    }
}
