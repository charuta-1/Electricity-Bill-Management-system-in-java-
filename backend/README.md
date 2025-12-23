# VIT EnergySuite - Backend

Spring Boot backend for the VIT EnergySuite electricity platform

## Technology Stack

- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- Spring Security + JWT
- MySQL 8.0
- Flyway (Database Migrations)
- iText 7 (PDF Generation)
- ZXing (QR Code Generation)
- Maven

## Prerequisites

- JDK 17 or higher
- Maven 3.8+
- MySQL 8.0+

## Database Setup

1. Create MySQL database:
```sql
CREATE DATABASE vit_billing CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. Update `src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=YOUR_PASSWORD
```

## Build and Run

### Using Maven:
```bash
mvn clean install
mvn spring-boot:run
```

### Using JAR:
```bash
mvn clean package
java -jar target/billing-system-1.0.0.jar
```

### Using Docker:
```bash
docker build -t vit-billing-backend ..
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL="jdbc:mysql://host.docker.internal:3306/vit_billing" \
  -e SPRING_DATASOURCE_USERNAME="root" \
  -e SPRING_DATASOURCE_PASSWORD="your_password" \
  -e JWT_SECRET="change_me" \
  vit-billing-backend
```

> **Tip:** Change the environment variables to match your database and security configuration. `host.docker.internal` works on Windows and macOS for accessing a MySQL instance running on the host.

## Deploying to Render

1. Commit the repository (Render can auto-deploy from Git).
2. Create a **Web Service** in Render pointing at this repo.
3. Use the following settings:

  | Setting | Value |
  | --- | --- |
  | Root Directory | `backend` |
  | Build Command | `mvn -DskipTests package` |
  | Start Command | `java -jar target/billing-system-1.0.0.jar` |
  | Runtime | Java |
  | Region | Closest to your users |

4. Add required environment variables (Render Dashboard → Environment):

  - `DATABASE_URL` – JDBC URL for your MySQL instance (Render external DB or other provider)
  - `DATABASE_USERNAME`
  - `DATABASE_PASSWORD`
  - `JWT_SECRET` – at least 32 characters; use a secure random string
  - `JWT_EXPIRATION` (optional) – defaults to `86400000`
  - `CORS_ALLOWED_ORIGINS` – e.g. `https://your-frontend.vercel.app`
  - `CORS_ALLOWED_ORIGIN_PATTERNS` – keep `https://*.vercel.app` for preview builds

5. Render automatically injects the `PORT` variable; the application uses it via `server.port=${PORT:8080}`.
6. Deploy. The backend will be accessible at `https://<service-name>.onrender.com/api`.

For infrastructure-as-code, you can use the provided `render.yaml` at the repository root.

## Application Structure

```
src/main/java/com/msedcl/billing/
├── BillingSystemApplication.java    # Main application
├── config/                           # Configuration classes
│   └── CorsConfig.java
├── controller/                       # REST Controllers
│   ├── AuthController.java
│   ├── MeterReadingController.java
│   └── BillController.java
├── dto/                             # Data Transfer Objects
│   ├── LoginRequest.java
│   ├── AuthResponse.java
│   ├── MeterReadingRequest.java
│   ├── PaymentRequest.java
│   └── RegisterRequest.java
├── entity/                          # JPA Entities
│   ├── User.java
│   ├── Customer.java
│   ├── Account.java
│   ├── TariffMaster.java
│   ├── TariffSlab.java
│   ├── MeterReading.java
│   ├── Bill.java
│   ├── Payment.java
│   ├── Complaint.java
│   ├── AuditLog.java
│   └── AdditionalCharge.java
├── repository/                      # Data Access Layer
│   ├── UserRepository.java
│   ├── CustomerRepository.java
│   ├── AccountRepository.java
│   ├── TariffMasterRepository.java
│   ├── TariffSlabRepository.java
│   ├── MeterReadingRepository.java
│   ├── BillRepository.java
│   ├── PaymentRepository.java
│   ├── ComplaintRepository.java
│   ├── AuditLogRepository.java
│   └── AdditionalChargeRepository.java
├── security/                        # Security & JWT
│   ├── JwtUtil.java
│   ├── JwtRequestFilter.java
│   ├── CustomUserDetailsService.java
│   └── SecurityConfig.java
└── service/                         # Business Logic
  ├── BillingService.java          # Bill generation
  ├── RegistrationService.java     # Customer self-registration
  ├── CustomerService.java         # Customer management
  ├── PdfService.java              # PDF generation
  └── QrCodeService.java           # QR code generation

src/main/resources/
├── application.properties           # Configuration
└── db/schema/                       # Flyway migrations
  └── V1__vit_billing_schema.sql
```

## Database Migrations

Migrations run automatically on application startup using Flyway.

