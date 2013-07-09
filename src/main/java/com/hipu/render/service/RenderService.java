package com.hipu.render.service;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.hipu.crawlcommons.config.Config;
import com.hipu.crawlcommons.servlet.ConfigServlet;
import com.hipu.render.pool.BrowserPool;
import com.hipu.render.browser.ConfBrowserFactory;
import com.hipu.render.config.RenderArgs;
import com.hipu.render.servlet.*;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * @author weijian
 * Date : 2012-11-26 15:48
 */
public class RenderService implements Runnable{
    private static final Logger LOG = Logger.getLogger(RenderService.class);

    @Parameter(description="Help", names={"--help","-help"})
    private boolean help = false;
    
    @Parameter(description="Service Host", names="-h")
    private String host  = "localhost";

    @Parameter(description="Port Number", names="-p")
    private int port = 8081;

    private static final String SHUTDOWN_TOKEN = "qwert";

    /**
     * @Fields:mainThread:main process
     */
    private BrowserPool browserPool;
    

    public RenderService(){

    }

    @Override
    public void run() {
        Config conf  = Config.getInstance();

        int poolSize = conf.getInt(RenderArgs.BROWSER_POOL_SIZE);
        int statMins = conf.getInt(RenderArgs.BROWSER_STAT_MINUTES);
        int timeout = conf.getInt(RenderArgs.BROWSER_POOL_LOAD_TIMEOUT);
        int rebuild = conf.getInt(RenderArgs.BROWSER_REBUILD_INTERVAL_MINUTES);
        boolean enableStat = conf.getBoolean(RenderArgs.BROWSER_ENABLE_STAT);

        browserPool = new BrowserPool.Builder()
                .withPoolSize(poolSize)
                .withTimeout(timeout)
                .withBrowserFactory(new ConfBrowserFactory())
                .enableStat(enableStat)
                .withStatDelay(statMins, TimeUnit.MINUTES)
                .withRebuildInterval(rebuild, TimeUnit.MINUTES)
                .build();


        Server server = new Server();
        Connector conn = new SelectChannelConnector();
        conn.setHost(host);
        conn.setPort(port);
        server.setConnectors(new Connector[] { conn });

        ServletContextHandler pages = new ServletContextHandler(ServletContextHandler.SESSIONS);
        pages.setContextPath("/");
        ResourceHandler handler = new ResourceHandler();
        handler.setBaseResource(Resource.newClassPathResource("resource"));
        handler.setCacheControl("max-age=5");
        pages.setHandler(handler);


        ServletContextHandler root = new ServletContextHandler(ServletContextHandler.SESSIONS);
        root.setContextPath("/service");
        root.addServlet(new ServletHolder(new ConfigServlet()), "/config");
        root.addServlet(new ServletHolder(new RenderServlet(browserPool)), "/render");
        root.addServlet(new ServletHolder(new ScreenServlet(browserPool)), "/screen");
        root.addServlet(new ServletHolder(new PoolInfoServlet(browserPool)), "/pool");
        root.addServlet(new ServletHolder(new ScreenAllServlet(browserPool)), "/allscreen");
        root.addServlet(new ServletHolder(new RebuildServlet(browserPool)), "/rebuild");

        Handler shutdownHandler = new ShutdownHandler(server, SHUTDOWN_TOKEN);

        HandlerList lists = new HandlerList();
        lists.setHandlers(new Handler[] { shutdownHandler, root, pages });

        server.setHandler(lists);


        try {
            server.start();
            server.join();

        } catch (Exception e) {
            LOG.fatal("", e);
        }

        browserPool.destroy();
    }

    /**
     * @param args
     */
    public static void main(String[] args) throws ConfigurationException {

        URL url = RenderService.class.getResource("/log4j.properties");
        if ( url == null ) {
            url =  RenderService.class.getResource("/conf/log4j.properties");
        }
        PropertyConfigurator.configure(url);
        
        URL properties = RenderService.class.getResource("/render.properties");
        if ( properties == null ) {
        	properties =  RenderService.class.getResource("/conf/render.properties");
        }

        Configuration config = new PropertiesConfiguration(properties);
        Config.getInstance().registerConfig(RenderArgs.class, config);

//        System.out.println(Config.getInstance().getBoolean(RenderArgs.BROWSER_ENABLE_PROXY));

        RenderService service = new RenderService();
        JCommander commander = new JCommander(service);
        try {
            commander.parse(args);
        } catch (ParameterException e) {
            LOG.error(e.getMessage());
            commander.usage();
        }
        if ( service.help ){
            commander.usage();
        }else{
            service.run();
        }
    }
}