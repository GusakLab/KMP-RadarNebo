---
title: Compose Method for Readable Flow
impact: CRITICAL
tags: struct, compose-method, readability, abstraction-levels
---

## Compose Method for Readable Flow

Make method body read as steps at one abstraction level. Name each step to reveal intent.

**Incorrect (mixed abstraction levels):**

```typescript
async function publishArticle(article: Article, author: User): Promise<void> {
  // Low-level validation mixed with high-level flow
  if (!article.title || article.title.trim().length === 0) {
    throw new Error('Title required')
  }
  if (!article.content || article.content.length < 100) {
    throw new Error('Content must be at least 100 characters')
  }
  if (!author.permissions.includes('publish')) {
    throw new Error('User cannot publish')
  }

  article.status = 'published'
  article.publishedAt = new Date()
  article.slug = article.title.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '')

  await db.articles.update(article.id, article)

  const followers = await db.users.find({ following: author.id })
  for (const follower of followers) {
    await sendEmail(follower.email, `New article: ${article.title}`, article.excerpt)
  }

  await searchIndex.add({ id: article.id, title: article.title, content: article.content })
}
```

**Correct (consistent abstraction level):**

```typescript
async function publishArticle(article: Article, author: User): Promise<void> {
  validateArticleForPublishing(article)
  verifyPublishPermission(author)

  const publishedArticle = markAsPublished(article)
  await saveArticle(publishedArticle)

  await notifyFollowers(author, publishedArticle)
  await indexForSearch(publishedArticle)
}

function validateArticleForPublishing(article: Article): void {
  if (!article.title?.trim()) {
    throw new Error('Title required')
  }
  if (!article.content || article.content.length < 100) {
    throw new Error('Content must be at least 100 characters')
  }
}

function verifyPublishPermission(author: User): void {
  if (!author.permissions.includes('publish')) {
    throw new Error('User cannot publish')
  }
}

function markAsPublished(article: Article): Article {
  return {
    ...article,
    status: 'published',
    publishedAt: new Date(),
    slug: generateSlug(article.title)
  }
}

function generateSlug(title: string): string {
  return title.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '')
}
```

Reference: [Compose Method Pattern](https://wiki.c2.com/?ComposeMethod)
