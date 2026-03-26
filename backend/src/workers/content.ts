import { Hono } from 'hono';

import { getCachedJsonContent } from '@/services/cache';
import type {
  AppBindings,
  CompatibilityContentDocument,
  DailyContentDocument,
  DailySignContent,
  Language,
  MonthlyContentDocument,
  MonthlySignContent,
  PersonalityContentDocument,
  WeeklyContentDocument,
  WeeklySignContent
} from '@/types';
import { getDateIdentifier, getMonthIdentifier, getWeekIdentifier } from '@/utils/date';
import {
  normalizeCompatibilityPair,
  parseBooleanFlag,
  validateLanguage,
  validateSign
} from '@/utils/validators';

function jsonError(status: number, code: string, message: string) {
  return Response.json({ error: { code, message } }, { status });
}

export function filterDailyContent(content: DailySignContent, isPremium: boolean) {
  if (isPremium) {
    return content;
  }

  return {
    short: content.short,
    lucky_number: content.lucky_number,
    lucky_color: content.lucky_color,
    energy: content.energy,
    love_score: content.love_score,
    career_score: content.career_score
  };
}

export function filterWeeklyContent(content: WeeklySignContent, isPremium: boolean) {
  return isPremium ? content : { summary: content.summary };
}

export function filterMonthlyContent(content: MonthlySignContent, isPremium: boolean) {
  return isPremium ? content : { summary: content.summary };
}

export function filterCompatibilityContent(
  content: CompatibilityContentDocument,
  isPremium: boolean
) {
  if (isPremium) {
    return content;
  }

  return {
    overall_score: content.overall_score,
    summary: content.summary
  };
}

export function filterPersonalityContent(content: PersonalityContentDocument, isPremium: boolean) {
  if (isPremium) {
    return content;
  }

  return {
    summary: content.summary,
    element: content.element,
    planet: content.planet,
    color: content.color,
    stone: content.stone
  };
}

export function registerContentRoutes(app: Hono<AppBindings>) {
  app.get('/content/daily', async (c) => {
    const sign = validateSign(c.req.query('sign') ?? '');
    const language = validateLanguage(c.req.query('lang') ?? 'tr');
    const date = c.req.query('date') ?? getDateIdentifier();
    const bypassCache =
      parseBooleanFlag(c.req.header('x-cache-bypass')) &&
      c.req.header('x-admin-secret') === c.env.ADMIN_SECRET;

    const document = await getCachedJsonContent<DailyContentDocument>(c.env, {
      language,
      type: 'daily',
      identifier: date,
      r2Key: `content/daily/${language}/${date}.json`,
      bypassCache
    });

    if (!document) {
      return jsonError(404, 'CONTENT_NOT_FOUND', 'Daily content was not found.');
    }

    return c.json({
      date: document.date,
      language,
      sign,
      ...filterDailyContent(document.signs[sign], c.get('auth').isPremium)
    });
  });

  app.get('/content/weekly', async (c) => {
    const sign = validateSign(c.req.query('sign') ?? '');
    const language = validateLanguage(c.req.query('lang') ?? 'tr');
    const week = c.req.query('week') ?? getWeekIdentifier();
    const bypassCache =
      parseBooleanFlag(c.req.header('x-cache-bypass')) &&
      c.req.header('x-admin-secret') === c.env.ADMIN_SECRET;

    const document = await getCachedJsonContent<WeeklyContentDocument>(c.env, {
      language,
      type: 'weekly',
      identifier: week,
      r2Key: `content/weekly/${language}/${week}.json`,
      bypassCache
    });

    if (!document) {
      return jsonError(404, 'CONTENT_NOT_FOUND', 'Weekly content was not found.');
    }

    return c.json({
      week: document.week,
      week_start: document.week_start,
      week_end: document.week_end,
      language,
      sign,
      ...filterWeeklyContent(document.signs[sign], c.get('auth').isPremium)
    });
  });

  app.get('/content/monthly', async (c) => {
    const sign = validateSign(c.req.query('sign') ?? '');
    const language = validateLanguage(c.req.query('lang') ?? 'tr');
    const month = c.req.query('month') ?? getMonthIdentifier();
    const bypassCache =
      parseBooleanFlag(c.req.header('x-cache-bypass')) &&
      c.req.header('x-admin-secret') === c.env.ADMIN_SECRET;

    const document = await getCachedJsonContent<MonthlyContentDocument>(c.env, {
      language,
      type: 'monthly',
      identifier: month,
      r2Key: `content/monthly/${language}/${month}.json`,
      bypassCache
    });

    if (!document) {
      return jsonError(404, 'CONTENT_NOT_FOUND', 'Monthly content was not found.');
    }

    return c.json({
      month: document.month,
      month_start: document.month_start,
      month_end: document.month_end,
      language,
      sign,
      ...filterMonthlyContent(document.signs[sign], c.get('auth').isPremium)
    });
  });

  app.get('/content/personality', async (c) => {
    const sign = validateSign(c.req.query('sign') ?? '');
    const language = validateLanguage(c.req.query('lang') ?? 'tr');
    const bypassCache =
      parseBooleanFlag(c.req.header('x-cache-bypass')) &&
      c.req.header('x-admin-secret') === c.env.ADMIN_SECRET;

    const document = await getCachedJsonContent<PersonalityContentDocument>(c.env, {
      language,
      type: 'personality',
      identifier: sign,
      r2Key: `content/personality/${language}/${sign}.json`,
      bypassCache
    });

    if (!document) {
      return jsonError(404, 'CONTENT_NOT_FOUND', 'Personality content was not found.');
    }

    return c.json({
      sign,
      language,
      ...filterPersonalityContent(document, c.get('auth').isPremium)
    });
  });

  app.get('/content/compat', async (c) => {
    const language = validateLanguage(c.req.query('lang') ?? 'tr');
    const sign1 = c.req.query('sign1') ?? '';
    const sign2 = c.req.query('sign2') ?? '';
    const normalized = normalizeCompatibilityPair(sign1, sign2);
    const bypassCache =
      parseBooleanFlag(c.req.header('x-cache-bypass')) &&
      c.req.header('x-admin-secret') === c.env.ADMIN_SECRET;

    const document = await getCachedJsonContent<CompatibilityContentDocument>(c.env, {
      language,
      type: 'compat',
      identifier: normalized.key,
      r2Key: `content/compat/${language}/${normalized.key}.json`,
      bypassCache
    });

    if (!document) {
      return jsonError(404, 'CONTENT_NOT_FOUND', 'Compatibility content was not found.');
    }

    return c.json({
      sign1: normalized.normalizedSign1,
      sign2: normalized.normalizedSign2,
      language,
      ...filterCompatibilityContent(document, c.get('auth').isPremium)
    });
  });
}
