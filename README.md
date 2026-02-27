# POI Search API (Java Spring Boot)

一个基于 Spring Boot 的 RESTful API 服务，用于查询指定经纬度附近的 POI（兴趣点），支持 AWS Location Service 和 Google Maps Places API 的智能降级。

## 🌟 核心特性

- 🚀 **双服务集成**: 优先使用 AWS Geo Places V2 (SearchNearby API)，自动降级到 Google Maps
- 🔄 **智能 Fallback**: AWS 无结果时自动切换到 Google Maps
- 📍 **灵活搜索**: 支持经纬度、半径、关键词搜索
- ⚡ **高性能**: 基于 Spring Boot 3.x 和 Java 17，使用 AWS SDK 2.34.0
- 🎉 **零配置**: 使用 Places V2 API，**无需创建 Place Index**！
- 🛡️ **参数验证**: 使用 Bean Validation 进行输入验证
- 📊 **日志记录**: SLF4J + Logback 完整日志
- 🏗️ **分层架构**: Controller → Service → AWS/Google 清晰分层
- 📏 **精确距离**: API 直接返回每个 POI 到查询位置的距离（米）

## 📁 项目结构

```
poi-search-api-java/
├── pom.xml                                    # Maven 依赖配置
├── src/main/
│   ├── java/com/example/poisearch/
│   │   ├── PoiSearchApiApplication.java      # 主应用类
│   │   ├── config/
│   │   │   └── ServiceConfig.java            # AWS 和 Google Maps 配置
│   │   ├── controller/
│   │   │   ├── POIController.java            # POI 搜索接口
│   │   │   └── HealthController.java         # 健康检查接口
│   │   ├── model/
│   │   │   ├── Coordinates.java              # 坐标模型
│   │   │   ├── POI.java                      # POI 模型
│   │   │   ├── POISearchRequest.java         # 搜索请求
│   │   │   └── POISearchResponse.java        # 搜索响应
│   │   ├── service/
│   │   │   ├── AWSLocationService.java       # AWS Location Service 集成
│   │   │   ├── GoogleMapsService.java        # Google Maps 集成
│   │   │   └── POISearchService.java         # 主搜索服务（含 fallback 逻辑）
│   │   └── exception/
│   │       └── GlobalExceptionHandler.java   # 全局异常处理
│   └── resources/
│       └── application.properties            # 应用配置
└── README.md
```

## 🛠️ 技术栈

- **Java 17**
- **Spring Boot 3.2.2**
- **Maven**
- **AWS SDK 2.34.0** (Geo Places V2 - SearchNearby API)
- **Google Maps Services Java Client**
- **Lombok** (减少样板代码)
- **SLF4J + Logback** (日志)

## 📋 前置要求

- JDK 17 或更高版本
- Maven 3.6+
- **AWS 账号**
  - ✅ AWS 访问密钥 (Access Key ID & Secret Access Key)
  - ✅ IAM 权限：`geo-places:SearchNearby`
  - ❌ **不需要**创建 Place Index（Places V2 的新特性！）
- Google Maps API 密钥（作为备用服务）

## 🚀 快速开始

### 1. 克隆或进入项目目录

```bash
cd ~/poi-search-api-java
```

### 2. 配置环境变量

编辑 `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=3000

# AWS Geo Places Configuration (Places V2 API)
aws.region=us-east-1                              # AWS 区域
aws.access-key-id=你的AWS访问密钥                  # AWS Access Key ID
aws.secret-access-key=你的AWS密钥                  # AWS Secret Access Key

# Google Maps Configuration (作为备用服务)
google.maps.api-key=你的Google_Maps_API密钥

# Logging
logging.level.root=INFO
logging.level.com.example.poisearch=DEBUG
```

**配置说明:**
- **简化配置**: 不再需要配置 `aws.location.index-name`！
- **Places V2 优势**: SearchNearby API 无需预先创建 Place Index，开箱即用
- **Fallback 机制**: 如果 AWS 配置错误或查询失败，系统会自动降级到 Google Maps

### 3. 编译项目

```bash
mvn clean package
```

### 4. 运行应用

**开发模式** (使用 Spring Boot Maven 插件):
```bash
mvn spring-boot:run
```

