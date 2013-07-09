package com.hipu.render.stat;

import com.google.common.collect.ImmutableSet;
import org.apache.log4j.Logger;
import sun.misc.SharedSecrets;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

/**
 * @author weijian
 * Date : 2013-06-17 19:54
 */

public class Stat<K extends Enum<K>> {

    private static final Logger LOG = Logger.getLogger(Stat.class);

    private final EnumMap<K, Long> statMap;

    public Stat(Class<K> cls) {
        statMap = new EnumMap<K, Long>(cls);
        for ( K k : SharedSecrets.getJavaLangAccess()
                .getEnumConstantsShared(cls) ) {
            statMap.put(k, 0l);
        }
    }


    public long incrAndGet(K k){
        synchronized (statMap){
            long v = statMap.get(k) + 1l;
            statMap.put(k, v);
            return v;
        }
    }


    public long incrAndGet(K k, long val){
        synchronized (statMap){
            long v = statMap.get(k) + val;
            statMap.put(k, v);
            return v;
        }
    }

    public long getAndIncr(K k){
        synchronized (statMap){
            long v = statMap.get(k);
            statMap.put(k, v + 1l);
            return v;
        }

    }


    public long getAndIncr(K k, long val){
        synchronized (statMap){
            long v = statMap.get(k);
            statMap.put(k, v + val);
            return v;
        }
    }

    public long get(K k){
        synchronized (statMap){
            return statMap.get(k);
        }
    }

    public void set(K k, long val){
        synchronized (statMap){
            statMap.put(k, val);
        }
    }

    public void incr(K k, long val){
        synchronized (statMap){
            statMap.put(k, statMap.get(k) + val);
        }
    }

    public void incr(K k){
        synchronized (statMap){
            statMap.put(k, statMap.get(k) + 1l);
        }
    }


    public void reset(K k){
        synchronized (statMap){
            statMap.put(k, 0l);
        }
    }

    public long getAndReset(K k){
        synchronized (statMap){
            long v = statMap.get(k);
            statMap.put(k, 0l);
            return v;
        }
    }

    public void resetAll(){
        synchronized (statMap){
            for (Map.Entry<K, Long> e : statMap.entrySet() ) {
                e.setValue(0l);
            }
        }
    }

    public void reset(ImmutableSet<K> keys){
        synchronized (statMap){
            for (K k : keys ) {
                statMap.put(k, 0l);
            }
        }
    }

    public String toStringAndReset(int resetKeyOrdinal){
        synchronized (statMap){
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<K, Long> e : statMap.entrySet() ) {
                sb.append(e.getKey()).append(" ")
                        .append(e.getValue()).append(", ");

                if ( e.getKey().ordinal() <= resetKeyOrdinal ){
                    e.setValue(0l);
                }
            }
            return  sb.substring(0, sb.length() - 2);
        }
    }

    public String toStringAndResetAll(){
        synchronized (statMap){
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<K, Long> e : statMap.entrySet() ) {
                sb.append(e.getKey()).append(" ")
                  .append(e.getValue()).append(", ");
                e.setValue(0l);
            }
            return  sb.substring(0, sb.length() - 2);
        }
    }


    public static void main(String[] args) {
        Stat<StatUnit.BrowserStat> stat = new Stat<StatUnit.BrowserStat>(StatUnit.BrowserStat.class);

        stat.incr(StatUnit.BrowserStat.ERROR);
        stat.incr(StatUnit.BrowserStat.HANDLE);
        stat.incr(StatUnit.BrowserStat.LOAD_TIME);
        stat.incr(StatUnit.BrowserStat.REBUILD_SINCE_INIT);

        System.out.println(stat.toStringAndReset(StatUnit.BrowserStat.RESET_KEY_ORDINAL));
        System.out.println(stat.toStringAndReset(StatUnit.BrowserStat.RESET_KEY_ORDINAL));
        System.out.println(stat.toStringAndResetAll());
        System.out.println(stat.toStringAndResetAll());
        System.out.println(Arrays.toString(StatUnit.BrowserStat.class.getEnumConstants()));
    }
}
