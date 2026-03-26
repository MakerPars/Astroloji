# Release Runbook

Bu dokuman `v1.0.0` ve sonraki release'ler icin operasyon ekibinin takip edecegi tek sayfalik uygulama notudur.

## Kapsam

- Android uygulama release hazirligi
- Cloudflare Worker backend deploy adimlari
- Deploy sonrasi smoke check
- Rollback proseduru

## Preflight Checklist

Release almadan once su maddeler `PASS` olmali:

1. `main` branch temiz olmali.
2. Son GitHub Actions `ci` kosusu yesil olmali.
3. GitHub security alerts acik olmamali.
4. Android icin gerekli secret ve signing dosyalari hazir olmali.
5. Backend icin Doppler ve Cloudflare secret'lari guncel olmali.
6. R2 future content backfill son 14 gun araligini kapsiyor olmali.

## Gerekli Secret'lar

### Android

- `google-services.json`
- signing keystore ve signing property'leri
- `PLAY_SERVICE_ACCOUNT_JSON_PATH`
- `PLAY_TRACK`
- AdMob property'leri

### Backend

- `JWT_SECRET`
- `GOOGLE_SERVICE_ACCOUNT_JSON`
- `FIREBASE_SERVICE_ACCOUNT_JSON`
- `PLAY_WEBHOOK_SECRET`
- `ADMIN_SECRET`
- `CLOUDFLARE_API_TOKEN`

## Android Release Adimlari

1. `Astroloji/app/google-services.json` dosyasinin gercek config ile saglandigini dogrula.
2. Release Gradle property'lerinin CI veya lokal secure store'da oldugunu kontrol et.
3. Lokal on dogrulama calistir:

```powershell
cd Astroloji
.\gradlew.bat :app:detekt
.\gradlew.bat :app:ktlintCheck
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

4. Internal track publish gerekiyorsa Play Publisher ayarlarinin dogru oldugunu kontrol et.
5. Release artifact'i repoya commit etme.

## Backend Deploy Adimlari

1. Doppler secret'larinin guncel oldugunu kontrol et.
2. Lokal on dogrulama calistir:

```powershell
cd backend
npm ci
npm run build
npm test
```

3. Deploy:

```powershell
cd backend
npm run deploy:doppler
```

4. Deploy sonrasi Worker versiyonunu not et.

## Icerik Backfill Adimlari

Deploy sonrasi ya da takvim ilerlediginde future content eksigi varsa:

```powershell
cd backend
$env:SEED_DATE='2026-03-26'
$env:SEED_DAILY_DAYS='14'
$env:SEED_SKIP_STATIC_CONTENT='true'
npm run seed
```

GitHub Actions uzerinden alternatif:

- `content-backfill` workflow'unu manuel tetikle
- Gerekirse `seed_date` ve `daily_days` input'larini doldur

## Smoke Check

### Backend

1. `GET https://astrology.parsfilo.com/api/v1/health` -> `200`
2. `register` ve `users/me` zinciri -> `200`
3. `content/personality` -> `200`
4. `content/compat` -> `200`
5. En az bir gelecek tarihli `content/daily` -> `200`

### Android

1. Uygulama acilisinda crash olmamali.
2. Home ekranindan Daily ekranina gecis calismali.
3. Manual refresh stale cache'i asarak yeni veri istemeli.
4. Premium ekranina navigation calismali.

## Rollback

### Android

1. Play Console uzerinden son saglam internal release'e don.
2. Gerekirse yeni rollout'u durdur.

### Backend

1. Son saglam Worker deploy versiyonunu belirle.
2. Gerekirse onceki release commit'ine donup yeniden `npm run deploy:doppler` calistir.
3. Secret seti release ile uyumlu kalmali; eski config'e donerken secret drift kontrolu yap.

## Operasyon Sonrasi Kayit

Her release sonunda su bilgiler saklanmali:

- Release tag
- Git commit SHA
- Cloudflare Worker deploy ID
- Android artifact/build numarasi
- Smoke check sonucu
- Geri alma gerekiyorsa rollback nedeni

