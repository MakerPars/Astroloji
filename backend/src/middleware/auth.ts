import type { Next } from 'hono';

import type { AppContext, AppMiddleware, Env } from '@/types';
import { verifyAppJwt } from '@/utils/jwt';

function jsonError(c: AppContext, status: number, code: string, message: string) {
  return c.json({ error: { code, message } }, { status: status as 401 | 403 });
}

export function getBearerToken(value: string | undefined | null): string | null {
  if (!value) {
    return null;
  }

  const [scheme, token] = value.split(' ');
  if (scheme?.toLowerCase() !== 'bearer' || !token) {
    return null;
  }

  return token;
}

export const jwtAuthMiddleware: AppMiddleware = async (c, next: Next) => {
  const token = getBearerToken(c.req.header('authorization'));
  if (!token) {
    return jsonError(c, 401, 'UNAUTHORIZED', 'Missing authorization header.');
  }

  try {
    const claims = await verifyAppJwt(c.env, token);
    c.set('auth', {
      userId: claims.user_id,
      isPremium: claims.is_premium,
      firebaseUid: claims.firebase_uid,
      exp: claims.exp
    });
    await next();
  } catch {
    return jsonError(c, 401, 'INVALID_TOKEN', 'Authorization token is invalid or expired.');
  }
};

export const adminSecretMiddleware: AppMiddleware = async (c, next: Next) => {
  const adminSecret = c.req.header('x-admin-secret');
  if (!adminSecret || adminSecret !== c.env.ADMIN_SECRET) {
    return jsonError(c, 403, 'FORBIDDEN', 'Admin secret is invalid.');
  }

  c.set('isAdmin', true);
  await next();
};

export function requirePlayWebhookSecret(c: AppContext): Response | null {
  const secret = c.req.header('x-play-secret');
  if (!secret || secret !== c.env.PLAY_WEBHOOK_SECRET) {
    return jsonError(c, 403, 'FORBIDDEN', 'Play webhook secret is invalid.');
  }

  return null;
}
