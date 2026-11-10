# MainActivity's LAUNCH_EXAMPLE extra, which the nightly uses to open each example, loads examples by
# class name and calls their Context constructor.
-keep class * extends com.pspdfkit.catalog.SdkExample {
    public <init>(android.content.Context);
}

# JJWT loads jjwt-impl and its Jackson serializer by class name, so R8 can't see them used. Without
# this, the JWT the AI Assistant and Instant examples generate fails with "Unable to load class
# named [io.jsonwebtoken.impl.DefaultJwtBuilder]".
-keep class io.jsonwebtoken.** { *; }
