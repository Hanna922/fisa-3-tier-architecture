# MySQL Source (Master) Dockerfile
FROM mysql:8.4

# 커스텀 설정 파일 복사
COPY my.cnf /etc/mysql/conf.d/my.cnf

# 초기 스키마/데이터 SQL 복사
COPY init.sql /docker-entrypoint-initdb.d/init.sql

EXPOSE 3306
