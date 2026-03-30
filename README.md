# aSlicer

aSlicer is a 3D model processing tool designed to handle STL and 3MF file formats.

## Features
- Load and display STL (ASCII and Binary) models.
- Load and display 3MF models with material color support.
- Configurable 3D scene controls (Rotate, Pan, Zoom).
- Desktop UI with Clear and Open functionality.
- Persists application state between runs (window size, camera position, last used directory, etc.).
- Secure storage of sensitive data (passwords, access codes) using FIPS-compliant encryption.

## Security
aSlicer uses **Bouncy Castle FIPS** as its primary security provider to ensure high standards of data protection. Sensitive data, such as printer access codes and pairing codes, are encrypted using **AES-GCM (256-bit)** before being stored in configuration files. 

The encryption key is safely managed in a **BCFKS (Bouncy Castle FIPS KeyStore)** located in the user's configuration folder (`~/.aslicer/aslicer.bcfks`). Users can enable or disable this protection in the application settings.

## License
This project is licensed under the **GNU Affero General Public License, version 3 (AGPLv3)**.
See the [LICENSE](LICENSE) file for the full license text.

---
Copyright (C) 2026 cz.ad.print3d.aslicer contributors
