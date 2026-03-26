# Astroloji Repo

Android istemcisi ve Cloudflare Worker backend'inden olusan bu repo, gunluk/haftalik/aylik astroloji icerigi, premium abonelik, bildirim ve analytics akislarini birlikte tasir.

## Kapsam

- `Astroloji/`: Jetpack Compose tabanli Android uygulamasi
- `backend/`: Hono + Cloudflare Workers backend'i
- `scripts/`: repo seviyesinde yardimci dogrulama scriptleri

Bu repo yeni bir web/admin paneli icermiyor. Public API path'leri korunur:

- `/api/v1/users/*`
- `/api/v1/content/*`
- `/api/v1/subscriptions/*`
- `/api/v1/notifications/send`

## Mimari Ozet

- Android tarafinda Hilt, Room, DataStore, Retrofit/OkHttp, Firebase, WorkManager, Glance widget ve Play Billing kullanilir.
- Backend tarafinda Hono route katmani, D1 veritabani, R2 content store, KV cache/rate limit, Firebase FCM ve Google Play Developer API entegrasyonu vardir.
- Doppler, backend secret'lari icin source of truth olarak kullanilir.

## Lokal Kurulum

### Gereksinimler

- JDK 21
- Node.js 24+
- npm 10+
- Wrangler CLI
- Doppler CLI (secret sync kullaniyorsaniz)

### Android

1. `Astroloji/gradle.properties.example` dosyasini referans alip lokal `gradle.properties` veya kullanici-level Gradle properties tanimlayin.
2. Gercek `google-services.json` dosyasini `Astroloji/app/google-services.json` yoluna koyun.
3. Test veya CI icin gercek Firebase config'iniz yoksa ornek dosyayi kopyalayin:

```powershell
Copy-Item Astroloji/app/google-services.example.json Astroloji/app/google-services.json
```

4. Calistirma ve test:

```powershell
cd Astroloji
.\gradlew.bat test
.\gradlew.bat :app:assembleDebug
```

### Backend

1. `backend/.dev.vars.example` dosyasini referans alin.
2. Doppler kullaniyorsaniz:

```powershell
cd backend
npm ci
npm run doppler:devvars
```

3. Lokal test ve build:

```powershell
cd backend
npm ci
npm test
npm run build
```

## Secret Yonetimi

Repoda gercek secret tutulmaz. Asagidaki dosyalar lokal/CI secret store uzerinden uretilmelidir:

- `Astroloji/app/google-services.json`
- `backend/.dev.vars`
- Google Play service account JSON dosyasi

### Android Gradle property anahtarlari

- `ADMOB_APP_ID`
- `ADMOB_BANNER_ID`
- `ADMOB_INTERSTITIAL_ID`
- `ADMOB_REWARDED_ID`
- `ADMOB_REWARDED_INTERSTITIAL_ID`
- `ADMOB_APP_OPEN_ID`
- `ADMOB_NATIVE_ADVANCED_ID`
- `ADMOB_USE_TEST_IDS`
- `PLAY_TRACK`
- `PLAY_SERVICE_ACCOUNT_JSON_PATH`
- `PRIVACY_POLICY_URL`
- `TERMS_OF_USE_URL`
- `SUPPORT_EMAIL`

### Backend secret anahtarlari

- `JWT_SECRET`
- `GOOGLE_SERVICE_ACCOUNT_JSON`
- `FIREBASE_SERVICE_ACCOUNT_JSON`
- `PLAY_WEBHOOK_SECRET`
- `ADMIN_SECRET`

## Deploy Akisi

### Android release

1. Release Gradle properties ve signing bilgilerini CI secret store'a koy.
2. `PLAY_SERVICE_ACCOUNT_JSON_PATH` ile Play Publisher hesabini bagla.
3. `PLAY_TRACK=internal` ile ic dagitimdan basla.
4. Release artifact'i repoya commit etme; CI veya lokal secure output'ta uret.

### Backend release

Tek kaynak `backend/wrangler.toml` dosyasidir. Ayrik deploy config tutulmaz.

```powershell
cd backend
npm ci
npm run build
npm test
npm run deploy:doppler
```

## CI/CD

GitHub Actions workflow:

- backend icin `npm ci`, `npm run build`, `npm test`
- Android icin secret scan, ornek `google-services.json` kopyalama, `detekt`, `ktlintCheck`, `testDebugUnitTest`, `assembleDebug`
- `content-backfill` workflow'u her gun 01:15 UTC'de hafif seed calistirir; varsayilan olarak `SEED_DAILY_DAYS=14` ve `SEED_SKIP_STATIC_CONTENT=true` kullanir

## Smoke Check

Deploy sonrasi minimum dogrulama:

1. `GET /api/v1/health` 200 donmeli.
2. Register + profile acilisi zinciri calismali.
3. En az bir `daily` content cagrisinda veri donmeli.
4. Subscription verify veya RTDN fixture akisi hata vermemeli.
5. Android debug build acilip Home -> Daily -> Premium navigation'i calismali.

## Icerik Backfill Operasyonu

Gelecek tarihli `daily/weekly/monthly` dosyalarini ucuz sekilde guncellemek icin seed araci hafif mod destekler:

```powershell
cd backend
$env:SEED_DATE='2026-03-26'
$env:SEED_DAILY_DAYS='14'
$env:SEED_SKIP_STATIC_CONTENT='true'
npm run seed
```

Kurallar:

- `SEED_DATE` bos birakilirsa bugunden baslar.
- `SEED_DAILY_DAYS` pozitif integer olmalidir.
- `SEED_SKIP_STATIC_CONTENT=true` oldugunda `personality` ve `compat` dosyalari yeniden yuklenmez.
- GitHub Actions `content-backfill` workflow'u ayni akisi zamanlanmis olarak calistirir.

Gerekli GitHub secret'lari:

- `CLOUDFLARE_API_TOKEN`

## Secret Rotation Checklist

- Firebase service account rotate et.
- Google Play service account rotate et.
- `JWT_SECRET` rotate et.
- `PLAY_WEBHOOK_SECRET` rotate et.
- `ADMIN_SECRET` rotate et.
- Rotate sonrasi Doppler ve Cloudflare secret store senkronunu tekrarla.

## Rollback

- Android: son saglam internal track build'ine don.
- Backend: son saglam Worker deploy'una don ve secret setini o release ile hizala.
- Veritabani semasi degisikliginde rollback oncesi D1 snapshot kontrol et.

## Operasyon Notu

- Ayrintili release ve deploy uygulama adimlari icin `RELEASE_RUNBOOK.md` dosyasini kullanin.
