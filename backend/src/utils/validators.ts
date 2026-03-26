import { z } from 'zod';

import {
  LANGUAGES,
  SIGNS,
  SUBSCRIPTION_PRODUCTS,
  USER_EVENT_TYPES,
  type Language,
  type NotificationRequest,
  type RegisterRequest,
  type Sign,
  type SubscriptionVerifyRequest,
  type TrackEventRequest,
  type UpdateUserRequest
} from '@/types';

const signSchema = z.enum(SIGNS);
const languageSchema = z.enum(LANGUAGES);
const productSchema = z.enum(SUBSCRIPTION_PRODUCTS);
const eventTypeSchema = z.enum(USER_EVENT_TYPES);
const notificationHourSchema = z.number().int().min(0).max(23);
const utcOffsetSchema = z.number().int().min(-12).max(14);

export const registerSchema = z.object({
  sign: signSchema,
  language: languageSchema.default('tr'),
  fcm_token: z.string().min(1),
  notification_hour: notificationHourSchema.optional().default(9),
  utc_offset: utcOffsetSchema
});

export const updateUserSchema = z
  .object({
    sign: signSchema.optional(),
    language: languageSchema.optional(),
    fcm_token: z.string().min(1).optional(),
    notification_enabled: z.boolean().optional(),
    notification_hour: notificationHourSchema.optional(),
    utc_offset: utcOffsetSchema.optional()
  })
  .refine(
    (value) => Object.keys(value).length > 0,
    'At least one field must be provided.'
  );

export const subscriptionVerifySchema = z.object({
  purchase_token: z.string().min(1),
  product_id: productSchema
});

export const trackEventSchema = z.object({
  event_type: eventTypeSchema,
  meta: z.record(z.string(), z.unknown()).optional().default({})
});

export const notificationSchema = z.object({
  user_id: z.string().uuid().optional(),
  sign: signSchema.optional(),
  title: z.string().min(1),
  body: z.string().min(1),
  data: z.record(z.string(), z.unknown()).optional().default({})
});

export function validateSign(value: string): Sign {
  return signSchema.parse(value);
}

export function validateLanguage(value: string): Language {
  return languageSchema.parse(value);
}

export function validateRegisterBody(payload: unknown): RegisterRequest {
  return registerSchema.parse(payload);
}

export function validateUpdateUserBody(payload: unknown): UpdateUserRequest {
  return updateUserSchema.parse(payload);
}

export function validateSubscriptionBody(payload: unknown): SubscriptionVerifyRequest {
  return subscriptionVerifySchema.parse(payload);
}

export function validateTrackEventBody(payload: unknown): TrackEventRequest {
  return trackEventSchema.parse(payload);
}

export function validateNotificationBody(payload: unknown): NotificationRequest {
  return notificationSchema.parse(payload);
}

export function parseBooleanFlag(value: string | undefined): boolean {
  return value === '1' || value?.toLowerCase() === 'true';
}

export function normalizeCompatibilityPair(sign1: string, sign2: string) {
  const normalized = [validateSign(sign1), validateSign(sign2)].sort();
  return {
    normalizedSign1: normalized[0],
    normalizedSign2: normalized[1],
    key: `${normalized[0]}-${normalized[1]}`
  };
}

export function sanitizeNotificationData(data?: Record<string, unknown>): Record<string, string> {
  if (!data) {
    return {};
  }

  return Object.entries(data).reduce<Record<string, string>>((acc, [key, value]) => {
    if (typeof value === 'string') {
      acc[key] = value;
    }
    return acc;
  }, {});
}
