# Firebase synchronizace

## Co je připravené v aplikaci

- Firebase Anonymous Authentication bez přihlašovacího formuláře.
- Realtime Database pod `combinations/{id}`.
- Synchronizace pouze změněných kombinací.
- Offline fronta a disková persistence Firebase SDK.
- Lokální JSON zůstává zálohou a aplikace funguje i bez Firebase konfigurace.
- Konflikt stejné kombinace řeší `updatedAt`, při shodném čase rozhoduje `updatedBy`.

## Nastavení Firebase projektu

1. V Firebase Console vytvoř nebo vyber projekt.
2. Přidej Android aplikaci s package name `com.example.straidlov_alchimie_kalkulaka`.
3. Stáhni `google-services.json` do `app/google-services.json`.
4. V Authentication povol poskytovatele Anonymous.
5. V Realtime Database nasaď pravidla z `firebase.database.rules.json`.
6. Při distribuci mimo Google Play zvaž zapnutí App Check s Play Integrity. App Check nemění způsob distribuce APK.

Soubor `google-services.json` obsahuje projektovou konfiguraci, ne Firebase Admin credentials. Do aplikace ani repozitáře nepatří service-account JSON, privátní klíč ani univerzální tajný token.
