#!/usr/bin/env python3
"""
Download the TV OneVoice logo from the conversation and save it.
This is a helper script to save the logo image provided by the user.
"""

import base64
import os

# The TV OneVoice logo image data (base64 encoded PNG)
# This will be populated with the actual image data
LOGO_BASE64 = ""

def save_logo(output_path):
    """Save the logo image to the specified path."""
    if not LOGO_BASE64:
        print("❌ Error: Logo data not embedded in script")
        print("Please manually save the logo as 'new_logo.png' in the project root")
        return False
    
    try:
        # Decode base64 and save
        logo_data = base64.b64decode(LOGO_BASE64)
        with open(output_path, 'wb') as f:
            f.write(logo_data)
        print(f"✓ Logo saved to: {output_path}")
        return True
    except Exception as e:
        print(f"❌ Error saving logo: {e}")
        return False

if __name__ == '__main__':
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.join(script_dir, 'new_logo.png')
    save_logo(output_path)
