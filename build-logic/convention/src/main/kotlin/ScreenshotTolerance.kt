/**
 * Share of a render's pixels allowed to differ from its golden.
 *
 * macOS and the Linux CI runner render the same preview one antialiased pixel apart, well inside
 * this, while any real visual change moves thousands of pixels.
 */
const val SCREENSHOT_IMAGE_DIFFERENCE_THRESHOLD = 0.00001f
