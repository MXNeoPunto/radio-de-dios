# Radio JSON Structure Documentation

The app loads radio station data from a JSON file. This file can be hosted online or stored locally.

## Structure

The JSON root object contains `app` info, `radios` list, and `banner` configuration.

### Banner Configuration (New)

You can configure a banner image to appear at the top of the home screen.

```json
  "banner": {
    "enabled": true,
    "image": "https://example.com/banner.jpg"
  }
```

- `enabled`: `true` to show the banner, `false` to hide it.
- `image`: URL of the image to display. Recommended size: 300x100 or adaptable width. The banner is non-clickable.

### Radio Stations

List of radio stations.

```json
  "radios": [
    {
      "id": 1,
      "name": "Radio Name",
      "image": "https://example.com/logo.png",
      "stream_url": "https://stream.example.com/live",
      "description": "Description of the radio",
      "popular": true,
      "language": "es"
    }
  ]
```

- `id`: Unique integer ID.
- `name`: Name of the station.
- `image`: Logo URL.
- `stream_url`: Stream URL (mp3, aac, etc).
- `description`: Subtitle or description.
- `popular`: `true` to show a star icon.
- `language`: `es` for Spanish, `en` for English.

### App Info

```json
  "app": {
    "version": "1.0",
    "force_update": false
  }
```

## Backward Compatibility

Older versions of the app will ignore the `banner` field, so it is safe to add it to your existing JSON.
