package com.hipu.render.config;

import com.hipu.crawlcommons.config.ArgProp;
import com.hipu.crawlcommons.config.Args;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.util.List;

/**
 * @author weijian
 * Date : 2013-06-02 16:11
 */

public final class RenderArgs extends Args {

    private RenderArgs() {
        throw new AssertionError();
    }

    public static final ArgProp<Integer>	BROWSER_POOL_SIZE		= ArgProp.ofInteger	("browser.pool.size",    5);
    public static final ArgProp<Integer>	BROWSER_TIMEOUT		    = ArgProp.ofInteger	("browser.timeout",      180);
    public static final ArgProp<String>	    BROWSER_CACHE_DIR		= ArgProp.ofString	("browser.cache.dir",    "/tmp/cache");
    public static final ArgProp<Boolean>	BROWSER_ENABLE_PROXY	= ArgProp.ofBoolean	("browser.enable.proxy", false);
    public static final ArgProp<String>	    BROWSER_PROXY_HOST		= ArgProp.ofString	("browser.proxy.host",   "localhost");
    public static final ArgProp<Integer>	BROWSER_PROXY_PORT		= ArgProp.ofInteger	("browser.proxy.port",   31280);
    public static final ArgProp<Boolean>	BROWSER_ENABLE_STAT		= ArgProp.ofBoolean	("browser.enable.stat",  false);
    public static final ArgProp<Integer>	BROWSER_STAT_MINUTES	= ArgProp.ofInteger	("browser.stat.minutes", 1);
    public static final ArgProp<List>       BROWSER_EXTENSIONS		= ArgProp.ofList	("browser.extensions",   null);

    public static final ArgProp<Integer>	BROWSER_POOL_LOAD_TIMEOUT		    = ArgProp.ofInteger	("browser.pool.load.timeout",        180);
    public static final ArgProp<Integer>	BROWSER_REBUILD_INTERVAL_MINUTES    = ArgProp.ofInteger	("browser.rebuild.interval.minutes", 15);
    public static final ArgProp<String>     BROWSER_EXTENSION_NOSCRIPT_FILE	    = ArgProp.ofString	("browser.extension.noscript.file",  null);


    public static void main(String[] args) throws ConfigurationException {
        Configuration config = new PropertiesConfiguration(RenderArgs.class.getResource("/render.properties"));
        com.hipu.crawlcommons.config.Config conf = com.hipu.crawlcommons.config.Config.getInstance();
        conf.registerConfig(RenderArgs.class, config);
        System.out.println(conf.argsToString());
    }

}