**生产模式** (运行 JAR 文件):
```bash
java -jar target/poi-search-api-1.0.0.jar
```

### 5. 测试 API

**健康检查**:
```bash
curl http://localhost:3000/health
```

**搜索 POI** (纽约时代广场附近):
```bash
curl "http://localhost:3000/api/poi/search?lat=40.7580&lng=-73.9855&radius=1000&query=restaurant"
```

## 📖 API 文档

### GET `/health`

健康检查端点。

**响应示例**:
```json
{
  "status": "healthy",
  "application": "poi-search-api",
  "timestamp": "2026-02-25T10:00:00"
}
```

### GET `/api/poi/search`

搜索指定坐标附近的 POI。

**查询参数**:

| 参数 | 类型 | 必需 | 说明 | 限制 |
|------|------|------|------|------|
| `lat` | Double | 是 | 纬度 | -90 ~ 90 |
| `lng` | Double | 是 | 经度 | -180 ~ 180 |
| `radius` | Integer | 否 | 搜索半径（米） | 1 ~ 50000，默认 1000 |
| `query` | String | 否 | 关键词 | 例如 "restaurant", "hotel" |

**成功响应示例**:
```json
{
  "success": true,
  "source": "aws",
  "results": [
    {
      "name": "Central Park",
      "address": "New York, NY, United States",
      "coordinates": {
        "lat": 40.7829,
        "lng": -73.9654
      },
      "type": "park",
      "rating": 4.8,
      "placeId": "ChIJ4zGFAZpYwokRGUGph3Mf37k",
      "distance": 234.5
    }
  ],
  "count": 1
}
```

**注意**: `distance` 字段表示该 POI 到查询位置的距离（单位：米），由 AWS SearchNearby API 直接返回。

**错误响应示例**:
```json
{
  "success": false,
  "error": "Validation failed",
  "details": {
    "lat": "Latitude must be between -90 and 90"
  }
}
```

## 🔧 AWS Geo Places 配置

### 配置 IAM 权限

创建 IAM 用户或角色，附加以下权限策略:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "geo-places:SearchNearby"
      ],
      "Resource": "*"
    }
  ]
}
```

**重要说明**:
- ✅ **无需创建 Place Index**: Places V2 的 SearchNearby API 不需要预先创建索引
- ✅ **简化权限**: Resource 设置为 `*` 即可，无需指定特定资源
- ✅ **零配置**: 只需 AWS 访问密钥即可立即使用

**技术优势 (Places V2 vs Places V1)**:

| 特性 | Places V1 (旧) | Places V2 (新) |
|------|---------------|---------------|
| **Place Index** | ✗ 必须预先创建 | ✅ 无需创建 |
| **配置复杂度** | 高（需配置索引名称） | 低（仅需密钥） |
| **权限管理** | 需指定具体索引资源 | 通配符即可 |
| **API** | SearchPlaceIndexForText | SearchNearby |
| **距离计算** | 手动计算（Haversine） | API 直接返回 |
| **半径参数** | BiasPosition（不精确） | queryRadius（精确） |

**SearchNearby API 特性**:
- **原生附近搜索**: 专为"查找附近"场景设计，不是文本搜索的附加功能
- **精确半径控制**: 通过 `queryRadius` 参数精确指定搜索范围
- **分类过滤**: 支持按 POI 类别过滤（如 restaurant、hotel 等）
- **距离信息**: API 直接返回每个 POI 到查询点的距离（米）

## 🗺️ Google Maps API 配置

### 1. 启用 API

1. 访问 [Google Cloud Console](https://console.cloud.google.com/)
2. 创建项目或选择现有项目
3. 启用 **Places API**
4. 创建 API 密钥

### 2. 限制密钥

为了安全，建议限制 API 密钥:
- **API 限制**: 仅限制为 Places API
- **应用限制**: 根据需要设置 IP 地址或 HTTP referrer

### 3. 启用计费

Google Maps 需要启用计费账户（即使在免费额度内）。

## 🔄 Fallback 工作流程

```
用户请求
    ↓
优先尝试 AWS Location Service
    ↓
AWS 有结果? ——— 是 ——→ 返回 AWS 结果 (source: "aws")
    ↓ 否
尝试 Google Maps Places API
    ↓
