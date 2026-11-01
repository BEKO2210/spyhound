# Shizuku user service is instantiated by class name from the shell process.
-keep class de.belkis.spyhound.shell.ShellUserService { *; }
-keep class de.belkis.spyhound.shell.IShellService* { *; }

# libadb loads the Conscrypt provider by name; the certificate builder sets fields by name.
-keep class org.conscrypt.** { *; }
-dontwarn org.conscrypt.**
-keep class sun.security.** { *; }
-keep class io.github.muntashirakon.adb.** { *; }
-dontwarn com.android.org.conscrypt.**
