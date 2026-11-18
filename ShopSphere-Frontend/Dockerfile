# Multi-stage build for ShopSphere Angular Frontend
FROM node:20-alpine AS builder
WORKDIR /app

# Copy dependency definitions
COPY package*.json ./
RUN npm ci

# Copy project source and build production bundle
COPY . .
RUN npm run build -- --configuration production

# Production image with Nginx
FROM nginx:alpine
COPY --from=builder /app/dist/NewProject/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
