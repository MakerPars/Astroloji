import type { Env, GooglePlaySubscription, GooglePlaySubscriptionResponse, SubscriptionStatus } from '@/types';
import { createGoogleAccessToken } from '@/utils/jwt';

const GOOGLE_PLAY_SCOPE = 'https://www.googleapis.com/auth/androidpublisher';

function mapSubscriptionState(raw: string | undefined): SubscriptionStatus {
  switch (raw) {
    case 'SUBSCRIPTION_STATE_ACTIVE':
    case 'SUBSCRIPTION_STATE_IN_GRACE_PERIOD':
    case 'SUBSCRIPTION_STATE_ON_HOLD':
      return 'active';
    case 'SUBSCRIPTION_STATE_PAUSED':
      return 'paused';
    case 'SUBSCRIPTION_STATE_CANCELED':
      return 'cancelled';
    case 'SUBSCRIPTION_STATE_EXPIRED':
    case 'SUBSCRIPTION_STATE_PENDING_PURCHASE_CANCELED':
    case 'SUBSCRIPTION_STATE_REVOKED':
      return 'expired';
    default:
      return 'expired';
  }
}

function normalizeSubscription(
  purchaseToken: string,
  productId: string,
  raw: GooglePlaySubscriptionResponse
): GooglePlaySubscription | null {
  const lineItem = raw.lineItems?.find((item) => item.productId === productId) ?? raw.lineItems?.[0];
  if (!lineItem?.expiryTime) {
    return null;
  }

  return {
    purchaseToken,
    productId: lineItem.productId ?? productId,
    status: mapSubscriptionState(raw.subscriptionState),
    startsAt: raw.startTime ?? new Date().toISOString(),
    expiresAt: lineItem.expiryTime,
    autoRenewing: Boolean(lineItem.autoRenewingPlan),
    cancelReason: raw.canceledStateContext?.cancellationReason ?? null,
    raw
  };
}

async function playFetch<T>(env: Env, url: string, init?: RequestInit): Promise<T> {
  const accessToken = await createGoogleAccessToken(env.GOOGLE_SERVICE_ACCOUNT_JSON, GOOGLE_PLAY_SCOPE);
  const response = await fetch(url, {
    ...init,
    headers: {
      authorization: `Bearer ${accessToken}`,
      'content-type': 'application/json',
      ...(init?.headers ?? {})
    }
  });

  if (!response.ok) {
    if (response.status === 404) {
      return null as T;
    }
    throw new Error(`Google Play API request failed with ${response.status}.`);
  }

  if (response.status === 204) {
    return null as T;
  }

  return (await response.json()) as T;
}

export async function getSubscriptionStatus(
  env: Env,
  purchaseToken: string,
  productId: string,
  packageName: string
): Promise<GooglePlaySubscription | null> {
  const raw = await playFetch<GooglePlaySubscriptionResponse | null>(
    env,
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(
      packageName
    )}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`
  );

  if (!raw) {
    return null;
  }

  return normalizeSubscription(purchaseToken, productId, raw);
}

export async function verifySubscriptionPurchase(
  env: Env,
  purchaseToken: string,
  productId: string,
  packageName: string
): Promise<GooglePlaySubscription | null> {
  return getSubscriptionStatus(env, purchaseToken, productId, packageName);
}

export async function cancelSubscription(
  env: Env,
  purchaseToken: string,
  _productId: string,
  packageName: string
): Promise<boolean> {
  await playFetch(
    env,
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(
      packageName
    )}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}:cancel`,
    {
      method: 'POST',
      body: JSON.stringify({
        cancellationContext: {
          cancellationType: 'USER_REQUESTED_STOP_RENEWALS'
        }
      })
    }
  );

  return true;
}
