Hi Claude, please implement the Background Blur feature for our Jetpack Compose image editing application. This feature will keep the original image background but blur it out completely to simulate a DSLR bokeh/depth-of-field effect.

1. Requirements for Background Blur Logic
   Background Layer: The background will be the originalBitmap (the full image before background removal).

Blur Implementation: Create a helper function to blur the originalBitmap. Use a modern, high-performance blurring approach suitable for Jetpack Compose/Android:

For API 31+ (Android 12+): You can use native RenderEffect.createBlurEffect.

For backward compatibility (API 21-30): Implement an efficient Gaussian Blur bitmap processor (e.g., using a custom matrix or a lightweight custom blur shader/toolkit).

UI Control: Add a Slider in the control panel to dynamically adjust the blur radius/intensity from 0 (no blur, original image) to 25 (heavy DSLR blur effect).

State Management: Keep track of the blurIntensity (Float) in the ViewModel and trigger the blur processing asynchronously (using LaunchedEffect or coroutines) to prevent UI freezing on the main thread.

2. Canvas Rendering & Image Export Update
   In the Preview Canvas: 1. Render the blurred originalBitmap at the bottom layer.
2. Render the edited/transparent processedBitmap (the isolated subject) directly on top, aligned perfectly with the original coordinates.

Update generateFinalBitmap: Modify the final compositing function so that when the user saves the image, the canvas draws the blurred original background first, then stamps the high-res transparent subject bitmap on top before exporting to MediaStore as a high-quality JPEG.

3. Output
   Provide clean, modular, and production-ready Kotlin code (ViewModel updates, Blur utility function, and Composable UI changes). No long explanations, just give me the code.