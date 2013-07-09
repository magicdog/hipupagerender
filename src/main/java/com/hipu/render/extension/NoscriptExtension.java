package com.hipu.render.extension;

import com.hipu.crawlcommons.config.Config;
import com.hipu.render.config.RenderArgs;
import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.File;

/**
 * @author weijian
 * Date : 2013-06-19 11:38
 */

public class NoscriptExtension extends Extension {


    public NoscriptExtension(){
          this(Config.getInstance().getString(
                  RenderArgs.BROWSER_EXTENSION_NOSCRIPT_FILE));
    }

    public NoscriptExtension(String extFile) {
        this.extFile = new File(extFile);
    }

    public NoscriptExtension(File extFile) {
        this.extFile = extFile;
    }

    @Override
    protected void _setPreference(FirefoxProfile profile) {
        profile.setPreference("noscript.contentBlocker", true);
        profile.setPreference("noscript.ef.Blitzableiter.contentType", "shockwave|futuresplash");
        profile.setPreference("noscript.ef.enabled", true);
        profile.setPreference("noscript.forbidFrames", true);
        profile.setPreference("noscript.forbidIFrames", true);
        profile.setPreference("noscript.global", true);
        profile.setPreference("noscript.showPlaceholder", false);
    }
}
