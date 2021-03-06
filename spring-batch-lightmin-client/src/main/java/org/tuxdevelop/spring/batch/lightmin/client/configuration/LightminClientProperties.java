package org.tuxdevelop.spring.batch.lightmin.client.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "spring.batch.lightmin.client")
public class LightminClientProperties {

    @Setter
    private String managementUrl;
    @Setter
    private String serviceUrl;
    @Setter
    private String healthUrl;
    @Getter
    @Setter
    private boolean preferIp = false;
    @Getter
    @Setter
    private Integer serverPort;
    @Getter
    @Setter
    private Integer managementPort;

    @Getter
    @Setter
    private Map<String, String> externalLinks = new HashMap<>();

    @Getter
    private final String name;
    @Getter
    private final String healthEndpointId;

    private final ManagementServerProperties managementServerProperties;
    private final ServerProperties serverProperties;

    @Autowired
    public LightminClientProperties(final ManagementServerProperties managementServerProperties,
                                    final ServerProperties serverProperties,
                                    @Value("${spring.application.name:spring-boot-application}") final String name,
                                    @Value("${endpoints.health.id:health}") final String healthEndpointId) {
        this.name = name;
        this.healthEndpointId = healthEndpointId;
        this.managementServerProperties = managementServerProperties;
        this.serverProperties = serverProperties;
    }

    public String getManagementUrl() {
        if (managementUrl != null) {
            return managementUrl;
        }

        if ((managementPort == null || managementPort.equals(serverPort))
                && getServiceUrl() != null) {
            return append(getServiceUrl(), managementServerProperties.getContextPath());
        }

        if (managementPort == null) {
            throw new IllegalStateException(
                    "serviceUrl must be set when deployed to servlet-container");
        }

        if (preferIp) {
            InetAddress address = managementServerProperties.getAddress();
            if (address == null) {
                address = getHostAddress();
            }
            return append(append(createLocalUri(address.getHostAddress(), managementPort),
                    serverProperties.getContextPath()), managementServerProperties.getContextPath());

        }
        final String managementUrl = append(createLocalUri(getHostAddress().getCanonicalHostName(), managementPort),
                managementServerProperties.getContextPath());
        return managementUrl;
    }

    public String getHealthUrl() {
        if (healthUrl != null) {
            return healthUrl;
        }
        final String managementUrl = getManagementUrl();
        return append(managementUrl, healthEndpointId);
    }

    public String getServiceUrl() {
        if (serviceUrl != null) {
            return serviceUrl;
        }

        if (serverPort == null) {
            throw new IllegalStateException(
                    "serviceUrl must be set when deployed to servlet-container");
        }

        if (preferIp) {
            InetAddress address = serverProperties.getAddress();
            if (address == null) {
                address = getHostAddress();
            }
            return append(append(createLocalUri(address.getHostAddress(), serverPort),
                    serverProperties.getServletPath()), serverProperties.getContextPath());

        }
        return append(append(createLocalUri(getHostAddress().getCanonicalHostName(), serverPort),
                serverProperties.getServletPath()), serverProperties.getContextPath());
    }

    private String createLocalUri(final String host, final int port) {
        final String scheme = serverProperties.getSsl() != null && serverProperties.getSsl().isEnabled() ? "https" : "http";
        return scheme + "://" + host + ":" + port;
    }

    private String append(final String uri, final String path) {
        final String baseUri = uri.replaceFirst("/+$", "");
        if (StringUtils.isEmpty(path)) {
            return baseUri;
        }

        final String normPath = path.replaceFirst("^/+", "").replaceFirst("/+$", "");
        return baseUri + "/" + normPath;
    }

    private InetAddress getHostAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (final UnknownHostException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
    }

}
