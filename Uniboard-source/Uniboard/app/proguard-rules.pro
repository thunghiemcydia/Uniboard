# Keep the declared Android entry points explicit in optimized release builds.
-keep class com.vi.keyboa.MainActivity { *; }
-keep class com.vi.keyboa.VietIme { *; }

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
