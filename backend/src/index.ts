import { Hono } from 'hono';
import { ZodError } from 'zod';

import { adminSecretMiddleware, jwtAuthMiddleware } from '@/middleware/auth';
import { corsMiddleware } from '@/middleware/cors';
import { enforceRateLimit } from '@/services/cache';
import type { AppBindings } from '@/types';
import { validateTrackEventBody } from '@/utils/validators';
import { registerContentRoutes } from '@/workers/content';
import { handleCron } from '@/workers/cron';
import { registerNotificationRoutes } from '@/workers/notification';
import { registerSubscriptionRoutes } from '@/workers/subscription';
import { registerUserRoutes } from '@/workers/user';

function jsonError(status: number, code: string, message: string) {
  return Response.json({ error: { code, message } }, { status });
}

export function createApp() {
  const app = new Hono<AppBindings>();

  app.use('*', corsMiddleware);
  app.use('*', async (c, next) => {
    c.set('requestId', crypto.randomUUID());
    c.set('isAdmin', false);
    c.header('x-request-id', c.get('requestId'));
    await next();
  });

  app.onError((error) => {
    if (error instanceof ZodError) {
      return jsonError(
        400,
        'INVALID_REQUEST',
        error.issues[0]?.message ?? 'Request validation failed.'
      );
    }

    if (error instanceof SyntaxError) {
      return jsonError(400, 'INVALID_REQUEST', 'Request body must be valid JSON.');
    }

    console.error(error);
    return jsonError(500, 'INTERNAL_ERROR', 'An unexpected server error occurred.');
  });

  app.get('/api/v1/health', async (c) => {
    await c.env.DB.prepare('SELECT 1 AS ok').first();
    await c.env.CONTENT.head('healthcheck');

    return c.json({
      status: 'ok',
      timestamp: new Date().toISOString()
    });
  });

  const apiRoutes = new Hono<AppBindings>();
  const apiAdminRoutes = new Hono<AppBindings>();

  apiRoutes.use('/users/me', jwtAuthMiddleware);
  apiRoutes.use('/content/*', jwtAuthMiddleware);
  apiRoutes.use('/content/*', async (c, next) => {
    const allowed = await enforceRateLimit(c.env, `content:${c.get('auth').userId}`, 60, 60);
    if (!allowed) {
      return jsonError(429, 'RATE_LIMITED', 'Too many content requests.');
    }
    await next();
  });
  apiRoutes.use('/subscriptions/verify', jwtAuthMiddleware);
  apiRoutes.use('/subscriptions/restore', jwtAuthMiddleware);
  apiRoutes.use('/events/track', jwtAuthMiddleware);

  registerUserRoutes(apiRoutes);
  registerContentRoutes(apiRoutes);
  registerSubscriptionRoutes(apiRoutes);

  apiRoutes.post('/events/track', async (c) => {
    const body = validateTrackEventBody(await c.req.json());
    await c.env.DB
      .prepare(
        `INSERT INTO user_events (id, user_id, event_type, meta, created_at)
         VALUES (?, ?, ?, ?, ?)`
      )
      .bind(
        crypto.randomUUID(),
        c.get('auth').userId,
        body.event_type,
        JSON.stringify(body.meta ?? {}),
        new Date().toISOString()
      )
      .run();
    return c.json({ ok: true });
  });

  apiAdminRoutes.use('*', adminSecretMiddleware);
  registerNotificationRoutes(apiAdminRoutes);

  app.route('/api/v1', apiRoutes);
  app.route('/api/v1', apiAdminRoutes);

  return app;
}

const app = createApp();

export default {
  fetch: app.fetch,
  scheduled: handleCron
};
