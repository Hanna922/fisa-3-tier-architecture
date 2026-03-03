
### HikariCP `Failed to get driver instance`

**증상**

```
SEVERE: Exception sending context initialized event
java.lang.RuntimeException: Failed to get driver instance for
jdbcUrl=jdbc:mysql://localhost:3307/card_db
```

**원인**

`driverClassName`을 명시하지 않으면 HikariCP가 `DriverManager.getDriver(url)`로 드라이버를 탐색하는데, Servlet 컨테이너의 클래스로더 격리로 인해 MySQL Connector/J 자동 등록이 실패할 수 있습니다. `contextInitialized()`에서 예외가 발생하면 Tomcat이 웹 애플리케이션 전체를 비활성화하므로 모든 경로가 404를 반환합니다.

**해결**

```java
sourceConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
replicaConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
```

---



### `ERROR 3100`: InnoDB Cluster 구성 후 데이터 적재 실패

**증상**

```
ERROR 3100 (HY000) at line 84: Error on observer while running replication hook 'before_commit'.
```

**원인**

`setup.sql`의 벌크 로드 최적화 설정(`SET unique_checks=0`, `SET foreign_key_checks=0`)이 Group Replication의 `before_commit` 훅과 충돌합니다. Group Replication은 write set 기반 충돌 감지를 위해 `unique_checks=ON`을 요구합니다.

**해결**

InnoDB Cluster 구성 전에 데이터를 적재합니다. 클러스터 구성 후 Clone Plugin이 node2, node3으로 데이터를 자동 복사합니다. 


---

### 로그아웃 후 브라우저에 JSESSIONID 잔존

**증상**

로그아웃 후 리다이렉션은 정상이나 개발자 도구 Cookies 탭에 JSESSIONID가 남아 있음.

**원인**

`session.invalidate()`는 서버 측 세션 객체만 제거하며 브라우저 쿠키는 삭제하지 않습니다.

**해결**

로그아웃 시 `Max-Age=0` 쿠키를 응답에 포함하여 브라우저가 즉시 삭제하도록 합니다.

```java
Cookie cookie = new Cookie("JSESSIONID", "");
cookie.setMaxAge(0);
cookie.setPath("/");
resp.addCookie(cookie);
```

