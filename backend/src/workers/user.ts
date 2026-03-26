import { Hono } from 'hono';

import { enforceRateLimit } from '@/services/cache';
import type { AppBindings, FcmTokenRow, UserProfileResponse, UserRow } from '@/types';
import { signAppJwt, verifyFirebaseIdToken } from '@/utils/jwt';
import { validateRegisterBody, validateUpdateUserBody } from '@/utils/validators';

function jsonError(status: number, code: string, message: string) {
  return Response.json({ error: { code, message } }, { status });
}

async function getUserByFirebaseUid(db: D1Database, firebaseUid: string): Promise<UserRow | null> {
  return (await db
    .prepare('SELECT * FROM users WHERE firebase_uid = ?')
    .bind(firebaseUid)
    .first()) as UserRow | null;
}

async function getUserById(db: D1Database, userId: string): Promise<UserRow | null> {
  return (await db.prepare('SELECT * FROM users WHERE id = ?').bind(userId).first()) as UserRow | null;
}

async function getUserToken(db: D1Database, userId: string): Promise<FcmTokenRow | null> {
  return (await db
    .prepare('SELECT * FROM fcm_tokens WHERE user_id = ? ORDER BY updated_at DESC LIMIT 1')
    .bind(userId)
    .first()) as FcmTokenRow | null;
}

async function upsertFcmToken(
  db: D1Database,
  args: {
    userId: string;
    token: string;
    notificationEnabled?: boolean;
    notificationHour: number;
  }
) {
  const now = new Date().toISOString();
  const existing = (await db
    .prepare('SELECT id FROM fcm_tokens WHERE token = ?')
    .bind(args.token)
    .first()) as { id: string } | null;

  if (existing) {
    await db
      .prepare(
        `UPDATE fcm_tokens
         SET user_id = ?, notification_enabled = COALESCE(?, notification_enabled), notification_hour = ?, updated_at = ?
         WHERE id = ?`
      )
      .bind(
        args.userId,
        typeof args.notificationEnabled === 'boolean' ? Number(args.notificationEnabled) : null,
        args.notificationHour,
        now,
        existing.id
      )
      .run();
    return;
  }

  await db
    .prepare(
      `INSERT INTO fcm_tokens (id, user_id, token, platform, notification_enabled, notification_hour, created_at, updated_at)
       VALUES (?, ?, ?, 'android', ?, ?, ?, ?)`
    )
    .bind(
      crypto.randomUUID(),
      args.userId,
      args.token,
      typeof args.notificationEnabled === 'boolean' ? Number(args.notificationEnabled) : 1,
      args.notificationHour,
      now,
      now
    )
    .run();
}

function toUserProfile(user: UserRow, token: FcmTokenRow | null): UserProfileResponse {
  return {
    user_id: user.id,
    sign: user.sign,
    language: user.language,
    utc_offset: user.utc_offset,
    is_premium: Boolean(user.is_premium),
    premium_expires_at: user.premium_expires_at,
    notification_enabled: Boolean(token?.notification_enabled ?? 1),
    notification_hour: token?.notification_hour ?? 9
  };
}

