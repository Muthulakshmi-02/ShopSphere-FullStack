// Single source of truth for the backend's base URL.
// Product images are served statically from Spring Boot at /images/<filename>,
// and the DB only stores the bare filename (e.g. "iphone15.jpg") - so every
// place that renders a product image needs to prefix it with this base URL.
export const API_BASE_URL = 'http://localhost:8080';

// Shown when a product has no image. The old value was 'assets/placeholder.jpg', but angular.json
// only copies the "public" folder into the build, so that file never existed in the running app
// and the browser showed a broken image. This is a tiny inline PNG instead: nothing to ship, and
// Angular accepts data:image/png URLs (it blocks data:image/svg+xml ones).
const PLACEHOLDER_IMAGE =
  'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAQAAAAECAIAAAAmkwkpAAAAEElEQVR4nGOQVraAIwbiOAC+JAdhOeA+tQAAAABJRU5ErkJggg==';

export function productImageUrl(imageUrl: string | null | undefined): string {
  if (!imageUrl) {
    return PLACEHOLDER_IMAGE;
  }
  // Already a full URL (e.g. an external image) - use as-is.
  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl;
  }
  return `${API_BASE_URL}/images/${imageUrl}`;
}