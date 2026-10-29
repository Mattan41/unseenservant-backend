package org.kruskopf.backend.audit.entity;

/**
 * What an audit row is about.
 * <p>
 * Deliberately two families, because they answer different questions:
 * <ul>
 *   <li>the changes — {@link #ACCOUNT_CREATED}, {@link #ROLE_CHANGED} and the
 *       three {@code WHITELIST_*} values: <em>what happened to this account or
 *       address</em>, recorded in the caller's transaction, so an
 *       unwritable row vetoes the change it describes;</li>
 *   <li>the attempts and refusals — <em>who tried what</em>, recorded in their
 *       own transaction, because the request that produced them is allowed to
 *       succeed (a login) or is about to be refused (a role change), and either
 *       way the row has to survive.</li>
 * </ul>
 * The names are the wire contract: the client labels and badges on them, so a
 * rename is an API change. See docs/INTEGRATION_CONTRACT.md.
 */
public enum AuditEventType {

    /** A first login through a provider created the account. */
    ACCOUNT_CREATED,

    /** A correct password, or a completed OAuth handshake. */
    LOGIN_SUCCEEDED,

    /** A wrong password or an unknown username. */
    LOGIN_FAILED,

    /** The login throttle answered 429. */
    LOGIN_THROTTLED,

    /** A provider login was refused before an account was touched. */
    OAUTH_REFUSED,

    /** An administrator changed an account's role. */
    ROLE_CHANGED,

    /** A role change was refused by the self or last-administrator guard. */
    ROLE_CHANGE_REFUSED,

    /** An address was admitted to the whitelist (or a revoked one re-admitted). */
    WHITELIST_ADDED,

    /** A whitelist row was re-roled and/or re-activated. */
    WHITELIST_UPDATED,

    /** An address was revoked (a soft delete — the row stays). */
    WHITELIST_REVOKED
}
