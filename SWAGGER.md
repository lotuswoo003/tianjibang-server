# Swagger API 文档使用说明

## 访问方式

启动应用后，可以通过以下方式访问 Swagger 文档：

### Swagger UI (推荐)
```
http://localhost:8080/swagger-ui.html
```
或
```
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI JSON 格式
```
http://localhost:8080/v3/api-docs
```

### OpenAPI YAML 格式
```
http://localhost:8080/v3/api-docs.yaml
```

## API 分组说明

项目提供以下 API 模块：

### 1. 系统接口
- **健康检查**: GET `/api/health` - 检查服务运行状态
- **Hello接口**: GET `/api/hello` - 测试接口连通性

### 2. 股票数据接口
- **导入CSV文件**: POST `/api/stock/import`
- **根据ID查询**: GET `/api/stock/{id}`
- **根据股票代码查询**: GET `/api/stock/code/{stockCode}`
- **查询所有数据**: GET `/api/stock/all`
- **根据日期范围查询**: GET `/api/stock/range`
- **根据行业查询**: GET `/api/stock/industry/{industry}`
- **创建股票数据**: POST `/api/stock`
- **更新股票数据**: PUT `/api/stock/{id}`
- **删除股票数据**: DELETE `/api/stock/{id}`

### 3. 当前股票数据管理
- **导入CSV文件**: POST `/api/current-stock/import` - 只导入最近1年数据
- **查询所有当前股票数据**: GET `/api/current-stock/all`
- **根据股票代码查询**: GET `/api/current-stock/stock/{stockCode}`
- **根据日期范围查询**: GET `/api/current-stock/date-range`
- **清理旧数据**: DELETE `/api/current-stock/clean-old`
- **计算技术指标**: POST `/api/current-stock/calculate-indicators`
- **计算指定股票的技术指标**: POST `/api/current-stock/calculate-indicators/{stockCode}`

### 4. 选股策略
- **获取所有可用策略**: GET `/api/stock-selection/strategies`
- **根据策略代码选股**: GET `/api/stock-selection/select/{strategyCode}`
- **连续涨停策略**: GET `/api/stock-selection/consecutive-limit-up`
- **历史二波策略**: GET `/api/stock-selection/history-two-waves`

## 使用示例

### 在 Swagger UI 中测试 API

1. 启动应用：`mvn spring-boot:run`
2. 打开浏览器访问：http://localhost:8080/swagger-ui.html
3. 选择要测试的 API 接口
4. 点击 "Try it out" 按钮
5. 填写必要的参数
6. 点击 "Execute" 按钮执行请求
7. 查看响应结果

### 使用 curl 测试

```bash
# 健康检查
curl http://localhost:8080/api/health

# 获取所有选股策略
curl http://localhost:8080/api/stock-selection/strategies

# 根据股票代码查询
curl http://localhost:8080/api/stock/code/000001

# 导入CSV文件（需要POST请求）
curl -X POST http://localhost:8080/api/current-stock/import
```

## 配置说明

### application.yml 配置（可选）

如需自定义 Swagger 配置，可在 `application.yml` 中添加：

```yaml
springdoc:
  api-docs:
    enabled: true
    path: /v3/api-docs
  swagger-ui:
    enabled: true
    path: /swagger-ui.html
    tags-sorter: alpha
    operations-sorter: alpha
  show-actuator: false
```

### OpenApiConfig.java

配置文件位于 `src/main/java/org/example/config/OpenApiConfig.java`，可自定义：
- API 标题
- 描述信息
- 版本号
- 联系方式
- 许可证信息

## 注意事项

1. Swagger UI 仅用于开发和测试环境，生产环境建议关闭
2. 所有 Controller 已添加 `@Tag` 和 `@Operation` 注解，便于文档分组和说明
3. 日期参数格式为：`yyyy-MM-dd`，例如 `2024-01-01`
4. POST/PUT 请求需要在 Request Body 中传递 JSON 格式数据

## 依赖信息

项目使用 SpringDoc OpenAPI 3.x：

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

该依赖会自动集成：
- Swagger UI
- OpenAPI 3.0 规范
- 自动扫描所有 `@RestController` 和 `@Controller`
