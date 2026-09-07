import Foundation
import StoreKit
import ComposeApp

/// StoreKit 2, behind the Kotlin `BillingStoreKitCoordinator` protocol.
///
/// This has to be Swift: `Product.purchase()` and `Transaction` are Swift-only
/// `async` APIs with no Objective-C surface for Kotlin/Native to bind against.
/// The Kotlin side is therefore a plain-callback protocol with primitive
/// parameters, and `StoreKitBillingClient` wraps these back into suspend
/// functions.
///
/// Moving Eyes sells one non-consumable, which makes the lifecycle simple:
/// buy it, finish the transaction immediately, and read
/// `Transaction.currentEntitlements` on every launch to recover it after a
/// reinstall. There is no server and no receipt validation — the only
/// integrity check is that StoreKit handed us a `.verified` transaction, which
/// is enforced here.
@objc final class IOSStoreKitCoordinator: NSObject, BillingStoreKitCoordinator {

    /// Live `Transaction` handles keyed by their JWS, so `finishTransaction`
    /// can complete one the Kotlin side only knows by string.
    private var pendingTransactions: [String: Transaction] = [:]
    private let lock = NSLock()

    func loadProducts(
        productIds: [String],
        onComplete: @escaping ([BillingStoreKitProduct], String?) -> Void
    ) {
        Task {
            do {
                let products = try await Product.products(for: Set(productIds))
                onComplete(products.map { $0.asKotlin() }, nil)
            } catch {
                onComplete([], error.localizedDescription)
            }
        }
    }

    func purchase(
        productId: String,
        onComplete: @escaping (BillingStoreKitPurchaseResult) -> Void
    ) {
        Task {
            do {
                guard let product = try await Product.products(for: [productId]).first else {
                    // Almost always means the product isn't provisioned yet.
                    onComplete(.failed("App Store doesn't know \(productId)"))
                    return
                }

                switch try await product.purchase() {
                case .success(let verification):
                    switch verification {
                    case .verified(let transaction):
                        self.retain(transaction, jws: verification.jwsRepresentation)
                        onComplete(transaction.asKotlin(
                            status: .success,
                            jws: verification.jwsRepresentation,
                            displayPrice: product.displayPrice
                        ))
                    case .unverified(_, let error):
                        // Refuse to unlock on a transaction StoreKit itself
                        // won't vouch for.
                        onComplete(.failed("Unverified transaction: \(error.localizedDescription)"))
                    }

                // Ask-to-Buy and similar. Not a grant and not a failure — the
                // transaction shows up in currentEntitlements once approved,
                // and the next silent refresh picks it up.
                case .pending:
                    onComplete(BillingStoreKitPurchaseResult(
                        status: .pending,
                        productId: productId,
                        transactionId: nil,
                        jwsRepresentation: nil,
                        purchasedAtEpochMs: 0,
                        displayPrice: nil,
                        errorMessage: "Waiting for approval"
                    ))

                case .userCancelled:
                    onComplete(BillingStoreKitPurchaseResult(
                        status: .userCancelled,
                        productId: productId,
                        transactionId: nil,
                        jwsRepresentation: nil,
                        purchasedAtEpochMs: 0,
                        displayPrice: nil,
                        errorMessage: nil
                    ))

                @unknown default:
                    onComplete(.failed("Unknown StoreKit purchase result"))
                }
            } catch {
                onComplete(.failed(error.localizedDescription))
            }
        }
    }

    /// `Transaction.currentEntitlements` — everything this Apple ID owns.
    ///
    /// Deliberately does **not** call `AppStore.sync()`: sync can prompt for a
    /// password, and this runs silently on every launch. Reading entitlements
    /// never prompts, which is what makes a reinstall recover the unlock with
    /// no user action at all.
    func loadCurrentEntitlements(
        onComplete: @escaping ([BillingStoreKitPurchaseResult]) -> Void
    ) {
        Task {
            var results: [BillingStoreKitPurchaseResult] = []
            for await verification in Transaction.currentEntitlements {
                guard case .verified(let transaction) = verification else { continue }
                self.retain(transaction, jws: verification.jwsRepresentation)
                results.append(transaction.asKotlin(
                    status: .alreadyPurchased,
                    jws: verification.jwsRepresentation,
                    displayPrice: nil
                ))
            }
            onComplete(results)
        }
    }

    /// The Restore button. `AppStore.sync()` may prompt for a password, so this
    /// is only ever reached from an explicit tap.
    func restorePurchases(
        onComplete: @escaping ([BillingStoreKitPurchaseResult], String?) -> Void
    ) {
        Task {
            do {
                try await AppStore.sync()
                self.loadCurrentEntitlements { results in onComplete(results, nil) }
            } catch {
                onComplete([], error.localizedDescription)
            }
        }
    }

    /// Apple replays an unfinished transaction forever. A non-consumable's
    /// entitlement lives on the account, so finishing immediately loses nothing.
    func finishTransaction(
        jwsRepresentation: String,
        // Boxed for the same reason as IOSAudioCapture's KotlinInt.
        onComplete: @escaping (KotlinBoolean) -> Void
    ) {
        Task {
            guard let transaction = self.take(jws: jwsRepresentation) else {
                onComplete(KotlinBoolean(bool: false))
                return
            }
            await transaction.finish()
            onComplete(KotlinBoolean(bool: true))
        }
    }

    private func retain(_ transaction: Transaction, jws: String) {
        lock.lock()
        defer { lock.unlock() }
        pendingTransactions[jws] = transaction
    }

    private func take(jws: String) -> Transaction? {
        lock.lock()
        defer { lock.unlock() }
        return pendingTransactions.removeValue(forKey: jws)
    }
}

private extension Product {
    func asKotlin() -> BillingStoreKitProduct {
        BillingStoreKitProduct(
            productId: id,
            displayPrice: displayPrice,
            currencyCode: priceFormatStyle.currencyCode,
            // The store's decimal price in micros, for telemetry only. Display
            // always uses `displayPrice`, which is already localized.
            priceMicros: Int64(truncating: NSDecimalNumber(decimal: price * 1_000_000))
        )
    }
}

private extension Transaction {
    func asKotlin(
        status: BillingStoreKitPurchaseStatus,
        jws: String,
        displayPrice: String?
    ) -> BillingStoreKitPurchaseResult {
        BillingStoreKitPurchaseResult(
            status: status,
            productId: productID,
            transactionId: String(id),
            jwsRepresentation: jws,
            purchasedAtEpochMs: Int64(purchaseDate.timeIntervalSince1970 * 1000),
            displayPrice: displayPrice,
            errorMessage: nil
        )
    }
}

private extension BillingStoreKitPurchaseResult {
    static func failed(_ message: String) -> BillingStoreKitPurchaseResult {
        BillingStoreKitPurchaseResult(
            status: .failed,
            productId: nil,
            transactionId: nil,
            jwsRepresentation: nil,
            purchasedAtEpochMs: 0,
            displayPrice: nil,
            errorMessage: message
        )
    }
}
