package com.pharmasys.service;

import com.pharmasys.model.Cliente;
import com.pharmasys.model.Venta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppNotificationService {

    private final RestTemplate restTemplate;

    @Value("${whatsapp.bot.enabled:false}")
    private boolean enabled;

    @Value("${whatsapp.bot.base-url:http://localhost:8000}")
    private String botBaseUrl;

    @Value("${whatsapp.bot.api-key:}")
    private String botApiKey;

    @Value("${whatsapp.bot.use-template:true}")
    private boolean useTemplate;

    @Value("${whatsapp.bot.template.name:venta_confirmada}")
    private String templateName;

    @Value("${whatsapp.bot.template.language:es_PE}")
    private String templateLanguage;

    @Value("${whatsapp.bot.app-id:farmacia}")
    private String botAppId;

    public void notificarVentaRealizada(Venta venta) {
        if (!enabled) {
            return;
        }

        if (venta == null || venta.getId() == null) {
            return;
        }

        if (botApiKey == null || botApiKey.isBlank()) {
            log.warn("Notificacion WhatsApp omitida: whatsapp.bot.api-key no configurado");
            return;
        }

        Cliente cliente = venta.getCliente();
        if (cliente == null || cliente.getTelefono() == null || cliente.getTelefono().isBlank()) {
            log.info("Notificacion WhatsApp omitida para venta {}: cliente sin telefono", venta.getNumeroVenta());
            return;
        }

        String telefono = normalizarTelefono(cliente.getTelefono());
        if (telefono.isBlank()) {
            log.warn("Notificacion WhatsApp omitida para venta {}: telefono invalido '{}'", venta.getNumeroVenta(), cliente.getTelefono());
            return;
        }

        String nombreCliente = (cliente.getNombre() != null && !cliente.getNombre().isBlank())
            ? cliente.getNombre().trim()
            : "Cliente";
        String numeroVenta = (venta.getNumeroVenta() != null && !venta.getNumeroVenta().isBlank())
            ? venta.getNumeroVenta()
            : "VENTA";
        String totalTexto = venta.getTotal() != null
            ? venta.getTotal().setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
            : "0.00";

        Map<String, Object> payload = useTemplate
            ? construirPayloadTemplate(telefono, nombreCliente, numeroVenta, totalTexto)
            : construirPayloadTexto(telefono, nombreCliente, numeroVenta, totalTexto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-app-id", botAppId);
        headers.set("x-api-key", botApiKey);
        headers.set("x-idempotency-key", (useTemplate ? "venta-template-" : "venta-") + venta.getId());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            String endpoint = normalizarBaseUrl(botBaseUrl) + (useTemplate ? "/notify/template" : "/notify");
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Notificacion WhatsApp enviada para venta {} al telefono {} (template={})", venta.getNumeroVenta(), telefono, useTemplate);
            } else {
                log.warn("Notificacion WhatsApp respondio {} para venta {} (template={})", response.getStatusCode(), venta.getNumeroVenta(), useTemplate);
            }
        } catch (Exception ex) {
            // No interrumpir la venta por fallas externas de notificacion.
            log.warn("No se pudo enviar WhatsApp para venta {} (template={}): {}", venta.getNumeroVenta(), useTemplate, ex.getMessage());
        }
    }

    private Map<String, Object> construirPayloadTexto(String telefono, String nombreCliente, String numeroVenta, String totalTexto) {
        String mensaje = String.format(
                "Hola %s, tu compra %s por S/ %s fue registrada correctamente. Gracias por preferirnos.",
                nombreCliente,
                numeroVenta,
                totalTexto
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("telefono", telefono);
        payload.put("mensaje", mensaje);
        return payload;
    }

    private Map<String, Object> construirPayloadTemplate(String telefono, String nombreCliente, String numeroVenta, String totalTexto) {
        Map<String, Object> component = new HashMap<>();
        component.put("type", "body");
        component.put("parameters", java.util.List.of(nombreCliente, numeroVenta, totalTexto));

        Map<String, Object> payload = new HashMap<>();
        payload.put("telefono", telefono);
        payload.put("template_name", templateName);
        payload.put("language_code", templateLanguage);
        payload.put("components", java.util.List.of(component));
        return payload;
    }

    private String normalizarBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:8000";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String normalizarTelefono(String telefono) {
        if (telefono == null) {
            return "";
        }
        return telefono.replaceAll("[^0-9]", "");
    }
}
