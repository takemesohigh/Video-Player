# Next Player

Next Player is an Android native video player written in Kotlin. It provides a simple and easy-to-use interface for users to play videos on their
Android devices

## Supported formats

- **Video**: H.263, H.264 AVC (Baseline Profile; Main Profile on Android 6+), H.265 HEVC, MPEG-4 SP, VP8, VP9, AV1
    - Support depends on Android device
- **Audio**: Vorbis, Opus, FLAC, ALAC, PCM/WAVE (μ-law, A-law), MP1, MP2, MP3, AMR (NB, WB), AAC (LC, ELD, HE; xHE on Android 9+), AC-3, E-AC-3, DTS,
  DTS-HD, TrueHD
    - Support provided by ExoPlayer FFmpeg extension
- **Subtitles**: SRT, SSA, ASS, TTML, VTT, DVB
    - SSA/ASS has limited styling support see [this issue](https://github.com/google/ExoPlayer/issues/8435)

## Features

- Native Android app with simple and easy-to-use interface
- Completely free and open source and without any ads or excessive permissions
- Software decoders for h264 and hevc
- Audio/Subtitle track selection
- Vertical swipe to change brightness (left) / volume (right)
- Horizontal swipe to seek through video
- [Material 3 (You)](https://m3.material.io/) support
- Media picker with tree, folder and file view modes
- Play videos from url
- Play videos from storage access framework (Android Document picker)
- Control playback speed
- External Subtitle support
- Zoom gesture
- Picture-in-picture mode — **removed in this fork** (see Note below)
- Background playback
- Android TV version
- Search Functionality
- Subtitle delay
- Network storage support (SMB/FTP/SFTP/WebDAV)

## Note

The code is derived from anilbeesetti/nextplayer at v0.18.0 under GPL-3.0, with minSdk lowered back to 23 (Android 6.0) for wider device support.

Picture-in-picture support has been removed in this fork. Upstream's PiP implementation depends on `androidx.core:core-pip`, which declares minSdk 24 in its own manifest and is incompatible with this fork's minSdk 23. Since PiP itself requires Android 8.0 (API 26) regardless of this library, dropping it costs nothing on this fork's minimum-supported devices — it only affects API 26+ devices, which would otherwise have had PiP available. If you fork this project and want PiP support restored on API 26+ while keeping minSdk 23 for older devices, you'll need to reimplement it using the framework's `android.app.PictureInPictureParams` API directly instead of `core-pip`.

## Translations

This fork ships with English strings only; all other locale translations have been removed to keep the project maintainable for a single maintainer. If you fork this repository and want translations, you're responsible for adding them yourself.

The source strings to translate from are in [`core/ui/src/main/res/values/strings.xml`](core/ui/src/main/res/values/strings.xml) (and other modules' `values/strings.xml` files, where applicable). Android Studio's built-in [Translations Editor](https://developer.android.com/studio/write/translations-editor) is the easiest way to add a new locale — right-click any `strings.xml` file, select **Open editor**, and use **Add Locale**.

## License

Next Player is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for more information.
