package org.kruskopf.backend.addon;

import java.util.Locale;
import java.util.Optional;

/**
 * The campaign add-ons this backend knows about.
 * <p>
 * An add-on is an optional capability a campaign owner switches on, so that the
 * campaign's navigation carries only the sections the table actually uses. The
 * enum is the registry: it is what makes a key valid, and a request naming
 * anything else is refused instead of stored. The row itself is generic
 * ({@link org.kruskopf.backend.addon.entity.CampaignAddon}), so a second add-on
 * costs no migration — only an entry here and its own feature package.
 * <p>
 * {@link #key()} is the form the API speaks (lower case); the database column
 * holds the enum name, which is the same word in upper case.
 */
public enum AddonKey {

    /**
     * The campaign planner: a member starts a plan, everyone adds candidate
     * dates and marks whether they can attend. Consumers live in
     * {@code org.kruskopf.backend.planner}.
     */
    PLANNER;

    /** The key as the API spells it: lower case, so URLs read {@code planner}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves a key from a request path. Case-insensitive, because the key is a
     * label rather than a secret; unknown keys are simply absent.
     *
     * @param rawKey the key as it arrived
     * @return the matching add-on, or empty when the key is not one of ours
     */
    public static Optional<AddonKey> fromKey(String rawKey) {
        if (rawKey == null) {
            return Optional.empty();
        }
        String normalized = rawKey.trim().toUpperCase(Locale.ROOT);
        for (AddonKey candidate : values()) {
            if (candidate.name().equals(normalized)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }
}
