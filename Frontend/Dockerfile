ARG DOCKER_HUB_MIRROR=docker.1ms.run/library
FROM ${DOCKER_HUB_MIRROR}/node:22-alpine AS builder
WORKDIR /app
# Empty = same-origin /api via nginx (see .env.production)
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

COPY package.json package-lock.json ./
RUN npm ci

COPY . ./
RUN npm run build

ARG DOCKER_HUB_MIRROR=docker.1ms.run/library
FROM ${DOCKER_HUB_MIRROR}/nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
