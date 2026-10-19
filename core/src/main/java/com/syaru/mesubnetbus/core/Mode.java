package com.syaru.mesubnetbus.core;

public enum Mode {
    POWER(false, false), SHARED(true, true), EXTRACT(false, true), INSERT(true, false);

    public final boolean insert;
    public final boolean extract;
    Mode(boolean insert, boolean extract) { this.insert = insert; this.extract = extract; }
    public boolean storage() { return insert || extract; }
    public Mode next(int step) { return values()[Math.floorMod(ordinal() + step, values().length)]; }
    public static Mode decode(int value) {
        return value >= 0 && value < values().length ? values()[value] : SHARED;
    }
    public String translation() { return "mode.me_subnet_bus." + name().toLowerCase(java.util.Locale.ROOT); }
}
