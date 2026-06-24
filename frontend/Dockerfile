# syntax=docker/dockerfile:1

### build stage — Vite 로 정적 번들(dist) 생성
FROM node:22-alpine AS build
WORKDIR /app
# 의존성 레이어 캐시: lock 먼저 복사
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

### runtime stage — nginx 가 정적 서빙 + /api 리버스 프록시
FROM nginx:1.27-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
