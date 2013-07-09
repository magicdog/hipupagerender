package com.hipu.render.stat;

/**
 * @author weijian
 * Date : 2013-06-17 19:55
 */

public interface StatUnit{

    public static enum  BrowserStat  {

        HANDLE, TIMEOUT, ERROR, LOAD_TIME, REBUILD_SINCE_INIT;

        public static final int RESET_KEY_ORDINAL = 3;
    }

    public static enum PoolStat {

        BLOCK_TIME, BLOCK, TIMEOUT
    }
}
