package de.saschaufer.apps.tally.management;

import de.saschaufer.apps.tally.config.server.ServerProperties;
import lombok.Getter;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Getter
@Component
public class UserAgent {

    private final String appName;
    private final String appVersion;
    private final String port;
    private final String hostName;
    private final String hostIp;
    private final String fullName;

    public UserAgent(final BuildProperties buildProperties, final ServerProperties serverProperties) throws UnknownHostException {
        
        appName = buildProperties.getArtifact();
        appVersion = buildProperties.getVersion();
        port = String.valueOf(serverProperties.port());
        hostName = InetAddress.getLocalHost().getCanonicalHostName();
        hostIp = InetAddress.getLocalHost().getHostAddress();

        fullName = String.format("%s/%s(%s/%s:%s)", getAppName(), getAppVersion(), getHostName(), getHostIp(), getPort());
    }
}
