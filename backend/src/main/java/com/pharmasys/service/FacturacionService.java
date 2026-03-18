package com.pharmasys.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        if (dto.getVentaId() == null) {
            throw new RuntimeException("Debe indicar la venta");
        }

        Venta venta = ventaRepository.findById(dto.getVentaId())
                .orElseThrow(() -> new RuntimeException("Venta no encontrada: " + dto.getVentaId()));

        if (venta.getEstado() != Venta.EstadoVenta.COMPLETADA) {
            throw new RuntimeException("Solo se pueden facturar ventas COMPLETADAS");
        }

        validarDatosComprador(dto);

        // Verificar comprobantes previos de la venta.
        // Los comprobantes en ERROR se depuran para permitir reemision.
        List<Comprobante> comprobantesPrevios = comprobanteRepository.findAllByVenta_IdOrderByFechaEmisionDesc(dto.getVentaId());
        if (!comprobantesPrevios.isEmpty()) {
            boolean tieneNoError = comprobantesPrevios.stream()
                    .anyMatch(c -> c.getEstado() != null && !"ERROR".equalsIgnoreCase(c.getEstado()));

            if (tieneNoError) {
                throw new RuntimeException("Esta venta ya tiene un comprobante emitido");
            }

            comprobanteRepository.deleteAll(comprobantesPrevios);
        }

        String serie = resolverSerie(dto.getTipoComprobante());
        Integer ultimoNumero = comprobanteRepository.findMaxNumeroBySerie(serie);
        if (ultimoNumero == null) {
            ultimoNumero = 0;
        }
        Integer nuevoNumero = ultimoNumero + 1;

        Comprobante comprobante = new Comprobante();
        comprobante.setTipoComprobante(dto.getTipoComprobante());
        comprobante.setSerie(serie);
        comprobante.setNumero(nuevoNumero);
        comprobante.setNumeroCompleto(serie + "-" + String.format("%08d", nuevoNumero));
        comprobante.setVenta(venta);
        comprobante.setTipoDocComprador(dto.getTipoDocComprador() != null ? dto.getTipoDocComprador() : "0");
        comprobante.setNumDocComprador(dto.getNumDocComprador() != null ? dto.getNumDocComprador() : "-");
        comprobante.setRazonSocialComprador(
                dto.getRazonSocialComprador() != null ? dto.getRazonSocialComprador() : "CONSUMIDOR FINAL");
        comprobante.setTotal(venta.getTotal());
        comprobante.setFechaEmision(LocalDateTime.now());

        try {
            if ("nubefact".equalsIgnoreCase(proveedorFacturacion)) {
                emitirConNubefact(comprobante, dto, venta, serie, nuevoNumero);
                return comprobanteRepository.save(comprobante);
            }

            // Fallback APISUNAT
            if (apisunatToken.isBlank()) {
                comprobante.setEstado("ERROR");
                comprobante.setMensajeError("Token de APISUNAT no configurado. Configure apisunat.token en application.properties.");
                return comprobanteRepository.save(comprobante);
            }

            Map<String, Object> request = construirRequestApiSunat(dto, venta, serie, nuevoNumero);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apisunatToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            List<String> endpoints = "BOLETA".equals(dto.getTipoComprobante())
                    ? construirEndpoints(apisunatBaseUrl, apisunatEndpointBoleta, apisunatEndpointBoletaCandidates)
                    : construirEndpoints(apisunatBaseUrl, apisunatEndpointFactura, apisunatEndpointFacturaCandidates);

            String ultimoDetalleRuta = null;
            for (String endpoint : endpoints) {
                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
                    String responseBody = response.getBody() != null ? response.getBody() : "{}";

                    if (esHtml(responseBody)) {
                        ultimoDetalleRuta = "Endpoint devolvio HTML: " + endpoint;
                        continue;
                    }

                    JsonNode root = OBJECT_MAPPER.readTree(responseBody);
                    String mensajeRaiz = extraerMensajeProveedor(root);
                    if (esMensajeRutaNoExiste(mensajeRaiz)) {
                        ultimoDetalleRuta = "Ruta no encontrada: " + endpoint;
                        continue;
                    }

                    // APISUNAT puede devolver errores dentro del body con HTTP 200
                    if (root.has("errors") && !root.get("errors").isEmpty()) {
                        comprobante.setEstado("ERROR");
                        comprobante.setMensajeError(mensajeRaiz);
                    } else {
                        JsonNode data = root.path("data");
                        comprobante.setEstado("EMITIDO");
                        String linkPdf = data.path("linkPdf").asText(null);
                        String linkXml = data.path("linkXml").asText(null);
                        String linkCdr = data.path("linkCdr").asText(null);
                        String hash = data.path("codigoHash").asText(null);
                        // APISUNAT puede usar camelCase o snake_case según versión
                        if (linkPdf == null || linkPdf.isEmpty()) linkPdf = data.path("link_pdf").asText(null);
                        if (linkXml == null || linkXml.isEmpty()) linkXml = data.path("link_xml").asText(null);
                        if (linkCdr == null || linkCdr.isEmpty()) linkCdr = data.path("link_cdr").asText(null);
                        comprobante.setLinkPdf(linkPdf);
                        comprobante.setLinkXml(linkXml);
                        comprobante.setLinkCdr(linkCdr);
                        comprobante.setCodigoHash(hash);

                        // Respuesta exitosa sin enlaces ni hash suele indicar rechazo silencioso.
                        if ((linkPdf == null || linkPdf.isBlank())
                                && (linkXml == null || linkXml.isBlank())
                                && (linkCdr == null || linkCdr.isBlank())
                                && (hash == null || hash.isBlank())) {
                            comprobante.setEstado("ERROR");
                            comprobante.setMensajeError("Proveedor externo respondio sin datos del comprobante emitido.");
                        }
                    }

                    return comprobanteRepository.save(comprobante);
                } catch (HttpStatusCodeException ex) {
                    String detalle = extraerMensajeProveedor(ex.getResponseBodyAsString());
                    if (ex.getStatusCode() == HttpStatus.NOT_FOUND || esMensajeRutaNoExiste(detalle)) {
                        ultimoDetalleRuta = "Ruta no encontrada: " + endpoint;
                        continue;
                    }

                    comprobante.setEstado("ERROR");
                    comprobante.setMensajeError(detalle != null ? detalle : "Error del proveedor externo: " + ex.getStatusCode());
                    return comprobanteRepository.save(comprobante);
                }
            }

            comprobante.setEstado("ERROR");
            comprobante.setMensajeError(
                    "No se encontro una ruta valida de emision en APISUNAT. Revise apisunat.endpoint.factura/boleta. "
                            + (ultimoDetalleRuta != null ? "Detalle: " + ultimoDetalleRuta : "")
            );
        } catch (Exception e) {
            comprobante.setEstado("ERROR");
            comprobante.setMensajeError("No se pudo emitir el comprobante con el proveedor externo: " + e.getMessage());
        }

        return comprobanteRepository.save(comprobante);
    }

    private void validarDatosComprador(EmitirComprobanteDTO dto) {
        String tipoComprobante = dto.getTipoComprobante() != null ? dto.getTipoComprobante().trim().toUpperCase(Locale.ROOT) : "";
        String tipoDoc = dto.getTipoDocComprador() != null ? dto.getTipoDocComprador().trim() : "0";
        String numDoc = dto.getNumDocComprador() != null ? dto.getNumDocComprador().trim() : "";

        if ("FACTURA".equals(tipoComprobante)) {
            if (!"6".equals(tipoDoc)) {
                throw new RuntimeException("Para FACTURA el tipo de documento del comprador debe ser RUC (6)");
            }
            if (!esRucValido(numDoc)) {
                throw new RuntimeException("RUC del comprador invalido. Debe tener 11 digitos y digito verificador correcto.");
            }
            return;
        }

        if ("1".equals(tipoDoc) && !esDniValido(numDoc)) {
            throw new RuntimeException("DNI del comprador invalido. Debe tener 8 digitos.");
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
        String razon   = dto.getRazonSocialComprador() != null ? dto.getRazonSocialComprador() : "CONSUMIDOR FINAL";
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
                                   Integer numero) throws Exception {
        if (nubefactUrl == null || nubefactUrl.isBlank()) {
            comprobante.setEstado("ERROR");
            comprobante.setMensajeError("Nubefact no configurado: falta nubefact.url");
            return;
        }
        if (nubefactToken == null || nubefactToken.isBlank()) {
            comprobante.setEstado("ERROR");
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
            comprobante.setEstado("ERROR");
            comprobante.setMensajeError("Nubefact devolvio HTML. Verifique la RUTA (nubefact.url).");
            return;
        }

        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        String detalle = extraerMensajeProveedor(root);

        if (root.has("errors") && !root.get("errors").isEmpty()) {
            comprobante.setEstado("ERROR");
            comprobante.setMensajeError(detalle != null ? detalle : "Nubefact rechazo la emision");
            return;
        }

        comprobante.setEstado("EMITIDO");

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
        if (sunatDescription != null && !sunatDescription.isBlank()) {
            if (esSunatAceptada(sunatDescription)) {
                // "ACEPTADA" y "ACEPTADA CON OBSERVACIONES" son válidas.
                comprobante.setEstado("EMITIDO");
                comprobante.setMensajeError(null);
            } else {
                comprobante.setEstado("ERROR");
                comprobante.setMensajeError(sunatDescription);
            }
        }
    }

    private Map<String, Object> construirRequestNubefact(EmitirComprobanteDTO dto,
                                                          Venta venta,
                                                          String serie,
                                                          Integer numero) {
        Map<String, Object> req = new LinkedHashMap<>();

        String tipoComprobante = "BOLETA".equalsIgnoreCase(dto.getTipoComprobante()) ? "2" : "1";
        String tipoDoc = dto.getTipoDocComprador() != null ? dto.getTipoDocComprador() : "0";
        String numDoc = dto.getNumDocComprador() != null ? dto.getNumDocComprador() : "-";
        String razon = dto.getRazonSocialComprador() != null ? dto.getRazonSocialComprador() : "CONSUMIDOR FINAL";

        BigDecimal subtotal = venta.getSubtotal() != null ? venta.getSubtotal() : BigDecimal.ZERO;
        BigDecimal igv = venta.getImpuesto() != null ? venta.getImpuesto() : BigDecimal.ZERO;
        BigDecimal total = venta.getTotal() != null ? venta.getTotal() : BigDecimal.ZERO;
        BigDecimal descuento = venta.getDescuento() != null ? venta.getDescuento() : BigDecimal.ZERO;

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
        req.put("descuento_global", descuento.setScale(2, RoundingMode.HALF_UP).toPlainString());
        req.put("total_descuento", descuento.setScale(2, RoundingMode.HALF_UP).toPlainString());
        req.put("total_gravada", subtotal.setScale(2, RoundingMode.HALF_UP).toPlainString());
        req.put("total_igv", igv.setScale(2, RoundingMode.HALF_UP).toPlainString());
        req.put("total", total.setScale(2, RoundingMode.HALF_UP).toPlainString());
        req.put("detraccion", false);
        req.put("enviar_automaticamente_a_la_sunat", true);
        req.put("enviar_automaticamente_al_cliente", false);
        req.put("codigo_unico", venta.getNumeroVenta() != null ? venta.getNumeroVenta() : "");
        req.put("formato_de_pdf", "A4");

        List<Map<String, Object>> items = new ArrayList<>();
        for (DetalleVenta detalle : venta.getDetalles()) {
            Map<String, Object> item = new LinkedHashMap<>();

            // En PharmaSys el precio unitario se maneja como valor base (sin IGV).
            BigDecimal valorUnitario = detalle.getPrecioUnitario() != null ? detalle.getPrecioUnitario() : BigDecimal.ZERO;
            BigDecimal precioUnitario = valorUnitario.multiply(BigDecimal.valueOf(1.18));
            BigDecimal cantidad = BigDecimal.valueOf(detalle.getCantidad() != null ? detalle.getCantidad() : 0);
            BigDecimal descuentoItem = detalle.getDescuento() != null ? detalle.getDescuento() : BigDecimal.ZERO;
            BigDecimal subtotalItem = valorUnitario.multiply(cantidad).subtract(descuentoItem).setScale(2, RoundingMode.HALF_UP);
            BigDecimal igvItem = subtotalItem.multiply(BigDecimal.valueOf(0.18)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalItem = precioUnitario.multiply(cantidad).setScale(2, RoundingMode.HALF_UP);
            if (descuentoItem.compareTo(BigDecimal.ZERO) > 0) {
                totalItem = subtotalItem.add(igvItem).setScale(2, RoundingMode.HALF_UP);
            }

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

            items.add(item);
        }
        req.put("items", items);

        return req;
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
        } catch (Exception ignored) {
            return rawBody;
        }
    }

    private String extraerMensajeProveedor(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return "No se pudo emitir el comprobante. Verifique los datos enviados.";
        }

        JsonNode errors = root.path("errors");
        if (!errors.isMissingNode() && !errors.isNull()) {
            if (errors.isTextual()) {
                String msg = errors.asText(null);
                if (msg != null && !msg.isBlank()) {
                    return msg;
                }
            }
            if (errors.isObject()) {
                String msg = errors.path("message").asText(null);
                if (msg != null && !msg.isBlank()) {
                    return msg;
                }
                String asText = errors.toString();
                if (asText != null && !asText.isBlank()) {
                    return asText;
                }
            }
        }

        if (errors.isArray() && !errors.isEmpty()) {
            JsonNode first = errors.get(0);
            String msg = first.path("message").asText(null);
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
            msg = first.path("error").asText(null);
            if (msg != null && !msg.isBlank()) {
                return msg;
            }
            String asText = first.asText(null);
            if (asText != null && !asText.isBlank()) {
                return asText;
            }
        }

        String message = root.path("message").asText(null);
        if (message != null && !message.isBlank()) {
            if (esMensajeRutaNoExiste(message)) {
                return "La ruta configurada en APISUNAT no existe: " + message;
            }
            return message;
        }

        JsonNode errorNode = root.path("error");
        String nestedMessage = errorNode.path("message").asText(null);
        if (nestedMessage != null && !nestedMessage.isBlank()) {
            if (esMensajeRutaNoExiste(nestedMessage)) {
                return "La ruta configurada en APISUNAT no existe: " + nestedMessage;
            }
            return nestedMessage;
        }

        JsonNode data = root.path("data");
        String dataMessage = data.path("message").asText(null);
        if (dataMessage != null && !dataMessage.isBlank()) {
            return dataMessage;
        }

        String sunatDescription = root.path("sunat_description").asText(null);
        if (sunatDescription != null && !sunatDescription.isBlank()) {
            return sunatDescription;
        }

        return "No se pudo emitir el comprobante. Verifique los datos enviados.";
    }

    private String construirEndpoint(String baseUrl, String path) {
        String normalizedBase = baseUrl != null ? baseUrl.trim() : "";
        String normalizedPath = path != null ? path.trim() : "";

        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
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
        boolean esBoleta = "BOLETA".equalsIgnoreCase(tipoComprobante);

        if ("nubefact".equalsIgnoreCase(proveedorFacturacion)) {
            return esBoleta ? nubefactSerieBoleta : nubefactSerieFactura;
        }

        return esBoleta ? serieBoleta : serieFactura;
    }
}
