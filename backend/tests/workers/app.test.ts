import { describe, expect, it } from 'vitest';

import { createApp } from '@/index';
import type { Env } from '@/types';

function createEnv(): Env {
  return {
    DB: {
      prepare() {
        return {
          bind() {
            return this;
          },
          async first() {
            return { ok: 1 };
          },
          async all() {
            return { results: [] };
          },
          async run() {
            return { success: true, meta: {} };
          }
        };
      },
      async batch() {
        return [];
      }
    } as unknown as D1Database,
    CONTENT: {
      async head() {
        return { size: 1 } as R2Object;
      },
      async get() {
        return null;
      }
    } as unknown as R2Bucket,
    CACHE: {
      async get() {
        return null;
      },
      async put() {
        return;
      },
      async delete() {
        return;
      }
    } as unknown as KVNamespace,
    ENVIRONMENT: 'test',
    PACKAGE_NAME: 'com.example.astrology',
    PREMIUM_MONTHLY_PRODUCT_ID: 'premium_monthly',
    PREMIUM_YEARLY_PRODUCT_ID: 'premium_yearly',
    ALLOWED_ORIGINS: 'https://yourdomain.com',
    JWT_SECRET: 'super-secret',
    GOOGLE_SERVICE_ACCOUNT_JSON: JSON.stringify({
      client_email: 'play@example.iam.gserviceaccount.com',
      private_key: 'FAKE_TEST_PLAY_PRIVATE_KEY',
      token_uri: 'https://oauth2.googleapis.com/token',
      project_id: 'demo-project'
    }),
    FIREBASE_SERVICE_ACCOUNT_JSON: JSON.stringify({
      client_email: 'firebase@example.iam.gserviceaccount.com',
      private_key: 'FAKE_TEST_FIREBASE_PRIVATE_KEY',
      token_uri: 'https://oauth2.googleapis.com/token',
      project_id: 'demo-project'
    }),
    PLAY_WEBHOOK_SECRET: 'play-secret',
    ADMIN_SECRET: 'admin-secret'
  };
}

describe('app routes', () => {
  it('returns health status when dependencies are reachable', async () => {
    const app = createApp();
    const response = await app.request('/api/v1/health', {}, createEnv());

    expect(response.status).toBe(200);
    await expect(response.json()).resolves.toMatchObject({
      status: 'ok'
    });
  });

  it('requires firebase authorization for user registration', async () => {
    const app = createApp();
    const response = await app.request(
      '/api/v1/users/register',
      {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({
          sign: 'aries',
          language: 'tr',
          fcm_token: 'token-1',
          notification_hour: 9,
          utc_offset: 3
        })
      },
      createEnv()
    );

    expect(response.status).toBe(401);
    await expect(response.json()).resolves.toEqual({
      error: {
        code: 'UNAUTHORIZED',
        message: 'Missing authorization header.'
      }
    });
  });

  it('returns a validation error when registration receives an empty json body', async () => {
    const app = createApp();
    const response = await app.request(
      '/api/v1/users/register',
      {
        method: 'POST',
        headers: {
          'content-type': 'application/json',
          authorization: 'Bearer invalid-token'
        },
        body: ''
      },
      createEnv()
    );

    expect(response.status).toBe(400);
    await expect(response.json()).resolves.toEqual({
      error: {
        code: 'INVALID_REQUEST',
        message: 'Request body must be valid JSON.'
      }
    });
  });
});
