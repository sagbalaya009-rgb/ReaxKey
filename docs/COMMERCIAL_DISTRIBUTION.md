# Commercial Distribution

## 1. Commercial model

ReaxKey is planned as a **one-time paid Android app on Google Play**. A user pays once to acquire the app from Google Play and receives the complete product.

The product deliberately does **not** use:

- subscriptions,
- advertising,
- required ReaxKey accounts,
- a free-vs-Pro feature split,
- recurring cloud service fees for core functionality.

## 2. Purchase architecture

The base commercial model is a **paid Play Store listing**, not an in-app purchase. Therefore the Android implementation should not add Google Play Billing Library merely to unlock the app. Google Play controls whether a normal customer can acquire the paid listing.

Development builds may be installed directly by the owner. This is separate from commercial entitlement.

If stronger licensing, additional paid content, or optional purchases are ever desired, treat that as a new architecture decision. Do not quietly add online license checks to the keyboard.

## 3. Privacy implications

Payment must not undermine the local-first design. Core typing, media indexing, browsing, search, favorites, collections and rich-content insertion should continue to work without a ReaxKey backend.

Do not add Internet permission solely for monetization in the initial product.

## 4. Play Store release rules

Before first production upload, verify against current Google Play documentation:

1. final application ID (`com.reaxkey.keyboard`),
2. required target SDK/API level,
3. Android App Bundle and Play App Signing requirements,
4. developer verification/testing-track requirements applicable to the account,
5. Data Safety disclosures,
6. privacy-policy requirements for an IME/keyboard,
7. media permission declarations and policy constraints,
8. supported countries/currencies and final one-time price.

Do not hard-code a sale price in source code. Price and regional pricing are Play Console configuration.

## 5. Release channels

- **Local development:** debug APK.
- **Owner/device QA:** debug or internally signed APK.
- **Pre-release Play QA:** internal/closed Play track as appropriate.
- **Commercial production:** paid Google Play listing.

The repository must never contain the release/upload keystore or its passwords.

## 6. Updates

Paid users receive normal app updates through Google Play. A later app update must not introduce a subscription requirement for functionality that was sold as part of the one-time-purchase product without an explicit product decision and migration plan.

## 7. Vision-model future

The planned on-device vision/semantic-search extension should preferably remain on-device so it does not create a recurring inference bill that conflicts with the one-time purchase model. If a future cloud model becomes necessary, its economics and privacy model must be documented before implementation.
