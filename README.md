# Radio De Dios - Suggestions for Improvement

Here is a list of potential improvements and new features that could be added to the radio application to make it even better:

### 1. Advanced Audio Equalizer
Implement an audio equalizer using the Android `AudioEffect` API. Allow users to select presets (Pop, Rock, Classical, Spoken Word) or adjust custom bands to optimize their listening experience.

### 2. Next Song / Stream Polling
Currently, standard Icecast/Shoutcast streams do not broadcast "Next Song" natively via ID3/Icy metadata. If the radio station has a JSON API (e.g., AzuraCast or Centova Cast), implement a polling mechanism in `RadioService` to fetch the "Now Playing" and "Next Song" data directly from the server.

### 3. Alarm Clock Feature
In addition to the current Sleep Timer, add an Alarm feature that automatically wakes the device and starts playing the user's favorite radio station at a specific time.

### 4. Chromecast Support
Integrate the Google Cast SDK to allow users to cast the radio stream to their smart TVs, Google Home, or other Chromecast-enabled speakers directly from the player interface.

### 5. Offline Caching / Recording
Allow users to record the live radio stream to their local storage so they can listen to their favorite segments later while offline.

### 6. Enhanced Car Mode UI
The current Car Mode is great, but could be enhanced with Voice Commands (via speech recognition) to change stations without touching the screen, improving driving safety.

### 7. Android Auto Integration
Build a native `MediaBrowserServiceCompat` implementation specifically tailored for Android Auto, allowing users to browse their favorite stations directly from their car's dashboard interface.

### 8. Dynamic App Icon & Theming
Allow the user to select custom app icons or fully customize the app's primary color palette beyond the standard Light/Dark system themes (e.g., "Material You" dynamic colors).