package com.hipu.render.pool;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.hipu.crawlcommons.config.Config;
import com.hipu.crawlcommons.utils.DaemonThreadFactory;
import com.hipu.crawlcommons.utils.IBuilder;
import com.hipu.render.browser.Browser;
import com.hipu.render.browser.BrowserFactory;
import com.hipu.render.browser.ConfBrowserFactory;
import com.hipu.render.browser.DefaultBrowserFactory;
import com.hipu.render.config.RenderArgs;
import com.hipu.render.stat.StatUnit.PoolStat;
import com.hipu.render.stat.Statistic;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.openqa.selenium.remote.SessionNotFoundException;
import org.openqa.selenium.remote.UnreachableBrowserException;

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author weijian
 * Date : 2013-06-14 13:34
 */

public class BrowserPool extends Statistic<PoolStat>{
    public static final Logger LOG = Logger.getLogger(BrowserPool.class);

    private final int poolSize;

    private final int timeout;

    private final long rebuildInterval;

    private AtomicInteger running;

    private Set<Browser> browserSet;

    private BrowserFactory browserFactory;

    private BlockingQueue<Browser> availableBrowserQueue;

    private ScheduledExecutorService statExecutor;

    private ScheduledExecutorService rebuildExecutor;

    private ExecutorService executor;


    private BrowserPool(Builder builder) {
        super(builder.enableStat, PoolStat.class);
        this.poolSize = builder.poolSize;
        this.timeout = builder.timeout;
        this.rebuildInterval = builder.rebuildInterval;
        _init(this.poolSize, builder.browserFactory, builder.statDelay, builder.statTimeUnit);
    }

    private void _init(int poolSize, BrowserFactory browserFactory, int statDelay, TimeUnit timeUnit){

        browserSet = Sets.newSetFromMap(new ConcurrentHashMap<Browser, Boolean>(poolSize));
        availableBrowserQueue = new LinkedBlockingQueue<Browser>(poolSize);

        this.browserFactory = (browserFactory == null ?
                new DefaultBrowserFactory() : browserFactory);
        running = new AtomicInteger(0);

        Browser browser = newBrowser();
        browserSet.add(browser);
        availableBrowserQueue.add(browser);

        executor = Executors.newCachedThreadPool(new DaemonThreadFactory());

        DaemonThreadFactory threadFactory = new DaemonThreadFactory();

        if ( rebuildInterval != 0l ){
            rebuildExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
            rebuildExecutor.scheduleWithFixedDelay(new RebuildTask(), rebuildInterval, rebuildInterval, TimeUnit.SECONDS);
        }

        if ( this.enableStat ){
            statExecutor = Executors.newSingleThreadScheduledExecutor(threadFactory);
            statExecutor.scheduleWithFixedDelay(new StatTask(), 0, statDelay, timeUnit);
        }
    }

    private class StatTask implements Runnable{

        @Override
        public void run() {
            if (LOG.isInfoEnabled()) {
                for ( Browser browser : BrowserPool.this.browserSet ){
                    LOG.info(browser.getAndResetStat());
                }
                LOG.info(BrowserPool.this.getAndResetStat());
            }
        }
    }

    private class RebuildTask implements Runnable{

        @Override
        public void run() {
            if (LOG.isInfoEnabled()) {
                LOG.info("Check need rebuilding...");
            }

            for ( Browser browser : BrowserPool.this.browserSet ){
                if ( BrowserPool.this._needRebuild(browser) ){
                    browser.rebuildFirefox();
                }
            }
        }
    }

