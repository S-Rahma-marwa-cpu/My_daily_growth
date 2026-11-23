package com.mydailygrowth.model;

public enum Prayer {
    FAJR("Fajr", "الفجر"),
    DHUHR("Dhuhr", "الظهر"),
    ASR("Asr", "العصر"),
    MAGHRIB("Maghrib", "المغرب"),
    ISHA("Isha", "العشاء");

    private final String label;
    private final String arabic;

    Prayer(String label, String arabic) {
        this.label = label;
        this.arabic = arabic;
    }

    public String label() { return label; }
    public String arabic() { return arabic; }

    public static Prayer fromLabel(String label) {
        for (Prayer prayer : values()) {
            if (prayer.label.equalsIgnoreCase(label) || prayer.name().equalsIgnoreCase(label)) return prayer;
        }
        
        throw new IllegalArgumentException("Unknown prayer: " + label);
    }
}
