/**
 * The SDK and JVM levels every module in this build compiles and runs against.
 */
const val COMPILE_SDK = 37

const val MIN_SDK = 31

const val TARGET_SDK = 37

const val JDK_VERSION = 17

/**
 * Robolectric needs a newer runtime than the compile toolchain to emulate the compiled SDK.
 */
const val TEST_JDK_VERSION = 21
