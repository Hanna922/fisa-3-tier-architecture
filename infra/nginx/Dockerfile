# Nginx Dockerfile
FROM nginx:1.25-alpine

# 기본 nginx.conf 제거 후 커스텀 설정으로 교체
RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