Google 有结果? ——— 是 ——→ 返回 Google 结果 (source: "google")
    ↓ 否
返回错误响应
```

## 📊 日志说明

应用使用 SLF4J + Logback 记录日志:

- **INFO**: 常规操作日志
- **DEBUG**: 详细的调试信息（包括请求参数）
- **ERROR**: 错误和异常堆栈

**查看日志**:
```bash
# 实时查看日志
tail -f logs/spring.log

# 或在控制台查看（开发模式）
mvn spring-boot:run
```

## 🧪 测试用例

### 测试不同场景

**基本搜索**:
```bash
curl "http://localhost:3000/api/poi/search?lat=40.7580&lng=-73.9855"
```

**指定半径**:
```bash
curl "http://localhost:3000/api/poi/search?lat=40.7580&lng=-73.9855&radius=500"
```

**关键词搜索**:
```bash
curl "http://localhost:3000/api/poi/search?lat=40.7580&lng=-73.9855&query=coffee"
```

**无效参数测试**:
```bash
curl "http://localhost:3000/api/poi/search?lat=100&lng=-73.9855"
# 预期返回 400 错误
```

## 📦 打包部署

### 构建可执行 JAR

```bash
mvn clean package
```

生成的 JAR 位于: `target/poi-search-api-1.0.0.jar`

### Docker 化（可选）

创建 `Dockerfile`:

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/poi-search-api-1.0.0.jar app.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建和运行:
```bash
docker build -t poi-search-api .
docker run -p 3000:3000 \
  -e AWS_ACCESS_KEY_ID=your_key \
  -e AWS_SECRET_ACCESS_KEY=your_secret \
  -e AWS_LOCATION_INDEX_NAME=your_index \
  -e GOOGLE_MAPS_API_KEY=your_key \
  poi-search-api
```

### 部署到 AWS

**使用 Elastic Beanstalk**:
1. 打包应用: `mvn clean package`
2. 创建 Elastic Beanstalk 应用
3. 上传 JAR 文件
4. 配置环境变量

**使用 EC2**:
1. 安装 JDK 17
2. 上传 JAR 文件
3. 使用 `systemd` 或 `nohup` 运行

## 💰 成本考虑

- **AWS Location Service**: $0.40 / 1000 次请求（免费层后）
- **Google Maps Places API**: $17 / 1000 次请求（$200 月度信用额度后）

**优势**: Fallback 策略最大化使用 AWS，显著降低成本！

## 🐛 故障排除

### 问题: 应用启动失败

**检查**:
1. JDK 版本是否为 17+
2. `application.properties` 是否配置正确
3. AWS 和 Google API 密钥是否有效

### 问题: AWS 返回空结果或报错

**可能原因**:
1. **AWS 凭证配置错误**: Access Key ID 或 Secret Access Key 不正确
2. **IAM 权限不足**: IAM 用户/角色缺少 `geo-places:SearchNearby` 权限
3. **查询的地理位置没有 POI 数据**: 某些偏远地区可能没有数据
4. **半径设置过小**: 尝试增大搜索半径
5. **区域配置错误**: `aws.region` 设置不正确

**调试步骤**:
1. 查看日志: `logging.level.com.example.poisearch=DEBUG`
2. 验证 AWS 凭证:
   ```bash
   aws sts get-caller-identity
   ```
3. 验证 IAM 权限（确保有 `geo-places:SearchNearby` 权限）
4. 尝试不同的坐标、半径和关键词
5. 检查 AWS 区域是否正确（Places V2 支持的区域）

### 问题: Google Maps fallback 不工作

**检查**:
1. Google Maps API 密钥是否正确
2. Places API 是否已启用
3. 是否启用了计费账户

## 📝 开发指南

### 添加新的搜索提供商

1. 创建新的 Service 类（如 `BingMapsService.java`）
2. 在 `POISearchService` 中添加 fallback 逻辑
3. 在 `ServiceConfig` 中配置客户端

### 修改响应格式

编辑 `POI.java` 和 `POISearchResponse.java` 模型类。

### 添加速率限制

可以集成 Spring 的 `Bucket4j` 或使用 API Gateway。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT

---

**使用 ❤️ 构建 | Spring Boot + AWS Location Service + Google Maps API**