    private Browser newBrowser() {
        int id;
        for (;;) {
            int all = running.get();
            if ( all >= poolSize ){
                return null;
            }

            if ( running.compareAndSet(all, all + 1) ){
                id = all + 1;
                break;
            }
        }

        Browser browser = browserFactory.newBrowser(Integer.toString(id));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Construct Browser[" + id + "]!");
        }
        return browser;
    }

    protected Browser _getBrowser() throws InterruptedException {
        _startTiming();
        Browser browser = availableBrowserQueue.poll();
        if ( browser == null ){
            browser = newBrowser();
            if ( browser == null ){
                _incrStat(PoolStat.BLOCK);
                browser = availableBrowserQueue.take();
            } else {
                browserSet.add(browser);
            }
        }

        _endTiming(PoolStat.BLOCK_TIME);
        return browser;
    }



    protected void _returnBrowser(Browser browser){
        availableBrowserQueue.add(browser);
    }

    protected void _returnErrorBrowser(Browser browser){
        executor.submit(new ReturnErrBrowserTask(browser));
    }

    private boolean  _needRebuild(Browser browser){
        return rebuildInterval != 0l &&
                ( browser.lastRebuildInterval() / 1000 >= rebuildInterval );
    }


    private class ReturnErrBrowserTask implements Runnable{
        Browser browser;

        private ReturnErrBrowserTask(Browser browser) {
            this.browser = browser;
        }

        @Override
        public void run() {
            try {
                browser.forceRebuildingFirefox();
            } catch (Exception e){
                LOG.error("", e);
            }

            BrowserPool.this.availableBrowserQueue.add(browser);
        }
    }


    private static class Task implements Callable<String>{
        private Browser browser;
        private String url;

        private Task(Browser browser, String url) {
            this.browser = browser;
            this.url = url;
        }

        @Override
        public String call() throws Exception {
            return browser.load(url);
        }
    }

    public String load(String url) throws Exception {
        Browser browser = _getBrowser();

        Future<String> future = executor.submit(new Task(browser, url));

        String html = "";
        try {
            html = future.get(this.timeout, TimeUnit.SECONDS);
            _returnBrowser(browser);
        } catch (InterruptedException e) {
            _returnErrorBrowser(browser);
            LOG.error(String.format("%s\t%s\t%s", e.getMessage(), browser.name(), url));
        } catch (TimeoutException e) {
            _incrStat(PoolStat.TIMEOUT);
            _returnErrorBrowser(browser);
            LOG.error(String.format("%s\t%s\t%s", e.getMessage(), browser.name(), url));
        } catch (ExecutionException e) {
            _returnErrorBrowser(browser);
            LOG.error(String.format("%s\t%s\t%s", e.getMessage(), browser.name(), url));
        } catch (Exception e) {
            _returnErrorBrowser(browser);
            throw e;
        }

        return html;
    }

    public String _load(String url) throws Exception {
        Browser browser = _getBrowser();
        String base64;
        try {
            base64 = browser.load(url);
        } catch (SessionNotFoundException e) {
            _returnErrorBrowser(browser);

            LOG.error(url, e);
            throw e;
        } catch (UnreachableBrowserException e) {
            _returnErrorBrowser(browser);

            LOG.error(url, e);
            throw e;
        } catch (Exception e) {
            _returnErrorBrowser(browser);
            LOG.error(url, e);
            throw e;
        }

        _returnBrowser(browser);
        return base64;
    }

    public String loadScreenshot(String url) throws Exception {
        Browser browser = _getBrowser();
        String base64;
        try {
            base64 = browser.loadScreenshot(url);
        } catch (SessionNotFoundException e) {
            _returnErrorBrowser(browser);

            LOG.error(url, e);
            throw e;
        } catch (UnreachableBrowserException e) {
            _returnErrorBrowser(browser);

            LOG.error(url, e);
            throw e;
        } catch (Exception e) {
            _returnErrorBrowser(browser);
            LOG.error(url, e);
            throw e;
        }

        _returnBrowser(browser);
        return base64;
    }


    // NOTE: not thread safe
    //       Just for test
    public List<String> screenAllBrowser(){
        List<String> list = Lists.newArrayList();
          for ( Browser browser : browserSet ){
               list.add(browser.screen());
          }
        return list;
    }


    public void rebuildAllBrowser(){
        for ( Browser browser : browserSet ){
            browser.rebuildFirefox();
        }
    }

    @Override
    public String getAndResetStat() {
        return _toStringAndResetAll();
    }




    public int availableBrowsers(){
        return availableBrowserQueue.size();
    }

    public void destroy(){
        for( Browser browser : browserSet ){
            browser.destroy();
        }
    }

    public static class Builder
            implements IBuilder<BrowserPool> {

        private int poolSize = 10;

        private int timeout = 180;

        private int statDelay = 3;
        private TimeUnit statTimeUnit = TimeUnit.MINUTES;

        private boolean enableStat = false;

        private BrowserFactory browserFactory = null;

        private long rebuildInterval = 0l;

        public Builder(){
        }

        public Builder withPoolSize(int poolSize)
        {
            Preconditions.checkArgument(poolSize > 0, "PoolSize must be positive integer!");

            this.poolSize = poolSize;
            return this;
        }

        public Builder withStatDelay(int delay, TimeUnit timeUnit)
        {
            this.statDelay = delay;
            this.statTimeUnit = timeUnit;
            return this;
        }

        public Builder enableStat(boolean enableStat)
        {
            this.enableStat = enableStat;
            return this;
        }

        public Builder withBrowserFactory(BrowserFactory browserFactory)
        {
            this.browserFactory = browserFactory;
            return this;
        }

        public Builder withTimeout(int timeout){
            Preconditions.checkArgument(timeout > 0, "Timeout must be positive integer!");

            this.timeout = timeout;
            return this;
        }

        public Builder withRebuildInterval(long interval, TimeUnit timeUnit){
            Preconditions.checkArgument(interval >= 0, "Interval must be greater or equal than 0!");

            this.rebuildInterval = TimeUnit.SECONDS.convert(interval, timeUnit);
            return this;
        }

        @Override
        public BrowserPool build() {
            return new BrowserPool(this);
        }
    }


    public static void main(String[] args) throws ConfigurationException, InterruptedException {
        URL url = BrowserPool.class.getResource("/log4j.properties");

        PropertyConfigurator.configure(url);

        URL properties = BrowserPool.class.getResource("/render.properties");

        Configuration config = new PropertiesConfiguration(properties);
        Config.getInstance().registerConfig(RenderArgs.class, config);

        final BrowserPool pool = new BrowserPool.Builder()
                .withPoolSize(30)
                .withBrowserFactory(new ConfBrowserFactory())
                .withRebuildInterval(2, TimeUnit.MINUTES)
                .build();

        for (int i = 0; i < 30; i++) {
//            System.out.println(i);
//            System.out.println("running " + pool.running);

//
//            Browser browser = pool.newBrowser();
//
//            pool.browserSet.add(browser);
//            pool.availableBrowserQueue.add(browser);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        System.out.println(pool.load("http://www.baidu.com/"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }


        Thread.sleep(600000);
    }
}
