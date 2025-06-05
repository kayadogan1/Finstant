package enums;

/**
 * Enum representing supported platform names.
 * <p>
 * This enum provides a fixed set of platform names that can be used
 * consistently throughout the application.
 * </p>
 */
public enum PlatformName {
    TELNET("Telnet"),
    REST("REST");

    private final String name;

    /**
     * Constructor for PlatformName enum
     * @param name The display name of the
     */
    PlatformName(String name) {
        this.name = name;
    }

    /**
     * Returns the display name of the platform.
     * @return the platform name as a string
     */
    public String getName() {
        return name;
    }
}
