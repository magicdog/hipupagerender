package com.hipu.render.stat;

/**
 * @author weijian
 * Date : 2013-06-18 10:54
 */

public abstract class Statistic<K extends Enum<K>> {

    protected Stat<K> stat;

    protected boolean enableStat = false;

    private static final ThreadLocal<Long> START_TIME_THREAD_LOCAL
            = new ThreadLocal<Long>();

    public Statistic() {
    }

    public Statistic(boolean enableStat, Class<K> cls) {
        this.enableStat = enableStat;
        if ( enableStat ){
            this.stat = new Stat<K>(cls);
        }
    }

    protected void _startTiming(){
        if ( enableStat ) {
            START_TIME_THREAD_LOCAL.set(System.currentTimeMillis());
        }
    }

    protected void _endTiming(K k){
        if ( enableStat ) {
            long start = START_TIME_THREAD_LOCAL.get();
            stat.incr(k, System.currentTimeMillis() - start);
        }
    }

    protected void _incrStat(K k) {
        if ( enableStat ){
            stat.incr(k);
        }
    }

    protected void _incrStat(K k, long v) {
        if ( enableStat ){
            stat.incr(k, v);
        }
    }

    protected String _toStringAndReset(int resetKeyOrdinal) {
        return enableStat ? stat.toStringAndReset(resetKeyOrdinal) : "";
    }

    protected String _toStringAndResetAll() {
        return enableStat ? stat.toStringAndResetAll() : "";
    }

    public abstract String getAndResetStat();


    public boolean isEnableStat() {
        return enableStat;
    }


}
