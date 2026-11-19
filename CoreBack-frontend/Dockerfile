# Stage 1: build the React app
FROM node:20-alpine as build-stage

WORKDIR /app

# Install dependencies first (cached while package*.json does not change)
COPY package*.json ./
RUN npm install

# Copy the source code
COPY . .

# Build the production bundle into /app/dist
RUN npm run build

# Stage 2: serve the static files with Nginx
FROM nginx:alpine

# Copy only the build output (no node_modules or sources in the final image)
COPY --from=build-stage /app/dist /usr/share/nginx/html
# SPA fallback (try_files -> index.html)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
