# Moosic Android App (`http://music.rrbb.men`)

Native Android streaming wrapper for `http://music.rrbb.men` featuring:
- **HTTP Cleartext Support:** Configured with `usesCleartextTraffic="true"` and `network_security_config.xml` so Android allows HTTP connections, session cookies, and live `<audio>` streaming without HTTPS.
- **Automatic Port Detection (`8000` vs `80`):** Probes `http://music.rrbb.men:8000/` and `http://music.rrbb.men/` on launch and loads whichever HTTP port is open.
- **Background Audio Keep-Alive Service:** Uses a Foreground Media Playback service, partial CPU `WakeLock`, and `WifiLock` so music continues streaming when your screen is off or you switch apps.
- **Persistent Personal Room & Private Playlists:** Automatically provisions and persists a private `personal_room_id` (`900000000`–`999999999`) in Android `SharedPreferences` and passes the `MoosicAndroidApp/1.0` User-Agent so your personal playlists remain private to your phone.