export function registerUserRoutes(app: Hono<AppBindings>) {
  app.post('/users/register', async (c) => {
    const ip = c.req.header('cf-connecting-ip') ?? 'unknown';
    const allowed = await enforceRateLimit(c.env, `register:${ip}`, 10, 60);
    if (!allowed) {
      return jsonError(429, 'RATE_LIMITED', 'Too many register attempts.');
    }

    const authHeader = c.req.header('authorization');
    if (!authHeader) {
      return jsonError(401, 'UNAUTHORIZED', 'Missing authorization header.');
    }

    const token = authHeader.split(' ')[1];
    if (!token) {
      return jsonError(401, 'UNAUTHORIZED', 'Missing authorization header.');
    }

    const body = validateRegisterBody(await c.req.json());
    const firebaseClaims = await verifyFirebaseIdToken(c.env, token);
    const firebaseUid = firebaseClaims.sub;
    const now = new Date().toISOString();

    let user = await getUserByFirebaseUid(c.env.DB, firebaseUid);
    if (user) {
      await c.env.DB
        .prepare(
          `UPDATE users
           SET sign = ?, language = ?, utc_offset = ?, last_seen_at = ?
           WHERE id = ?`
        )
        .bind(body.sign, body.language, body.utc_offset, now, user.id)
        .run();
    } else {
      const userId = crypto.randomUUID();
      await c.env.DB
        .prepare(
          `INSERT INTO users (id, firebase_uid, sign, language, utc_offset, is_premium, premium_expires_at, created_at, last_seen_at)
           VALUES (?, ?, ?, ?, ?, 0, NULL, ?, ?)`
        )
        .bind(userId, firebaseUid, body.sign, body.language, body.utc_offset, now, now)
        .run();
      user = await getUserById(c.env.DB, userId);
    }

    if (!user) {
      return jsonError(500, 'USER_SYNC_FAILED', 'Unable to create or load user.');
    }

    await upsertFcmToken(c.env.DB, {
      userId: user.id,
      token: body.fcm_token,
      notificationHour: body.notification_hour ?? 9
    });

    const refreshedUser = await getUserById(c.env.DB, user.id);
    if (!refreshedUser) {
      return jsonError(500, 'USER_SYNC_FAILED', 'Unable to load user after register.');
    }

    const jwt = await signAppJwt(c.env, {
      userId: refreshedUser.id,
      isPremium: Boolean(refreshedUser.is_premium),
      firebaseUid
    });

    return c.json({
      user_id: refreshedUser.id,
      jwt,
      is_premium: Boolean(refreshedUser.is_premium),
      premium_expires_at: refreshedUser.premium_expires_at
    });
  });

  app.get('/users/me', async (c) => {
    const user = await getUserById(c.env.DB, c.get('auth').userId);
    if (!user) {
      return jsonError(404, 'USER_NOT_FOUND', 'User was not found.');
    }

    const token = await getUserToken(c.env.DB, user.id);
    return c.json(toUserProfile(user, token));
  });

  app.put('/users/me', async (c) => {
    const body = validateUpdateUserBody(await c.req.json());
    const user = await getUserById(c.env.DB, c.get('auth').userId);
    if (!user) {
      return jsonError(404, 'USER_NOT_FOUND', 'User was not found.');
    }

    const now = new Date().toISOString();
    await c.env.DB
      .prepare('UPDATE users SET sign = ?, language = ?, utc_offset = ?, last_seen_at = ? WHERE id = ?')
      .bind(
        body.sign ?? user.sign,
        body.language ?? user.language,
        body.utc_offset ?? user.utc_offset,
        now,
        user.id
      )
      .run();

    if (body.fcm_token) {
      await upsertFcmToken(c.env.DB, {
        userId: user.id,
        token: body.fcm_token,
        notificationEnabled: body.notification_enabled,
        notificationHour: body.notification_hour ?? 9
      });
    } else if (
      typeof body.notification_enabled === 'boolean' ||
      typeof body.notification_hour === 'number'
    ) {
      await c.env.DB
        .prepare(
          `UPDATE fcm_tokens
           SET notification_enabled = COALESCE(?, notification_enabled),
               notification_hour = COALESCE(?, notification_hour),
               updated_at = ?
           WHERE user_id = ?`
        )
        .bind(
          typeof body.notification_enabled === 'boolean' ? Number(body.notification_enabled) : null,
          body.notification_hour ?? null,
          now,
          user.id
        )
        .run();
    }

    const refreshedUser = await getUserById(c.env.DB, user.id);
    const refreshedToken = await getUserToken(c.env.DB, user.id);
    return c.json(toUserProfile(refreshedUser as UserRow, refreshedToken));
  });
}
