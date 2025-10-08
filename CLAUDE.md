# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot 3.3.3 application for managing stock market data. It uses MyBatis for database access and MySQL for storage. The application provides REST APIs for querying and managing stock trading data including prices, volumes, market values, and industry classifications.

## Technology Stack

- **Framework**: Spring Boot 3.3.3
- **Java Version**: 17
- **Database**: MySQL (stock_db)
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

### Database Setup
The database schema is defined in `src/main/resources/schema.sql`. Execute this file to create the `stock_data` table in MySQL.

Database connection is configured in `src/main/resources/application.yml`:
- URL: jdbc:mysql://localhost:3306/stock_db
- Default credentials: root/root (change in application.yml)

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

The `stock_data` table stores daily stock trading information:
- **Primary fields**: stock_code, stock_name, trade_date
- **Price data**: open_price, high_price, low_price, close_price
- **Volume data**: volume, turnover
- **Market cap**: float_market_value, total_market_value
- **Industry classification**: industry_level1/2/3 (申万行业分类)
- **Indexes**: Unique index on (stock_code, trade_date), indexes on stock_code and trade_date

### MyBatis Configuration

- Mapper XML files location: `classpath:mapper/*.xml`
- Type aliases package: `org.example.entity`
- Auto-mapping: Underscore to camelCase enabled
- SQL logging: StdOutImpl (logs SQL statements to console)

### Key Features

- Query stock data by code, date, or date range
- Industry-based stock filtering
- Batch insert support for bulk data loading
- Unique constraint prevents duplicate records for same stock/date combination
