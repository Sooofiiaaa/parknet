package com.parknet.model;

public enum District {
    CENTER("Център"),
    LOZENETS("Лозенец"),
    MLADOST("Младост 1"),
    STUDENTSKI("Студентски град"),
    LYULIN("Люлин"),
    OBORISHTE("Оборище"),
    PODUYANE("Подуяне"),
    KRASNO_SELO("Красно село"),
    GEO_MILEV("Гео Милев"),
    MANASTIRSKI_LIVADI("Манастирски ливади"),
    IVAN_VAZOV("Иван Вазов"),
    BOROVO("Борово"),
    DIANABAD("Дианабад"),
    DRUZHBA("Дружба 1"),
    NADEZHDA("Надежда"),
    BANISHORA("Банишора"),
    OVCHA_KUPEL("Овча купел"),
    GOTSE_DELCHEV("Гоце Делчев"),
    IZTOK("Изток"),
    SVETA_TROITSA("Света Троица"),
    OTHER("Друго");

    private final String displayName;

    District(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
