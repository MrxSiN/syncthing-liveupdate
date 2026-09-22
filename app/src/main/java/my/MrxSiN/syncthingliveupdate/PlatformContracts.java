package my.MrxSiN.syncthingliveupdate;

import android.os.Build;

/**
 * Picks the {@link PlatformContract} for the release the module is running on.
 *
 * The choice is made once per process and cached, because every caller has to see
 * the same contract and {@code Build.VERSION.SDK_INT} cannot change underneath
 * them.
 */
final class PlatformContracts {

    private static final PlatformContract NEWEST_KNOWN = new Android37Contract();

    private static volatile PlatformContract current;

    private PlatformContracts() {
    }

    /** The contract for this device. */
    static PlatformContract current() {
        PlatformContract contract = current;
        if (contract == null) {
            synchronized (PlatformContracts.class) {
                contract = current;
                if (contract == null) {
                    contract = select(Build.VERSION.SDK_INT);
                    current = contract;
                }
            }
        }
        return contract;
    }

    /** Visible for testing; the selection rule without the cache. */
    static PlatformContract select(int sdk) {
        if (sdk == Android36Contract.SDK) {
            return new Android36Contract();
        }
        if (sdk == Android37Contract.SDK) {
            return new Android37Contract();
        }
        return new UnknownContract(NEWEST_KNOWN, sdk);
    }

    /** Visible for testing; drops the cached contract. */
    static void reset() {
        synchronized (PlatformContracts.class) {
            current = null;
        }
    }
}
