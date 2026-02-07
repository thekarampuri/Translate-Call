#!/usr/bin/env python3
"""
Script to copy and resize the TV OneVoice logo to all Android launcher icon locations.
Run this script after placing the new_logo.png file in the project root.
"""

from PIL import Image
import os
import sys

# Define the sizes for each density
ICON_SIZES = {
    'mdpi': 48,
    'hdpi': 72,
    'xhdpi': 96,
    'xxhdpi': 144,
    'xxxhdpi': 192
}

def create_launcher_icons(source_image_path, output_base_path):
    """Create launcher icons in all required densities."""
    
    if not os.path.exists(source_image_path):
        print(f"❌ Error: Source image not found at {source_image_path}")
        print(f"Please place your logo image as 'new_logo.png' in the project root.")
        return False
    
    print(f"📁 Loading source image: {source_image_path}")
    
    try:
        source_img = Image.open(source_image_path)
        print(f"✓ Source image loaded: {source_img.size} - {source_img.mode}")
    except Exception as e:
        print(f"❌ Error loading source image: {e}")
        return False
    
    # Convert to RGBA if needed
    if source_img.mode != 'RGBA':
        print(f"Converting from {source_img.mode} to RGBA...")
        source_img = source_img.convert('RGBA')
    
    # Generate icons for each density
    for density, size in ICON_SIZES.items():
        print(f"\n🎨 Generating {density} icons ({size}x{size})...")
        
        output_dir = os.path.join(output_base_path, f'mipmap-{density}')
        os.makedirs(output_dir, exist_ok=True)
        
        # Resize image with high-quality resampling
        resized_img = source_img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save ic_launcher.png
        ic_launcher_path = os.path.join(output_dir, 'ic_launcher.png')
        resized_img.save(ic_launcher_path, 'PNG', optimize=True)
        print(f"  ✓ {ic_launcher_path}")
        
        # Save ic_launcher_round.png
        ic_launcher_round_path = os.path.join(output_dir, 'ic_launcher_round.png')
        resized_img.save(ic_launcher_round_path, 'PNG', optimize=True)
        print(f"  ✓ {ic_launcher_round_path}")
        
        # Create ic_launcher_foreground.png with proper padding for adaptive icons
        # Adaptive icons use a 108dp canvas with 72dp safe zone
        # We need to scale the icon to fit within the safe zone
        foreground_canvas_size = int(size * 1.5)  # 108/72 = 1.5
        foreground_img = Image.new('RGBA', (foreground_canvas_size, foreground_canvas_size), (0, 0, 0, 0))
        
        # Center the resized logo on the canvas
        paste_x = (foreground_canvas_size - size) // 2
        paste_y = (foreground_canvas_size - size) // 2
        foreground_img.paste(resized_img, (paste_x, paste_y), resized_img)
        
        # Resize back to target size
        foreground_img = foreground_img.resize((size, size), Image.Resampling.LANCZOS)
        
        ic_launcher_foreground_path = os.path.join(output_dir, 'ic_launcher_foreground.png')
        foreground_img.save(ic_launcher_foreground_path, 'PNG', optimize=True)
        print(f"  ✓ {ic_launcher_foreground_path}")
    
    print("\n✅ All launcher icons generated successfully!")
    print("\n📝 Next steps:")
    print("   1. Clean and rebuild the project")
    print("   2. Uninstall the old app from your device")
    print("   3. Install the new build to see the updated icon")
    return True

if __name__ == '__main__':
    # Determine paths
    script_dir = os.path.dirname(os.path.abspath(__file__))
    source_image = os.path.join(script_dir, 'new_logo.png')
    output_base = os.path.join(script_dir, 'app', 'src', 'main', 'res')
    
    # Run the generator
    success = create_launcher_icons(source_image, output_base)
    sys.exit(0 if success else 1)
