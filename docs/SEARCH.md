# Search and Ranking

## 1. Scope before vision

No image-understanding model is used initially.

Index searchable text from:

- display/filename,
- user label,
- source/folder display names,
- manual tags,
- collection names,
- basic derived type tokens such as `gif`, `sticker`, `animated`.

## 2. Normalization

- Unicode normalize,
- lowercase using locale-safe logic,
- trim punctuation/extra whitespace,
- tokenize conservatively,
- optionally remove simple filename boilerplate.

## 3. Ranking

Suggested weighted ranking:

1. exact user-label/tag match,
2. prefix match,
3. filename token match,
4. collection/source match,
5. favorite boost,
6. recent-use boost,
7. frequency boost.

Do not let high usage permanently bury new media. Apply time decay and bounded boosts.

## 4. Empty query ranking

Default media view:

- recents first,
- favorites accessible separately,
- then recently added or source-specific depending on selected tab.

## 5. Performance

Room FTS is appropriate for lexical search if dependency/version support is current. Keep queries bounded/paged. Search must not load entire library into Kotlin memory.

## 6. Future vision seam

Future semantic data may add:

- generated tags,
- captions,
- embedding vectors.

Do not redesign user-facing search contract. Semantic provider enriches the same result model.
