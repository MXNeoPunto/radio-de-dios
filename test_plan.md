1. **Create `PrayerModeActivity` and `PrayerGenerator` logic:**
   - Create `PrayerGenerator.java` to handle the local, offline dynamic generation of prayers based on questions.
   - It will have predefined arrays of phrases for different categories (health, family, finances, anxiety, spiritual strength).
   - The user inputs: target person name/description, category, and an optional brief text.
   - Generate prayers without repeating consecutively. Store a history of generated prayers using SharedPreferences (or SQLite/JSON file, but SharedPreferences storing a JSON string array is lightweight and sufficient for a simple history).
   - Create `PrayerModeActivity.java` (edge-to-edge Android 16 compatible) to host the flow:
     - Form to ask questions.
     - Result screen displaying generated prayer and random bible verse based on category.
     - Buttons: "Amen" (local counter), "Blessings" (motivational toast/snackbar), "Share" (Intent share), "Listen" (TextToSpeech).
     - Access to history.
2. **Create UI Layouts for Prayer Mode:**
   - `activity_prayer_mode.xml`: The main activity layout, modern design, floating cards. Handle safe insets for edge-to-edge.
   - `dialog_prayer_history.xml`: A dialog or separate activity/fragment to view saved prayers.
3. **Update Main Menu:**
   - Add a new "Prayer Mode" (Modo Oración) button in `dialog_modern_menu.xml`.
   - Update `MainActivity.java`'s `showModernMenu()` to bind the new button and launch `PrayerModeActivity`.
4. **Strings & Localization:**
   - Add string resources for English and Spanish for the Prayer Mode flow (titles, hints, buttons, categories, verses, phrases for generation).
5. **Assets:**
   - Create an icon for the menu, e.g., `ic_pray.xml`.
6. **Pre-commit checks**
   - Run `pre_commit_instructions` tool to make sure tests pass.
