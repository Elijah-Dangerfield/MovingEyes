package com.dangerfield.movingeyes.libraries.movingeyes

/**
 * Per-installation identifier — a UUID minted on first launch, persisted in
 * [AppData.installId], and regenerated on reinstall.
 *
 * This is the *only* identifier Moving Eyes has. There are no accounts, so
 * analytics events are scoped to this id and nothing else, and crash reports
 * carry it so "every session from this tester" is answerable without knowing
 * who the tester is.
 *
 * Implementations must be cheap and non-suspending. The expected shape is
 * "hydrate once at boot, cache in memory, return the cached value";
 * `null` means hydration hasn't landed yet, which callers treat as
 * "attribute omitted" rather than an error.
 */
interface InstallIdProvider {
    fun current(): String?
}
