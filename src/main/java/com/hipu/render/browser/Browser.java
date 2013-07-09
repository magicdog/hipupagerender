package com.hipu.render.browser;

import com.google.common.base.Preconditions;
import com.hipu.crawlcommons.config.Config;
import com.hipu.crawlcommons.utils.IBuilder;
import com.hipu.render.config.RenderArgs;
import com.hipu.render.extension.Extension;
import com.hipu.render.stat.StatUnit.BrowserStat;
import com.hipu.render.stat.Statistic;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author weijian
 * Date : 2013-03-08 14:27
 */
public class Browser extends Statistic<BrowserStat> {
    private static final Logger LOG = Logger.getLogger(Browser.class);

    private final String id;

    private final int timeout;
    private String cacheDir = null;
    private boolean enableProxy = false;
    private String proxyHost;
    private int proxyPort;

    private final List<Extension> extensions;

    private long rebuildTime;

    /**
     * @Fields: profile	 : contains the concrete settings about firefox
     * @Fields: firefoxDriver: using this object to load url
     */
	private FirefoxProfile profile;
	private FirefoxDriver firefoxDriver;

    private Browser(Builder builder) {
        super(builder.enableStat, BrowserStat.class);

        this.id = (builder.id != null ? builder.id : Integer.toHexString(super.hashCode()));

        this.timeout = builder.timeout;
        this.cacheDir = builder.cacheDir;

        this.enableProxy = builder.enableProxy;
        this.proxyHost = builder.proxyHost;
        this.proxyPort = builder.proxyPort;

        this.extensions = builder.extensions;

        this.rebuildTime = System.currentTimeMillis();
        _initFirefox();
    }


