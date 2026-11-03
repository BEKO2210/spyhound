# IOC hardening

STIX accepts only complete equality comparisons and their OR combinations. Revoked indicators and unsupported
conditions are skipped. CSV retains quote state across newlines and discards malformed or oversized records.
CSV/TSV fields are bounded before row construction (64 fields, 8192 characters each); every indicator parser rejects
sources above 200,000 indicators instead of returning a partial result.

Both curated assets have SHA-256 pins and whole-source KnownGood rejection. Unreadable or rejected curated assets
appear in the existing corrupt-source report. Source totals now include these two assets (46 instead of 44 catalog
sources); update downloads still use only the catalog. Catalog commits require exactly 40 lowercase hex characters.

Validation: 7 parser/catalog regression failures and 2 curated-source regression failures reproduced before their
fixes. Direct JVM compilation and execution passed 60 tests across IocHardeningTest, CoreTest, ScanIocEdgeCasesTest,
AntivirusTest and IocStoreDebugTest, including noShippedSourceHitsTheKnownGoodList. The standalone run used the Android
API jar, a temporary BuildConfig and copies of the two ScanEngine text constants; Android lifecycle code was not run.
All 32 bundled source counts remain pinned at their original values, totaling 8,296 parsed indicators.

Gradle, APK builds and device tests were not run. Next validation: full unit suite and debug build; on the device,
check the source ratio includes curated assets and normal scans retain their previous findings. No asset changed.
