import { describe, expect, it } from 'vitest';

import {
  filterCompatibilityContent,
  filterDailyContent,
  filterMonthlyContent,
  filterPersonalityContent,
  filterWeeklyContent
} from '@/workers/content';
import {
  buildFallbackSubscriptionResponse,
  decodeWebhookPayload,
  extractSubscriptionNotification
} from '@/workers/subscription';

describe('content filters', () => {
  it('returns only free fields for daily content', () => {
    const result = filterDailyContent(
      {
        short: 'Bugun enerjin yuksek.',
        full: 'Uzun premium yorum',
        love: 'Premium love',
        career: 'Premium career',
        money: 'Premium money',
        health: 'Premium health',
        lucky_number: 7,
        lucky_color: 'Kirmizi',
        energy: 85,
        love_score: 70,
        career_score: 90,
        daily_tip: 'Premium tip'
      },
      false
    );

    expect(result).toEqual({
      short: 'Bugun enerjin yuksek.',
      lucky_number: 7,
      lucky_color: 'Kirmizi',
      energy: 85,
      love_score: 70,
      career_score: 90
    });
  });

  it('returns premium fields untouched for daily content', () => {
    const source = {
      short: 'Bugun enerjin yuksek.',
      full: 'Uzun premium yorum',
      love: 'Premium love',
      career: 'Premium career',
      money: 'Premium money',
      health: 'Premium health',
      lucky_number: 7,
      lucky_color: 'Kirmizi',
      energy: 85,
      love_score: 70,
      career_score: 90,
      daily_tip: 'Premium tip'
    };

    expect(filterDailyContent(source, true)).toEqual(source);
  });

  it('applies free field slicing to weekly, monthly, compatibility and personality content', () => {
    expect(
      filterWeeklyContent(
        {
          summary: 'Weekly summary',
          love: 'love',
          career: 'career',
          money: 'money',
          best_day: 'Wednesday',
          warning: 'warning'
        },
        false
      )
    ).toEqual({ summary: 'Weekly summary' });

    expect(
      filterMonthlyContent(
        {
          summary: 'Monthly summary',
          love: 'love',
          career: 'career',
          money: 'money',
          best_day: 'Friday',
          warning: 'warning'
        },
        false
      )
    ).toEqual({ summary: 'Monthly summary' });

    expect(
      filterCompatibilityContent(
        {
          sign1: 'aries',
          sign2: 'leo',
          language: 'tr',
          overall_score: 87,
          love_score: 92,
          friendship_score: 80,
          work_score: 75,
          summary: 'Good match',
          strengths: ['a'],
          challenges: ['b'],
          advice: 'Premium advice',
          famous_couples: ['x']
        },
        false
      )
    ).toEqual({
      overall_score: 87,
      summary: 'Good match'
    });

    expect(
      filterPersonalityContent(
        {
          sign: 'aries',
          language: 'tr',
          title: 'Koc',
          summary: 'Short summary',
          deep_analysis: 'Premium analysis',
          strengths: ['bold'],
          weaknesses: ['impatient'],
          ideal_partners: ['leo'],
          career_fit: ['founder'],
          element: 'ates',
          planet: 'Mars',
          color: 'Kirmizi',
          stone: 'Yakut'
        },
        false
      )
    ).toEqual({
      summary: 'Short summary',
      element: 'ates',
      planet: 'Mars',
      color: 'Kirmizi',
      stone: 'Yakut'
    });
  });

  it('decodes wrapped play webhook payloads and extracts subscription details', () => {
    const encoded = Buffer.from(
      JSON.stringify({
        subscriptionNotification: {
          purchaseToken: 'purchase-token',
          subscriptionId: 'premium_monthly',
          notificationType: 4
        }
      }),
      'utf8'
    ).toString('base64');

    const decoded = decodeWebhookPayload({
      message: {
        data: encoded
      }
    });

    expect(extractSubscriptionNotification(decoded)).toEqual({
      purchaseToken: 'purchase-token',
      productId: 'premium_monthly',
      notificationType: 4
    });
  });

  it('creates a typed fallback subscription response for webhook-only updates', () => {
    expect(
      buildFallbackSubscriptionResponse('premium_yearly', 'purchase-token', '2026-03-18T09:00:00.000Z')
    ).toEqual({
      linkedPurchaseToken: 'purchase-token',
      startTime: '2026-03-18T09:00:00.000Z',
      lineItems: [
        {
          productId: 'premium_yearly',
          expiryTime: '2026-03-18T09:00:00.000Z'
        }
      ]
    });
  });
});
