Sen ileri seviye bir yazılım mühendisisin.

CALISMA PRENSIPLERI

1. Windows ortaminda PowerShell ile calis.
2. Kod degisikliginden once mevcut dokumantasyon ve bu dosyayi oku.
3. Yeni kutuphane eklemeden once surum pinini ve gerekcesini bu dosyada guncelle.
4. Her davranis degisikliginde once test yaz, sonra minimum kodla gecer hale getir.
5. Her patch turundan sonra ilgili testleri calistir; genel dogrulamada tum build/test zincirini kos.
6. Secret ve release artefact dosyalarini repoda tutma; ornek dosya + lokal secret store kullan.

## Onayli Bagimliliklar
| Paket | Versiyon | Neden |
| ----- | -------- | ----- |
| Android Gradle Plugin | 9.1.0 | Android Developers AGP release notes kontrol edildi; repo bu pin ile lokal test/build dogruladi. |
| Kotlin + Compose Compiler plugin | 2.3.20 | Kotlin release kanali incelendi; repo pin'i mevcut Compose/Hilt/KSP akisi ile calisiyor, kurtarma kapsaminda gereksiz downgrade yapilmadi. |
| KSP | 2.3.6 | Kotlin pin'i ile uyumlu repo pin'i korunuyor; kod uretimi lokal build'de dogrulandi. |
| AndroidX Core / AppCompat / Activity / Lifecycle | 1.18.0 / 1.7.1 / 1.13.0 / 2.10.0 | AndroidX resmi release notlari kontrol edildi; mevcut pinler unit test ve debug compile ile gecerli. |
| Jetpack Compose BOM | 2026.03.00 | Compose BOM ailesi Android Developers kanalina gore izlendi; repo pin'i mevcut UI ve test setiyle uyumlu. |
| Navigation Compose | 2.9.7 | AndroidX navigation release notlari kontrol edildi; mevcut navigation graph'i ile stabil calisiyor. |
| Room | 2.8.4 | AndroidX Room release notlari kontrol edildi; migration ve DAO katmani bu pin ile dogrulandi. |
| DataStore | 1.2.1 | AndroidX DataStore stable kanali referans alindi; preferences tabanli session akisi bu surumle sorunsuz. |
| Hilt / AndroidX Hilt | 2.59.2 / 1.3.0 | Dagger Hilt ve AndroidX Hilt entegrasyonu mevcut DI + WorkManager zinciriyle dogrulandi. |
| WorkManager / Glance | 2.11.1 / 1.1.1 | Background work ve widget katmani resmi stable aileleriyle uyumlu, lokal build'de gecerli. |
| Firebase Android BoM + plugins | 34.10.0 / 4.4.4 / 3.0.6 | Firebase release notlari ve plugin kanali kontrol edildi; Auth, Messaging, Crashlytics ve Remote Config akislariyla birlikte calisiyor. |
| Google Mobile Ads / App Set / UMP / Play Billing | 25.1.0 / 16.1.0 / 4.0.0 / 8.3.0 | Google resmi release sayfalari incelendi; reklam ve subscription akisi bu pinlerle hizali. |
| OkHttp / Retrofit / kotlinx.serialization / Coroutines | 5.3.2 / 3.0.0 / 1.10.0 / 1.10.2 | Network katmani ve coroutine tabanli repository akislari lokal testte dogrulandi. |
| Coil / Lottie / Timber | 3.4.0 / 6.7.1 / 5.0.1 | UI medya/logging katmani icin mevcut pinler korunuyor; yeni kutuphane eklenmedi. |
| Detekt / ktlint Gradle / Play Publisher | 1.23.8 / 14.2.0 / 4.0.0 | Statik analiz ve release otomasyonu icin resmi plugin sayfalari kontrol edildi; gorevler lokal olarak tanimli. |
| JUnit / MockK / Turbine / Truth / Robolectric | 4.13.2 / 1.14.9 / 1.2.1 / 1.4.5 / 4.16.1 | Android unit test zinciri bu kombinasyonla gecerli. |
| Hono / jose / zod | 4.8.3 / 6.1.0 / 4.1.5 | Backend runtime kutuphaneleri resmi dokumantasyon ve npm paket kanallari uzerinden kontrol edildi; mevcut Worker davranisi ile uyumlu. |
| TypeScript / tsx / Vitest | 5.9.2 / 4.20.5 / 3.2.4 | Backend build ve test araci zinciri mevcut kodla birlikte yesil. |
| Wrangler / @cloudflare/workers-types | 4.37.1 / 4.20260301.0 | Cloudflare Workers resmi dokumani kontrol edildi; deploy config ve tipler bu pin ile dogrulandi. |

Not: 2026-03-26 tarihinde resmi kaynaklar kontrol edildi. Repo kurtarma gorevinde davranis disi versiyon degisikligi yapilmadi; mevcut pinler lokal build/test ile dogrulanan calisan baz cizgi olarak korundu.
