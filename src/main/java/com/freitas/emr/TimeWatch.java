package com.freitas.emr;

import java.util.concurrent.TimeUnit;

public class TimeWatch {

	long starts;

    private TimeWatch() {
        reset();
    }

    public static TimeWatch start() {
        return new TimeWatch();
    }

    public TimeWatch reset() {
        starts = System.nanoTime();
        return this;
    }

    public long time() {
        long ends = System.nanoTime();
        return ends - starts;
    }

    public long time(TimeUnit unit) {
        return unit.convert(time(), TimeUnit.NANOSECONDS);
    }
    
}
