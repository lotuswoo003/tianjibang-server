# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.3.3 application for managing stock market data. It uses MyBatis for database access and MySQL for storage. The application provides REST APIs for querying and managing stock trading data including prices, volumes, market values, and industry classifications.

## Technology Stack

- **Framework**: Spring Boot 3.3.3
- **Java Version**: 17
- **Database**: MySQL (stock_db)
- **Cache**: Redis (localhost:6379)
- **ORM**: MyBatis 3.0.3
- **API Documentation**: SpringDoc OpenAPI 2.5.0 (Swagger)
- **Build Tool**: Maven

## Common Commands

### Build and Run
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=HelloControllerTest
```

### Scheduled Tasks

The application includes scheduled tasks for automated data import:

**Manual Trigger:**
```bash
curl -X POST "http://localhost:8080/api/scheduled-task/import-incremental-data"
```

**Automatic Schedule:**
- Runs daily at 2:00 AM (can be disabled via `app.data-import.enabled=false`)
- Reads CSV files from configured directory
- Processes files with naming pattern: `daily_yyyy-MM-dd.csv`
- Automatically calculates technical indicators after import
- Sorts files by name to process in chronological order

### Data Import Configuration

**Local Environment** (`application-local.yml`):
```yaml
app:
  data-import:
    path: /Users/lynn/IdeaProject/study/stock_addition_data
    enabled: true
```

**Production Environment** (`application-prod.yml`):
```yaml
app:
  data-import:
    path: /usr/lynn/data
    enabled: true
```

**Directory Structure:**
```
stock_addition_data/
├── daily_2025-09-25.csv
├── daily_2025-09-26.csv
├── daily_2025-09-27.csv
└── ...
```

**File Naming Pattern:** `daily_yyyy-MM-dd.csv` (e.g., daily_2025-09-25.csv)

**CSV Format:**
- Headers: 日期,股票代码,开盘,收盘,最高,最低,成交量,成交额,振幅,涨跌幅,涨跌额,换手率
- Encoding: UTF-8
- Trade date is extracted from filename, not CSV content
- Float market value is calculated from: 成交额 / (换手率 / 100)
- Stock name and industry fields are left empty (to be enriched from other sources)
- Batch inserts 1000 records at a time for performance
- Uses INSERT to allow duplicate handling

### Database Setup
The database schema is defined in `src/main/resources/schema.sql`. Execute this file to create the `stock_data` table in MySQL.

Database connection is configured in `src/main/resources/application-local.yml`:
- URL: jdbc:mysql://localhost:3306/stock
- Username: root
- Password: 123456

### Redis Setup
Redis is used for caching stock selection results.

Local configuration (`application-local.yml`):
- Host: localhost
- Port: 6379
- Database: 0
- No password required

Make sure Redis is running:
```bash
# Start Redis (Mac with Homebrew)
redis-server

# Or using Docker
docker run -d -p 6379:6379 redis:latest
```

### API Documentation
After starting the application, access Swagger UI at:
```
http://localhost:8080/swagger-ui.html
```

## Architecture

### Layer Structure

The application follows a standard three-layer architecture:

1. **Controller Layer** (`org.example.controller`): REST API endpoints
   - `HelloController`: Basic health check and hello endpoints

2. **Service Layer** (`org.example.service`): Business logic
   - `StockDataService`: Interface defining stock data operations
   - `StockDataServiceImpl`: Implementation with CRUD operations, date range queries, and industry filtering

3. **Data Access Layer** (`org.example.mapper`): MyBatis mappers
   - `StockDataMapper`: Java interface with @Mapper annotation
   - `StockDataMapper.xml`: SQL mappings in `src/main/resources/mapper/`

4. **Entity Layer** (`org.example.entity`):
   - `StockData`: Domain model representing stock trading data

### Database Schema

**`stock_data` table** stores daily stock trading information:
- **Primary fields**: stock_code, stock_name, trade_date
- **Price data**: open_price, high_price, low_price, close_price
- **Volume data**: volume, turnover
- **Market cap**: float_market_value, total_market_value
- **Industry classification**: industry_level1/2/3 (申万行业分类)
- **Indexes**: Unique index on (stock_code, trade_date), indexes on stock_code and trade_date

**`current_stock_data` table** stores recent 1-year stock data with technical indicators:
- All fields from `stock_data` plus:
- **Technical indicators**: ma5, ma10, ma20 (moving averages)
- **Volume metrics**: volume_ratio (量比), turnover_rate (换手率)
- **Turnover rate calculation**: (成交量 × 收盘价 / 流通市值) × 100
- Updated via `/api/current-stock/calculate-indicators` endpoint

### MyBatis Configuration

- Mapper XML files location: `classpath:mapper/*.xml`
- Type aliases package: `org.example.entity`
- Auto-mapping: Underscore to camelCase enabled
- SQL logging: StdOutImpl (logs SQL statements to console)

### Selected Stock Management

The application uses a dedicated table `selected_stock` to persist stock selection results:

**Table Structure:**
- Primary fields: strategy_code, stock_code, stock_name, selection_date
- Tracking fields: next_day_trend, three_day_close_price, seven_day_close_price
- Additional: remark, created_at, updated_at
- Unique constraint: (strategy_code, stock_code, selection_date)

**Stock Selection Workflow:**
1. **Sync Phase**: Call `/api/stock-selection/sync` with strategy codes and date
   - Executes stock selection logic for each strategy
   - Saves results to `selected_stock` table
   - Returns statistics (success count, total stocks, errors)

2. **Query Phase**: Call `/api/stock-selection/select/{strategyCode}`
   - Retrieves latest selection results from database
   - Returns stocks with tracking information (trends, prices)
   - No computation needed, fast database query

**Benefits:**
- Persistent storage of selection history
- Track performance metrics (next day trend, future prices)
- Query historical selections by date range
- No cache expiration issues
- Support for manual annotations via `remark` field

### Stock Selection Strategies

The application provides multiple stock selection strategies:

**1. Consecutive Limit Up (连续涨停策略)**
- Finds stocks with consecutive limit-up days in the past 6 months
- Minimum 2 consecutive limit-up days required

**2. History Two Waves (历史二波策略)**
- Identifies stocks with two separate limit-up waves in the past year
- First wave: consecutive limit-ups
- Pullback period: at least 5 days with >5% decline
- Second wave: another set of consecutive limit-ups

**3. MA Convergence (均线粘合策略)** 🆕
- **Common conditions**:
  - MA5 turning upward (连续2天上涨)
  - MA5 close to MA10 (within 3%)
  - MA5 close to MA20 (within 5%)
  - Excludes ST stocks

- **ChYB (创业板 - starts with 300)**:
  - Max single-day gain >15% in past 90 days
  - Current price >10% below 6-month high

- **Main Board (沪深主板 - 000/600 series)**:
  - Consecutive limit-ups in past 60 days
  - Current price >20% below 6-month high

### Key Features

- Query stock data by code, date, or date range
- Industry-based stock filtering
- Batch insert support for bulk data loading
- Unique constraint prevents duplicate records for same stock/date combination
- Persistent stock selection results with performance tracking
- Strategy-based selection with historical data
- Redis support for general caching needs
- Technical indicators: MA5, MA10, MA20, volume ratio, turnover rate