    protected void _initFirefox() {

		profile = new FirefoxProfile();

		if (this.enableProxy) {
            profile.setPreference("network.proxy.type", 1);
            profile.setPreference("network.proxy.http", this.proxyHost);
            profile.setPreference("network.proxy.http_port", this.proxyPort);
        }

        if ( this.cacheDir != null ){
		    profile.setPreference( "browser.cache.disk.enable", 			true );
            profile.setPreference( "browser.cache.disk.parent_directory", 	this.cacheDir);
        }

        profile.setPreference( "permissions.default.image", 					2 ); 	// forbidding loading image
		profile.setPreference( "network.http.pipelining", 						true ); // open multithread load
		profile.setPreference( "network.http.proxy.pipelining", 				true ); // open proxy multithread load
		profile.setPreference( "network.http.pipelining.maxrequests",			8 ); 	// the number of thread loading one page

		profile.setPreference( "plugins.click_to_play", 						true ); // forbidding loading plugins, including flash
		profile.setPreference( "media.autoplay.enabled", 						false );// disable autoplay
		profile.setPreference( "plugin.default_plugin_disabled", 				false );// never providing hint when failed to load plugin
		profile.setPreference( "network.http.max-persistent-connections-per-proxy",  200 );
		profile.setPreference( "network.http.max-persistent-connections-per-server", 32 );

		profile.setPreference( "privacy.popups.disable_from_plugins", 			3 );
		profile.setPreference( "extensions.enabledAddons", 						"" );

		profile.setPreference( "content.notify.interval", 						750000 );
		profile.setPreference( "content.notify.ontimer", 						true );

		profile.setPreference( "content.switch.threshold", 						250000 );
		profile.setPreference( "browser.cache.memory.capacity", 				65536 );
		profile.setPreference( "browser.cache.memory.enable", 					true );

		profile.setPreference( "nglayout.initialpaint.delay", 					0 );
		profile.setPreference( "ui.submenuDelay", 								0 );

		profile.setPreference( "network.http.max-connections", 					256 );
		profile.setPreference( "network.dns.disableIPv6", 						true );
		profile.setPreference( "network.http.requests.max-start-delay", 		0 );
		profile.setPreference( "content.interrupt.parsing", 					true );
		profile.setPreference( "content.max.tokenizing.time", 					2250000 );

		profile.setPreference( "content.notify.backoffcount",   				5 );
		profile.setPreference( "plugin.expose_full_path", 						true );
		profile.setPreference( "network.http.keep-alive", 						true );
		profile.setPreference( "network.http.version", 							"1.1" );
		profile.setPreference( "dom.popup_maximum", 							0 );

        profile.setPreference( "browser.chrome.favicons",           false );


        profile.setPreference( "network.http.connection-retry-timeout", 5 );
        profile.setPreference( "network.http.pipelining.read-timeout",  10 );
        profile.setPreference( "network.http.connection-timeout",       5 );
        profile.setPreference( "network.http.keep-alive.timeout",       5 );

        if ( extensions != null ){
            for (Extension ext : extensions ){
                try {
                    ext.load(profile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        firefoxDriver = new FirefoxDriver(profile);
        this.setTimeout(this.timeout);
	}


	private void setTimeout(int timeout) {
		firefoxDriver.manage().timeouts().setScriptTimeout(timeout, TimeUnit.SECONDS);
        firefoxDriver.manage().timeouts().implicitlyWait(timeout, TimeUnit.SECONDS);
        firefoxDriver.manage().timeouts().pageLoadTimeout(timeout, TimeUnit.SECONDS);
	}

    /**
     * @Title: load, not thread safe
     * @Description: return the source of page
     * @param url : like http://www.hipu.com
     * @return String : the page source, if failed return ""
     */
    public String load(String url) throws Exception{
    	if ( url == null )
    		return "";

        if (LOG.isDebugEnabled()) {
            LOG.debug(name() + " load " + url);
        }

        synchronized (id){
            _incrStat(BrowserStat.HANDLE);

            _startTiming();
            try{
                firefoxDriver.get(url);
            } catch (TimeoutException e){
                _incrStat(BrowserStat.TIMEOUT);

                if (LOG.isInfoEnabled()){
                    LOG.info("WebDriver load timeout: " + url);
                }
            } catch (Exception e) {
                _incrStat(BrowserStat.ERROR);
                throw e;
            }

            String html = firefoxDriver.getPageSource();

            _endTiming(BrowserStat.LOAD_TIME);

            return html;
        }
    }

    public String loadScreenshot(String url) throws Exception{
        if ( url == null )
            return "";

        synchronized (id){

            _incrStat(BrowserStat.HANDLE);

            _startTiming();

            try{
                firefoxDriver.get(url);
            } catch (TimeoutException e){
                _incrStat(BrowserStat.TIMEOUT);

                if (LOG.isInfoEnabled()){
                    LOG.info("WebDriver load timeout: " + url);
                }
            } catch (Exception e) {
                _incrStat(BrowserStat.ERROR);
                throw e;
            }

            String base64 = firefoxDriver.getScreenshotAs(OutputType.BASE64);

            _endTiming(BrowserStat.LOAD_TIME);

            return base64;
        }
    }

    public String screen(){
        synchronized (id){
            return firefoxDriver.getScreenshotAs(OutputType.BASE64);
        }
    }

    private void _destroyFirefox(){
        try {
            firefoxDriver.kill();
        }  catch (Exception e) {
            LOG.warn("Close firefox error!", e);
        }
    }

    public void destroy(){
        _destroyFirefox();
    }

    public void forceRebuildingFirefox(){
        _incrStat(BrowserStat.REBUILD_SINCE_INIT);

        if (LOG.isInfoEnabled()) {
            LOG.info("Force rebuilding " + name());
        }
        _destroyFirefox();

        _initFirefox();

        _recordRebuildTime();
    }

    public void rebuildFirefox(){
        _incrStat(BrowserStat.REBUILD_SINCE_INIT);

        synchronized (id){
            if (LOG.isInfoEnabled()) {
                LOG.info("Safely rebuild " + name());
            }
            _destroyFirefox();
            _initFirefox();
        }
        _recordRebuildTime();
    }

    public String name(){
        return "Browser-"+id;
    }

    @Override
    public String getAndResetStat() {
        return String.format("%s %s", name(), _toStringAndReset(BrowserStat.RESET_KEY_ORDINAL));
    }

    private void _recordRebuildTime(){
        rebuildTime = System.currentTimeMillis();
    }

    public long lastRebuildInterval() {
        return System.currentTimeMillis() - rebuildTime;
    }

    public int getTimeout() {
        return timeout;
    }

    public String getCacheDir() {
        return cacheDir;
    }

    public boolean isEnableProxy() {
        return enableProxy;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getId() {
        return id;
    }


    public static class Builder
            implements IBuilder<Browser> {

        private String id = null;

        private int timeout = 60;
        private String cacheDir = null;
        private boolean enableStat = false;
        private boolean enableProxy = false;
        private String proxyHost = null;
        private int proxyPort = 0;

        private List<Extension> extensions = null;

        public Builder(){
        }

        public Builder withId(String browserId)
        {
            Preconditions.checkArgument(browserId != null, "BrowserId must not be null!");

            this.id = browserId;
            return this;
        }

        public Builder withTimeout(int timeout)
        {
            Preconditions.checkArgument(timeout > 0, "Timeout must be positive integer!");

            this.timeout = timeout;
            return this;
        }

        public Builder withCacheDir(String cacheDir)
        {
            this.cacheDir = cacheDir;
            return this;
        }

        public Builder enableStat(boolean enableStat)
        {
            this.enableStat = enableStat;
            return this;
        }

        public Builder withProxy(String host, int port)
        {
            this.proxyHost = host;
            this.proxyPort = port;
            this.enableProxy = true;
            return this;
        }

        public Builder withExtensions(List<Extension> extensions)
        {
            this.extensions = extensions;
            return this;
        }


        @Override
        public Browser build() {
            return new Browser(this);
        }
    }





    public static void main(String[] args) throws ConfigurationException, InterruptedException {
//        final Browser browser = new Browser(1, 60, null, "/tmp/cache");
        URL url = Browser.class.getResource("/log4j.properties");

        PropertyConfigurator.configure(url);

        URL properties = Browser.class.getResource("/render.properties");

        Configuration config = new PropertiesConfiguration(properties);
        Config.getInstance().registerConfig(RenderArgs.class, config);

        final Browser browser = new ConfBrowserFactory().newBrowser("1");
//
//        System.out.println(browser.name());
//
//
//        String url = "http://hb.qq.com/a/20130222/000482.htm";
////        String url = "http://ent.ifeng.com/tv/special/haoshengyin/kuaixun2/detail_2012_09/30/18021061_0.shtml ";
//        Thread t1 = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
////                    String s = browser.load("http://dzwww.com/");
////                    System.out.println(s);
//                    browser.forceRebuildingFirefox();
//                } catch (Exception e) {
//                    LOG.error("", e);
//                }
//            }
//        });
//        Thread t2 = new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    String s = browser.load("http://www.huffingtonpost.com/2012/09/30/eagles-beat-giants-19-17-tynes-missed-field-goal_n_1927862.html");
//                    System.out.println(s);
//                } catch (Exception e) {
//                    LOG.error("", e);
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        t2.start();
//
//
//        Thread.sleep(2000);
//
////        browser.firefoxDriver.close();
//        t1.start();
//
//        t2.join();
//        t1.join();

        try {
        	Date start = new Date();
//			System.out.println(browser.load(url));
//			System.out.println(browser.load("http://sports.xinmin.cn/2012/09/30/16564327.html"));
//            			System.out.println(browser.load("http://www.huffingtonpost.com/2012/09/30/eagles-beat-giants-19-17-tynes-missed-field-goal_n_1927862.html "));


//            System.out.println(browser.load("http://binzhou.dzwww.com/yvle/201307/t20130701_8579432_12.htm"));
            System.out.println(browser.load("http://dongying.dzwww.com/yl/201307/t20130702_8587724.htm"));
//            System.out.println(browser.load("http://ent.qq.com/a/20130301/000385.htm"));
//            System.out.println(browser.load("http://mobile.163.com/13/0325/07/8QQ1JCAL0011671M.html"));
//            System.out.println(browser.load("http://sports.163.com/13/0518/09/8V574V1B00051CA1.html"));
//            System.out.println(browser.load("http://news.dzwww.com/shehuixinwen/201303/t20130305_8077417.htm"));
			System.out.println(System.currentTimeMillis() - start.getTime());

//            browser.forceRebuildingFirefox();
//			System.out.println(browser.load("http://www.huffingtonpost.com/2012/09/30/eagles-beat-giants-19-17-tynes-missed-field-goal_n_1927862.html "));
			Date end = new Date();
			System.out.println(end.getTime() - start.getTime());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//        browser.destroy();
    }
}