To run manually:
```bash
mvn flyway:migrate
```

To reset database:
```bash
mvn flyway:clean
mvn flyway:migrate
```

## Default Users

Created automatically on first run:

**Admin:**
- Username: `admin`
- Password: `admin123`

**Customers:**
- Register via `POST /api/auth/register`
- Passwords stored with BCrypt hashing

## API Endpoints

### Authentication
- `POST /api/auth/register` - Customer self-registration (returns JWT + user info)
- `POST /api/auth/login` - Login
- `GET /api/auth/validate` - Validate token

### Meter Readings (Admin)
- `POST /api/admin/readings` - Add meter reading
- `GET /api/admin/readings/account/{accountId}` - Get readings by account
- `GET /api/admin/readings/{id}` - Get reading by ID

### Bills (Admin)
- `POST /api/admin/bills/generate/{readingId}` - Generate bill
- `GET /api/admin/bills/account/{accountId}` - Get bills by account
- `GET /api/admin/bills/{id}` - Get bill by ID
- `GET /api/admin/bills/invoice/{invoiceNumber}` - Get bill by invoice
- `GET /api/admin/bills` - Get all bills

## Configuration

### Database
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vit_billing
spring.datasource.username=root
spring.datasource.password=root
```

### JWT
```properties
jwt.secret=vit-billing-secret-key-change-this-in-production
jwt.expiration=86400000
```

### File Storage
```properties
pdf.storage.path=./bills/invoices/
```

### CORS
```properties
cors.allowed.origins=http://localhost:3000,http://localhost:5173
```

## Features

### Billing Calculation
- Slab-based tariff calculation
- Fixed charges and meter rent
- Electricity duty (16%)
- Fuel adjustment charge (5%)
- Late fee calculation
- Previous due handling

### PDF Generation
- Professional bill format
- Complete charge breakdown
- Customer and account details
- Payment instructions

### QR Code
- UPI payment QR code
- Embedded in bill
- Contains payment amount and reference

### Security
- JWT token authentication
- BCrypt password hashing
- Role-based access control
- CORS configuration

### Audit Trail
- All operations logged
- User tracking
- IP address recording
- Entity-level tracking

## Development

### Hot Reload
```bash
mvn spring-boot:run
```

Spring DevTools enables automatic restart on code changes.

### Debug Mode
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

### View SQL Queries
Set in application.properties:
```properties
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

## Testing

Run tests:
```bash
mvn test
```

Skip tests during build:
```bash
mvn clean install -DskipTests
```

## Production Build

```bash
mvn clean package -DskipTests
java -jar target/billing-system-1.0.0.jar --spring.profiles.active=prod
```

## Environment Variables

Can be set as environment variables instead of properties file:

```bash
export MYSQL_PASSWORD=your_password
export JWT_SECRET=your_secret_key
java -jar app.jar
```

## Troubleshooting

### Port Already in Use
Change port in application.properties:
```properties
server.port=8081
```

### Database Connection Failed
1. Verify MySQL is running
2. Check username/password
3. Ensure database exists

### Flyway Migration Failed
Drop and recreate database:
```sql
DROP DATABASE vit_billing;
CREATE DATABASE vit_billing;
```

### PDF/QR Generation Failed
Create directory:
```bash
mkdir -p bills/invoices
```

## Logging

Logs are written to console and file (if configured).

Log levels:
```properties
logging.level.root=INFO
logging.level.com.msedcl.billing=DEBUG
logging.level.org.springframework.security=DEBUG
```

## Dependencies

Key dependencies from pom.xml:
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-security
- mysql-connector-j
- flyway-core
- flyway-mysql
- jjwt-api (JWT)
- itext7-core (PDF)
- zxing-core (QR Code)

## API Testing

### Using curl:

Login:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

### Render Smoke Tests

After deploying to Render (replace `your-service` with the actual service name):

```bash
# Expect 401 (unauthenticated) but confirms service is reachable
curl -i https://your-service.onrender.com/api/auth/validate

# Login against Render backend
curl -X POST https://your-service.onrender.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# Example of calling a protected route once you have a token
curl https://your-service.onrender.com/api/admin/bills \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

Add Reading (with token):
```bash
curl -X POST http://localhost:8080/api/admin/readings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "accountId": 1,
    "currentReading": 150,
    "billingMonth": "2025-01",
    "readingType": "ACTUAL"
  }'
```

## Contributing

This is an academic project. For improvements:
1. Fork the repository
2. Create feature branch
3. Make changes
4. Test thoroughly
5. Submit pull request

## License

Academic project for educational purposes.

## Support

For issues or questions:
- Check SETUP_INSTRUCTIONS.md
- Review PROJECT_SUMMARY.md
- Verify configuration in application.properties

---

**Version**: 1.0.0
**Last Updated**: January 2025
