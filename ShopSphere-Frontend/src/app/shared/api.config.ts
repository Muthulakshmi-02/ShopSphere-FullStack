// Single source of truth for the backend's base URL.
// Product images are served statically from Spring Boot at /images/<filename>,
// and the DB only stores the bare filename (e.g. "iphone15.jpg") - so every
// place that renders a product image needs to prefix it with this base URL.
export const API_BASE_URL = 'http://localhost:8080';

export function productImageUrl(imageUrl: string | null | undefined): string {
  if (!imageUrl) {
    return 'assets/placeholder.jpg';
  }
  // Already a full URL (e.g. an external image) - use as-is.
  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl;
  }
  return `${API_BASE_URL}/images/${imageUrl}`;
}
