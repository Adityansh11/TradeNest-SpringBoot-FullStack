# TradeNest — Intelligent Spring Boot Trading Simulator

TradeNest is a Zerodha-inspired **virtual stock trading platform** built with Spring Boot, Spring Data JPA, H2/PostgreSQL, HTML, CSS and vanilla JavaScript.

It is an educational simulation only and does not place real trades.

## What makes this version different

TradeNest is more than CRUD trading. It includes a simulated market, portfolio risk analysis, scenario simulation, trading analytics and JWT-based account authorization.

### Core trading
- User registration and login
- BCrypt password hashing
- JWT authentication with protected user APIs
- ₹1,00,000 virtual starting balance
- Seeded Indian stocks
- Searchable market
- Buy and sell virtual shares
- Average buy-price calculation
- Holdings and unrealized P&L
- Order history
- Input validation and trade error handling

### Portfolio Risk Analyzer
- Diversification score using an HHI-style concentration calculation
- Sector allocation
- Cash allocation
- Best and worst holding
- Concentration warnings

### What-If Simulator
- Select a held/listed stock
- Enter a hypothetical price movement
- Projected stock price
- Portfolio value impact
- Projected P&L
- Wealth impact

### Simulated Market Engine
- Background market simulation runs every 60 seconds
- Controlled random price movement of approximately ±2.5%
- Stock prices update automatically
- Portfolio P&L changes as the simulated market moves

### Quant Research Lab
- Deterministic synthetic historical price paths for reproducible experiments
- Buy & Hold baseline
- 5/20 simple moving-average crossover strategy
- 10-day momentum strategy
- Equity curve visualization
- Total return vs buy-and-hold benchmark
- Maximum drawdown
- Annualized volatility
- Sharpe ratio
- Trade count and strategy diagnostics

The Quant Lab is intentionally labeled as a simulation/backtesting environment. It does not use live historical market data and is not financial advice.

## Trading Analytics
- Total orders
- Buy/sell order counts
- Buy/sell volume
- Most traded stock
- Realized P&L estimate from historical order sequence
- Open share count
- Current cash

## Technology

- Java 23
- Spring Boot 3.5.6
- Spring Web
- Spring Data JPA
- Spring Security
- BCrypt
- JWT (JJWT 0.12.6)
- H2 database
- PostgreSQL driver
- Maven
- HTML5
- CSS3
- Vanilla JavaScript

## Architecture

```text
HTML / CSS / JavaScript
          |
          | REST + JWT
          v
     Spring Boot
          |
   +------+------+----------------+
   |      |      |                |
Trading  Risk  What-If       Analytics
Engine  Engine  Engine          Engine
   |      |      |      |         |
   +------+------+------+------+---------+
          |      |      |
       Quant   Market  JPA
        Lab    Engine  |
                       |
                  H2 / PostgreSQL
          |
      H2 / PostgreSQL
```

## Project Structure

```text
TradeNest-SpringBoot-FullStack/
├── README.md
└── backend/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/
        ├── java/com/adityansh/zerodha/
        │   ├── config/
        │   ├── controller/
        │   ├── dto/
        │   ├── entity/
        │   ├── repository/
        │   ├── security/
        │   └── service/
        └── resources/
            ├── application.properties
            └── static/
```

## Run locally

Requirements:
- Java 23
- Maven 3.9+

From:

```text
TradeNest-SpringBoot-FullStack/backend
```

Run:

```powershell
mvn clean spring-boot:run
```

Then open:

```text
http://localhost:8081/
```

No separate frontend server is required.

## Build a JAR

```powershell
mvn clean package
```

Then run:

```powershell
java -jar target/zerodha-springboot-1.0.0.jar
```

## Docker

Build after creating the JAR:

```powershell
mvn clean package
cd backend
docker build -t tradenest .
docker run -p 8081:8081 -e JWT_SECRET="replace-with-a-long-production-secret" tradenest
```

For deployment, use a managed PostgreSQL database and provide:

```text
DB_URL
DB_DRIVER=org.postgresql.Driver
DB_USERNAME
DB_PASSWORD
JWT_SECRET
JWT_EXPIRATION_MS
PORT
```

Never use the development JWT secret in a public deployment.

## REST API

### Public
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/stocks`
- `GET /api/stocks/{symbol}`

### Protected
All user routes require:

```text
Authorization: Bearer <JWT>
```

- `GET /api/users/{id}/portfolio`
- `GET /api/users/{id}/orders`
- `GET /api/users/{id}/insights`
- `GET /api/users/{id}/analytics`
- `POST /api/users/{id}/buy`
- `POST /api/users/{id}/sell`
- `POST /api/users/{id}/what-if`
- `POST /api/users/{id}/quant/backtest`

## H2 Console

```text
http://localhost:8081/h2-console
```

Development connection:

```text
JDBC URL: jdbc:h2:file:./data/zerodha
User Name: sa
Password:
```

## Important notes

The market is simulated, not real. Prices are seeded initially and then changed by the scheduled simulation engine. P&L therefore becomes dynamic after the simulator runs.

The realized P&L analytics use the historical order sequence and average-cost method for the simulated account.

The project is not affiliated with Zerodha and must not be connected to real financial credentials or funds.
