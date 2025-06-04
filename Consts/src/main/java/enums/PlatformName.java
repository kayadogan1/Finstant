package enums;

public enum PlatformName {
    TELNET("Telnet"),
    REST("REST");

    private final String name;
    PlatformName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
