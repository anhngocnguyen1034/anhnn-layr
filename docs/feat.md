Hi Claude, please implement Insert Background from Gallery and Background Blur features into our Jetpack Compose image editing application.

1. Requirements for Inserting Background from Gallery
   Gallery Picker: Use rememberLauncherForActivityResult with PickVisualMedia() to allow the user to select an image from their device gallery.

State Management: Store the selected background image as a backgroundBitmap (Bitmap?) in the ViewModel. If it is null, fallback to the solid color or transparent background.

UI Render: In the Preview Box, render this backgroundBitmap directly behind the transparent processedBitmap (the subject). Ensure the background image is scaled to fit or fill the preview area properly.

2. Requirements for Background Blur
   Core Logic: Apply a blur effect to the background layer (either the solid color background or the image imported from the gallery).

Implementation: Use a fast blur algorithm (such as a custom Gaussian Blur matrix, or Android's native Toolkit.blur / RenderEffect.createBlurEffect for API 31+) to process the background layer before rendering the subject on top.

UI Control: Add a Slider in the control panel to adjust the blur intensity dynamically from 0 (no blur) to 25 (heavy blur, simulating a DSLR bokeh effect).

3. Final Image Export Update
   Update the generateFinalBitmap function to correctly composite the layers:

Draw the background image (with the applied blur effect, if any).

Draw the solid background color (if chosen).

Draw the edited subject Bitmap on top.

Save the final merged tệp as a high-quality JPEG/PNG using MediaStore.

4. Output
   Provide clean, modular, and production-ready Kotlin code that integrates these features into the existing Composable UI and ViewModel. No explanations needed, just give me the code.