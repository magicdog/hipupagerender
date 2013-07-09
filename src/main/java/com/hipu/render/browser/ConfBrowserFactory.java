package com.hipu.render.browser;

import com.beust.jcommander.internal.Lists;
import com.hipu.crawlcommons.config.Config;
import com.hipu.render.config.RenderArgs;
import com.hipu.render.extension.Extension;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * @author weijian
 * Date : 2013-06-18 13:33
 */

public class ConfBrowserFactory implements BrowserFactory {

    private Config conf;

    public ConfBrowserFactory() {
        conf = Config.getInstance();
    }


    @Override
    public Browser newBrowser(String id) {
        Browser.Builder builder = new Browser.Builder()
                .withId(id)
                .withTimeout(conf.getInt(RenderArgs.BROWSER_TIMEOUT))
                .withCacheDir(conf.getString(RenderArgs.BROWSER_CACHE_DIR))
                .withExtensions(getExtensions())
                .enableStat(conf.getBoolean(RenderArgs.BROWSER_ENABLE_STAT));

        if (conf.getBoolean(RenderArgs.BROWSER_ENABLE_PROXY)){
            builder.withProxy(conf.getString(RenderArgs.BROWSER_PROXY_HOST),
                    conf.getInt(RenderArgs.BROWSER_PROXY_PORT));
        }

        return builder.build();
    }

    protected List<Extension> getExtensions(){
        String[] classes = conf.getStringArray(RenderArgs.BROWSER_EXTENSIONS);
        if (classes.length == 0) {
            return null;
        }

        List<Extension> list = Lists.newArrayList(classes.length);

        for (String name : classes) {
            try{
                Class<?> c = Class.forName(name);
                Constructor<?> con = c.getConstructor();
                list.add((Extension) con.newInstance());
            } catch (Exception e){
                throw new RuntimeException("Class name[ " + name + "]", e);
            }
        }
        return list;
    }
}
