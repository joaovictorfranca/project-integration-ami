package com.eletra.integracao.networkftp.config;

import org.apache.ftpserver.DataConnectionConfigurationFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("!test")
public class FtpServerConfig {

    @Value("${application.ftp.pasv_ports}")
    private String ftpPasvPorts;

    @Value("${application.ftp.username}")
    private String ftpClientUsername;

    @Value("${application.ftp.password}")
    private String ftpClientPassword;

    @Value("${application.ftp.host}")
    private String ftpHost;

    @Value("${application.ftp.port}")
    private Integer ftpPort;

    @Value("${application.ftp.listener}")
    private String ftpListener;

    @Value("${application.ftp.root_directory}")
    private String ftpRootDirectory;

    @Bean
    public DefaultFtpSessionFactory ftpSessionFactory() {
        DefaultFtpSessionFactory sf = new DefaultFtpSessionFactory();
        sf.setHost(ftpHost);
        sf.setPort(ftpPort);
        sf.setUsername(ftpClientUsername);
        sf.setPassword(ftpClientPassword);
        sf.setClientMode(2);
        return sf;
    }

    @Bean(destroyMethod = "stop")
    public FtpServer ftpServer() throws Exception {
        FtpServerFactory serverFactory = new FtpServerFactory();

        ListenerFactory listenerFactory = new ListenerFactory();
        listenerFactory.setPort(ftpPort);

        DataConnectionConfigurationFactory dataConnFactory = new DataConnectionConfigurationFactory();
        dataConnFactory.setPassivePorts(ftpPasvPorts);
        listenerFactory.setDataConnectionConfiguration(dataConnFactory.createDataConnectionConfiguration());

        serverFactory.addListener(ftpListener, listenerFactory.createListener());

        BaseUser user = new BaseUser();
        user.setName(ftpClientUsername);
        user.setPassword(ftpClientPassword);

        Path home = Paths.get(System.getProperty("user.home"), ftpRootDirectory);
        Files.createDirectories(home);
        user.setHomeDirectory(home.toAbsolutePath().toString());

        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        user.setAuthorities(authorities);

        serverFactory.getUserManager().save(user);

        FtpServer server = serverFactory.createServer();
        server.start();
        return server;
    }
}