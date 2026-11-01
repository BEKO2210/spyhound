package de.belkis.spyhound.shell;

// Runs inside Shizuku's process (uid 2000 = ADB shell, or 0 = root). Output is streamed
// through a pipe so large logs do not hit the 1 MB binder transaction limit.
interface IShellService {
    ParcelFileDescriptor exec(String command, long timeoutSec) = 1;
    int uid() = 2;
    void destroy() = 16777114; // reserved by Shizuku for unbinding
}
