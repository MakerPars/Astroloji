# Astrology Backend

Cloudflare Workers tabanli astroloji backend'i. Bu klasor, Android istemciyi besleyen API, subscription webhook ve cron job logic'ini icerir.

## Stack

- TypeScript + Hono
- Cloudflare Workers
- D1 + R2 + KV
- Firebase Cloud Messaging HTTP v1
- Google Play Developer API Subscriptions v2

## Tek Kaynakli Deploy Config

Deploy icin sadece `wrangler.toml` kullanilir. Ayrik deploy config tutulmaz.

## Hizli Baslangic

```powershell
cd backend
npm ci
npm test
npm run build
```

## Lokal Secret Akisi

- Ornek dosya: `.dev.vars.example`
- Gercek lokal dosya: `.dev.vars`
- Onerilen akis: `npm run doppler:devvars`

Cloudflare secret store'a giden anahtarlar:

- `JWT_SECRET`
- `GOOGLE_SERVICE_ACCOUNT_JSON`
- `FIREBASE_SERVICE_ACCOUNT_JSON`
- `PLAY_WEBHOOK_SECRET`
- `ADMIN_SECRET`

## Scripts

- `npm run dev`
- `npm run dev:doppler`
- `npm run build`
- `npm test`
- `npm run doppler:devvars`
- `npm run doppler:cf-secrets`
- `npm run deploy:doppler`
- `npm run schema:apply`
- `npm run seed`

## API ve Guvenlik

- `POST /api/v1/users/register` Firebase ID token ister.
- `GET/PUT /api/v1/users/me` uygulama JWT'si ister.
- `GET /api/v1/content/*` uygulama JWT'si ister.
- `POST /api/v1/subscriptions/verify` uygulama JWT'si ister.
- `POST /api/v1/subscriptions/restore` uygulama JWT'si ister.
- `POST /api/v1/events/track` uygulama JWT'si ister.
- `POST /api/v1/webhooks/play-rtdn` sadece `X-Play-Secret` ile korunur.
- `POST /api/v1/notifications/send` sadece `X-Admin-Secret` ile korunur.

## Cache ve Rate Limit

- Cache key: `content:{lang}:{type}:{identifier}`
- TTL: daily 23 saat, weekly 6 gun, monthly 27 gun, compat/personality 30 gun
- Rate limit:
  - `/users/register`: IP basina dakikada 10
  - `/content/*`: kullanici basina dakikada 60
  - `/subscriptions/verify`: kullanici basina dakikada 5

## Notlar

- RTDN webhook parse akisi tip guvenli tutulur; eksik payload durumunda 400 doner.
- FCM tarafinda `registration-token-not-registered` / `UNREGISTERED` token'lari otomatik silinir.
- Daha genis deploy, rotate ve smoke-check adimlari icin repo kokundeki `README.md` dosyasina bak.
