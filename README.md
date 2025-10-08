# tinajibang-server

一个可打包为可执行 JAR 并支持 Docker 化部署的 Spring Boot 项目脚手架。

## 本地运行（无需安装 Maven，使用 IDE 直接运行）
- 入口类：`src/main/java/com/tinajibang/server/TinajibangServerApplication.java`
- 默认端口：`8080`（可通过环境变量 `SERVER_PORT` 覆盖）
- 测试接口：
  - `GET /ping` 返回 `{"status":"ok","message":"pong"}`
  - `GET /actuator/health` 健康检查

## 使用 Maven 打包 JAR
环境已配置 Spring Boot Maven 插件，将自动生成可执行 JAR。

```bash
mvn -DskipTests package
# 输出位置：target/tinajibang-server-0.0.1-SNAPSHOT.jar
```

运行：
```bash
java -jar target/tinajibang-server-0.0.1-SNAPSHOT.jar
# 或指定端口
SERVER_PORT=9090 java -jar target/tinajibang-server-0.0.1-SNAPSHOT.jar
```

## Docker 化
项目根目录已提供 `Dockerfile` 与 `.dockerignore`，采用多阶段构建（builder 使用 Maven 镜像，runtime 使用 JRE 镜像）。

构建镜像：
```bash
docker build -t tinajibang-server:latest .
```

运行容器：
```bash
docker run --rm -p 8080:8080 \
  -e SERVER_PORT=8080 \
  -e JAVA_OPTS="-Xms256m -Xmx512m" \
  --name tinajibang-server tinajibang-server:latest
```

验证：
```bash
curl http://localhost:8080/ping
curl http://localhost:8080/actuator/health
```

## 配置
- 应用名：`spring.application.name=tinajibang-server`
- 端口：`server.port=${SERVER_PORT:8080}`（支持从环境变量覆盖）

## 目录结构
- `src/main/java` 应用源码
- `src/main/resources/application.yml` 基础配置
- `Dockerfile` 多阶段构建镜像
- `.dockerignore` Docker 构建忽略
- `.gitignore` Git 忽略
