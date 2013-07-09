package com.hipu.render.browser;

/**
 * @author weijian
 * Date : 2013-06-18 13:33
 */

public class DefaultBrowserFactory implements BrowserFactory {


    public DefaultBrowserFactory() {
    }


    @Override
    public Browser newBrowser(String id) {
        return new Browser.Builder().withId(id).build();
    }

}
