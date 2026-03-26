import type { ExecutionContext, ScheduledController } from '@cloudflare/workers-types';

import { sendBatchNotifications } from '@/services/fcm';
import { getSubscriptionStatus } from '@/services/playBilling';
import type {
  CronNotificationJob,
  DailyContentDocument,
  Env,
  NotificationTargetRow,
  SubscriptionRow
} from '@/types';
import { getCurrentUtcHour, getDateIdentifier, shouldSendNotificationAtUtcHour } from '@/utils/date';

const SIGN_LABELS = {
  tr: {
    aries: 'Koc',
    taurus: 'Boga',
    gemini: 'Ikizler',
    cancer: 'Yengec',
    leo: 'Aslan',
    virgo: 'Basak',
    libra: 'Terazi',
    scorpio: 'Akrep',
    sagittarius: 'Yay',
    capricorn: 'Oglak',
    aquarius: 'Kova',
    pisces: 'Balik'
  },
  en: {
    aries: 'Aries',
    taurus: 'Taurus',
    gemini: 'Gemini',
    cancer: 'Cancer',
    leo: 'Leo',
    virgo: 'Virgo',
    libra: 'Libra',
    scorpio: 'Scorpio',
    sagittarius: 'Sagittarius',
    capricorn: 'Capricorn',
    aquarius: 'Aquarius',
    pisces: 'Pisces'
  }
} as const;

async function expireAndRefreshSubscriptions(env: Env) {
  const now = new Date().toISOString();
  const expiring = await env.DB.prepare(
    `SELECT * FROM subscriptions WHERE status = 'active' AND expires_at < ?`
  )
    .bind(now)
    .all<SubscriptionRow>();

  for (const subscription of expiring.results ?? []) {
    const current = await getSubscriptionStatus(
      env,
      subscription.purchase_token,
      subscription.product_id,
      env.PACKAGE_NAME
    );

    if (current?.status === 'active') {
      await env.DB
        .prepare(
          `UPDATE subscriptions
           SET expires_at = ?, status = ?, auto_renewing = ?, cancel_reason = ?, updated_at = ?
           WHERE id = ?`
        )
        .bind(
          current.expiresAt,
          current.status,
          Number(current.autoRenewing),
          current.cancelReason,
          now,
          subscription.id
        )
        .run();
      await env.DB
        .prepare('UPDATE users SET is_premium = 1, premium_expires_at = ? WHERE id = ?')
        .bind(current.expiresAt, subscription.user_id)
        .run();
      continue;
    }

    await env.DB
      .prepare(`UPDATE subscriptions SET status = 'expired', updated_at = ? WHERE id = ?`)
      .bind(now, subscription.id)
      .run();
    await env.DB
      .prepare('UPDATE users SET is_premium = 0, premium_expires_at = ? WHERE id = ?')
      .bind(subscription.expires_at, subscription.user_id)
      .run();
    await env.DB
      .prepare(
        `INSERT INTO subscription_events (id, user_id, purchase_token, event_type, payload, created_at)
         VALUES (?, ?, ?, 'expired', ?, ?)`
      )
      .bind(
        crypto.randomUUID(),
        subscription.user_id,
        subscription.purchase_token,
        JSON.stringify({ source: 'cron' }),
        now
      )
      .run();
  }
}

async function buildNotificationJob(
  env: Env,
  target: NotificationTargetRow
): Promise<CronNotificationJob | null> {
  const date = getDateIdentifier();
  const object = await env.CONTENT.get(`content/daily/${target.language}/${date}.json`);
  if (!object) {
    return null;
  }

  const document = (await object.json()) as DailyContentDocument;
  const entry = document.signs[target.sign];
  const title =
    target.language === 'tr'
      ? `${SIGN_LABELS.tr[target.sign]} Burcu Bugun`
      : `${SIGN_LABELS.en[target.sign]} Horoscope Today`;

  return {
    title,
    body: entry.short,
    data: {
      type: 'daily',
      sign: target.sign,
      date
    }
  };
}

async function dispatchScheduledNotifications(env: Env, currentUtcHour: number) {
  const targets = await env.DB.prepare(
    `SELECT u.id AS user_id, u.sign, u.language, u.utc_offset, f.token, f.notification_hour
     FROM users u
     INNER JOIN fcm_tokens f ON f.user_id = u.id
     WHERE f.notification_enabled = 1`
  ).all<NotificationTargetRow>();

  const groupedJobs = new Map<string, string[]>();

  for (const target of targets.results ?? []) {
    if (!shouldSendNotificationAtUtcHour(target.notification_hour, target.utc_offset, currentUtcHour)) {
      continue;
    }

    const job = await buildNotificationJob(env, target);
    if (!job) {
      continue;
    }

    const key = JSON.stringify(job);
    const tokens = groupedJobs.get(key) ?? [];
    tokens.push(target.token);
    groupedJobs.set(key, tokens);
  }

  for (const [serializedJob, tokens] of groupedJobs.entries()) {
    const job = JSON.parse(serializedJob) as CronNotificationJob;
    await sendBatchNotifications(env, tokens, job.title, job.body, job.data);
  }
}

export async function handleCron(
  controller: ScheduledController,
  env: Env,
  _ctx: ExecutionContext
) {
  if (controller.cron === '0 * * * *') {
    await expireAndRefreshSubscriptions(env);
    return;
  }

  if (controller.cron === '0 9 * * *' || controller.cron === '0 20 * * *') {
    await dispatchScheduledNotifications(env, getCurrentUtcHour());
  }
}
