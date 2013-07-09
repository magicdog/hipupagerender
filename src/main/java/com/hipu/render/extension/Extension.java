package com.hipu.render.extension;

import org.openqa.selenium.firefox.FirefoxProfile;

import java.io.File;
import java.io.IOException;

/**
 * @author weijian
 * Date : 2013-06-19 11:11
 */
public abstract class Extension {

    protected File extFile;


    public Extension(){

    }


    public void load(FirefoxProfile profile) throws IOException {
        profile.addExtension(extFile);
        _setPreference(profile);
    }

    protected abstract void _setPreference(FirefoxProfile profile);

}
