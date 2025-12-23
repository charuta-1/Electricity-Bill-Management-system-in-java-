# VIT EnergySuite - Electricity Billing Platform

![VIT EnergySuite](frontend/src/assets/logo-vit-energysuite.png)

A comprehensive, production-ready electricity billing management system built with modern technologies. Features dual user portals (Admin & Customer), advanced tariff calculations, automated bill generation with PDF invoices and QR codes, comprehensive audit trails, and seamless cloud deployment capabilities.

## 🚀 Quick Highlights

- **Dual Role System**: Separate admin and customer portals with role-based access control
- **Advanced Billing Engine**: Slab-based tariff calculations with subsidies, surcharges, and late fees
- **Secure Authentication**: JWT-based authentication with Spring Security
- **PDF Invoice Generation**: Professional invoices with UPI QR codes using iText and ZXing
- **Database Migrations**: Flyway-managed schema with comprehensive seed data
- **Modern UI**: Responsive React dashboard with Bootstrap and interactive charts
- **Cloud-Ready**: Pre-configured for Render, Vercel, and Railway deployment
- **Audit & Compliance**: Complete audit logging for all system operations

## 📋 Table of Contents

1. [Features](#-features)
2. [Technology Stack](#-technology-stack)
3. [Prerequisites](#-prerequisites)
4. [Quick Start](#-quick-start)
5. [Database Setup](#-database-setup)
6. [Backend Configuration](#-backend-configuration)
7. [Frontend Setup](#-frontend-setup)
8. [API Documentation](#-api-documentation)
9. [Database Schema](#-database-schema)
10. [Deployment](#-deployment)
11. [Testing](#-testing)
12. [Troubleshooting](#-troubleshooting)
13. [Contributing](#-contributing)
14. [License](#-license)

## ✨ Features

### Admin Portal
- **Dashboard**: Real-time metrics and system overview
- **Customer Management**: Add, edit, and manage customer profiles
- **Account Management**: Handle meter connections and account details
- **Meter Readings**: Record and manage electricity consumption data
- **Bill Generation**: Automated bill creation with complex tariff calculations
- **Tariff Management**: Configure pricing slabs, charges, and subsidies
- **Complaint Resolution**: Handle and resolve customer complaints
- **User Administration**: Manage admin users and permissions
- **Audit Reports**: Comprehensive system activity logs

### Customer Portal
- **Dashboard**: Personal account overview and quick actions
- **Bill Viewing**: Access all bills with PDF download
- **Payment Processing**: Secure online bill payments
- **Usage Analytics**: Interactive charts showing consumption patterns
- **Complaint Filing**: Submit and track service complaints
- **Profile Management**: Update personal information

### Core Billing Features
- **Slab-Based Pricing**: Multiple tariff tiers based on consumption
- **Subsidy Support**: Configurable subsidy rules for different categories
- **Late Fee Calculation**: Automatic penalty calculation
- **Additional Charges**: Electricity duty, meter rent, and other fees
- **PDF Generation**: Professional invoices with QR codes for UPI payments
- **Multi-Connection Types**: Residential, Commercial, Industrial, Agricultural

## 🛠 Technology Stack

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17
- **Database**: MySQL 8.0+
- **ORM**: Spring Data JPA with Hibernate
- **Security**: Spring Security with JWT
- **Migrations**: Flyway
- **PDF Generation**: iText 7.2.5
- **QR Codes**: ZXing 3.5.2
- **Email**: Spring Boot Mail
- **Validation**: Bean Validation (JSR-303)

### Frontend
- **Framework**: React 18.3.1
- **Build Tool**: Vite 5.4.8
- **Routing**: React Router DOM 6.27.0
- **UI Library**: Bootstrap 5.3.3 + React Bootstrap 2.10.4
- **Charts**: Chart.js 4.4.6 + React Chart.js 2 5.3.0
- **Icons**: React Icons 5.3.0
- **HTTP Client**: Axios 1.7.7
- **Linting**: ESLint with React and accessibility plugins

### DevOps & Deployment
- **Containerization**: Docker
- **Backend Deployment**: Render
- **Frontend Deployment**: Vercel
- **Database**: Railway (MySQL)
- **CI/CD**: GitHub Actions (optional)

## 📋 Prerequisites

- **Java**: JDK 17 or higher
- **Node.js**: Version 16+ LTS (18+ recommended)
- **Maven**: 3.6+ (for backend builds)
- **MySQL**: 8.0+ (for local development)
- **Git**: For version control

### Optional but Recommended
- **Docker**: For containerized development
- **MySQL Workbench**: For database management
- **Postman/Insomnia**: For API testing

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/piyush-5328/VIT-billing-system-.git
cd VIT-billing-system-
```

### 2. Database Setup
```sql
-- Create database
CREATE DATABASE vit_billing;

-- Update credentials in backend/src/main/resources/application.properties
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
```

### 3. Backend Setup
```bash
cd backend
mvn clean package -DskipTests
java -jar target/billing-system-1.0.0.jar
```
The backend will start on `http://localhost:8080` and automatically run Flyway migrations.

### 4. Frontend Setup
```bash
cd frontend
npm install
npm run dev
```
The frontend will start on `http://localhost:5173` (Vite default).

### 5. Access the Application
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080/api
- **H2 Console** (dev mode): http://localhost:8080/h2-console

## 🗄 Database Setup

### MySQL Configuration (Recommended)
1. Install MySQL 8.0+
2. Create database:
```sql
CREATE DATABASE vit_billing;
```
3. Update `backend/src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/vit_billing?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### H2 In-Memory (Development Only)
For quick development without MySQL setup:
```properties
spring.datasource.url=jdbc:h2:mem:vit_billing;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true
```

Add H2 dependency to `pom.xml`:
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

## ⚙ Backend Configuration

### Application Properties
Key configuration options in `application.properties`:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/vit_billing
spring.datasource.username=root
spring.datasource.password=password

# JWT
jwt.secret=your-256-bit-secret-key-here
jwt.expiration=86400000

# CORS
cors.allowed.origins=http://localhost:5173,http://localhost:3000
cors.allowed.origin.patterns=https://*.vercel.app

# Email (optional)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
```

### Environment Variables (Production)
```bash
DATABASE_URL=jdbc:mysql://your-db-host:3306/vit_billing
DATABASE_USERNAME=your_db_user
DATABASE_PASSWORD=your_db_password
JWT_SECRET=your-secure-jwt-secret
CORS_ALLOWED_ORIGINS=https://your-frontend.vercel.app
```

## 🎨 Frontend Setup

### Development
```bash
cd frontend
npm install
npm run dev          # Start dev server
npm run build        # Production build
npm run preview      # Preview production build
npm run lint         # Run ESLint
```

### Key Files
- `src/App.js`: Main application component with routing
- `src/context/AuthContext.js`: Authentication state management
- `src/api/axiosConfig.js`: API client configuration
- `src/components/`: Reusable UI components
- `src/pages/`: Page components for different routes

### Environment Configuration
Create `.env` file in frontend directory:
```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## 📚 API Documentation

### Authentication Endpoints
```
POST   /api/auth/login          # User login
POST   /api/auth/register       # Customer registration
GET    /api/auth/validate       # JWT validation
POST   /api/auth/logout         # User logout
```

### Admin Endpoints
```
GET    /api/admin/dashboard     # Dashboard statistics
GET    /api/admin/customers     # List customers
POST   /api/admin/customers     # Create customer
PUT    /api/admin/customers/{id} # Update customer
DELETE /api/admin/customers/{id} # Delete customer

GET    /api/admin/accounts      # List accounts
POST   /api/admin/accounts      # Create account
PUT    /api/admin/accounts/{id} # Update account

POST   /api/admin/readings      # Add meter reading
GET    /api/admin/bills         # List bills
POST   /api/admin/bills/generate/{readingId} # Generate bill

GET    /api/admin/tariffs       # List tariffs
POST   /api/admin/tariffs       # Create tariff
PUT    /api/admin/tariffs/{id}  # Update tariff

GET    /api/admin/complaints    # List complaints
PUT    /api/admin/complaints/{id} # Update complaint status
```

### Customer Endpoints
```
GET    /api/customer/dashboard  # Customer dashboard data
GET    /api/customer/bills      # List customer bills
GET    /api/customer/bills/{id} # Get specific bill
POST   /api/customer/pay/{billId} # Pay bill
GET    /api/customer/usage      # Usage history
POST   /api/customer/complaints # File complaint
GET    /api/customer/complaints # List customer complaints
```

### Common Response Format
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2025-01-29T10:30:00Z"
}
```

## 🗃 Database Schema

### Core Tables

#### Authentication & Users
- `users`: User accounts with roles (ADMIN/CUSTOMER)
- `audit_logs`: System activity tracking

#### Customer Management
- `customers`: Customer profile information
- `accounts`: Meter connection details

#### Billing Configuration
- `tariff_master`: Tariff definitions
- `tariff_slabs`: Pricing tiers within tariffs
- `additional_charges`: System-wide charges
- `subsidy_rules`: Subsidy configurations
- `late_fee_policies`: Penalty calculation rules

#### Billing Operations
- `meter_readings`: Consumption data
- `bills`: Generated bill records
- `payments`: Payment transactions
- `complaints`: Customer service requests

### Key Relationships
```
Customer (1) ──── (M) Account
Account (1) ──── (M) Meter Reading
Account (1) ──── (M) Bill
Bill (1) ──── (M) Payment
Tariff (1) ──── (M) Slab
```

## 🚀 Deployment

### Backend (Render)
1. Connect GitHub repository to Render
2. Use `render.yaml` configuration
3. Set environment variables:
   - `DATABASE_URL`
   - `DATABASE_USERNAME`
   - `DATABASE_PASSWORD`
   - `JWT_SECRET`
   - `CORS_ALLOWED_ORIGINS`

### Frontend (Vercel)
1. Connect GitHub repository to Vercel
2. Set build settings:
   - **Root Directory**: `frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
3. Set environment variable:
   - `VITE_API_BASE_URL`: Your Render backend URL

### Database (Railway)
1. Create MySQL database on Railway
2. Copy connection details to Render environment variables
3. Run Flyway migrations on first deployment

### Docker Deployment
```bash
# Build backend image
cd backend
docker build -t vit-billing-backend .

# Run with MySQL
docker run -p 8080:8080 \
  -e DATABASE_URL=jdbc:mysql://host:3306/db \
  -e DATABASE_USERNAME=user \
  -e DATABASE_PASSWORD=pass \
  vit-billing-backend
```

## 🧪 Testing

### Backend Tests
```bash
cd backend
mvn test                    # Run all tests
mvn test -Dtest=TestClass   # Run specific test class
mvn verify                  # Run tests with coverage
```

### Frontend Tests
```bash
cd frontend
npm test                    # Run Jest tests
npm run test:coverage       # Run with coverage report
```

### Manual Testing
- **Default Admin**: `admin / admin123`
- **API Testing**: Use Postman collection in `docs/` folder
- **E2E Testing**: Manual testing checklist in `docs/testing.md`

## 🔧 Troubleshooting

### Common Issues

#### Backend Won't Start
**Error**: `Connection refused` or `Access denied for user`
**Solution**: Check MySQL credentials in `application.properties`

#### Frontend Shows CORS Error
**Error**: `CORS policy` blocked request
**Solution**: Update `cors.allowed.origins` in backend config

#### Database Migration Fails
**Error**: Flyway migration checksum error
**Solution**:
```bash
mvn flyway:clean
mvn flyway:migrate
```

#### Port Already in Use
**Error**: `Port 8080 is already in use`
**Solution**: Change port in `application.properties` or kill process using port

#### PDF Generation Issues
**Error**: PDF not generating or corrupted
**Solution**: Check file permissions and iText dependencies

### Performance Tuning
- Enable database connection pooling
- Configure Hibernate second-level cache
- Use Redis for session storage (optional)
- Enable GZIP compression

### Logs and Debugging
```bash
# Enable debug logging
logging.level.com.msedcl.billing=DEBUG
logging.level.org.springframework.security=DEBUG

# View application logs
tail -f logs/application.log
```

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/your-feature`
3. **Commit** changes: `git commit -m 'Add your feature'`
4. **Push** to branch: `git push origin feature/your-feature`
5. **Open** a Pull Request

### Development Guidelines
- Follow Java/Spring Boot best practices
- Write comprehensive unit tests
- Update documentation for new features
- Ensure all tests pass before submitting PR
- Use meaningful commit messages

### Code Style
- **Backend**: Google Java Style Guide
- **Frontend**: Airbnb React Style Guide
- **Commits**: Conventional commits format

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Spring Boot Team** for the excellent framework
- **React Community** for the powerful frontend library
- **Open Source Contributors** for libraries and tools
- **VIT University** for the project inspiration

## 📞 Support

For support and questions:
- Open an issue on GitHub
- Check the [Wiki](wiki) for detailed guides
- Review the [API Documentation](docs/api.md)

---

**Built with ❤️ for modern electricity billing management**

*VIT EnergySuite - Powering the future of utility billing* ⚡

bills: The master bill record, storing all calculated charges, PDF paths, and status (links to accounts and meter_readings).

payments: Records all payments made against accounts and bills.

complaints: Stores customer-filed complaints.

additional_charges: Defines system-wide charges like "Electricity Duty".

audit_logs: A log of all major actions taken in the system.

Key Relationships:
Customer -> Account (One-to-Many)
Account -> Bill, MeterReading, Payment (One-to-Many)
TariffMaster -> TariffSlab (One-to-Many)

10. Deployment on Render

This project is pre-configured for deployment on Render.

The root render.yaml file defines a complete deployment that will automatically:

Create a MySQL Database: A free-tier private database service named vit-db.

Deploy the Backend: A Spring Boot service named vit-backend. It automatically uses the backend/Dockerfile to build and run the application.

Deploy the Frontend: A static site service named vit-frontend. It builds the React app and serves the static files.

To Deploy:

Push this project to a GitHub repository.

Go to the Render dashboard and create a new "Blueprint" service.

Connect the GitHub repository you just created.

Render will automatically detect the render.yaml file.

Click "Apply" to deploy all three services.

Note: You must manually set the JWT_SECRET environment variable in the Render dashboard for the vit-backend service for a secure production deployment.

11. Troubleshooting

Error: "Port 8080 was already in use"

Fix: Another application is using port 8080. Stop that application, or change the port in backend/src/main/resources/application.properties by adding server.port=8081 (and update the frontend API URL in frontend/src/api/axiosConfig.js to match).

Error: "Connection refused" on backend start

Fix: Your MySQL database server is not running. Start your MySQL server and try running the backend again.

Error: "Access denied for user 'root'..."

Fix: The password in application.properties is incorrect. Double-check that it matches your local MySQL password.

Frontend shows "Unable to load dataset" (CORS Error)

Fix: This happens when the frontend (e.g., localhost:3000) tries to talk to the backend (localhost:8080). Your backend is already configured to allow this (cors.allowed.origins). Ensure your backend is running. If your frontend starts on a different port (e.g., 3001), you must add http://localhost:3001 to the cors.allowed.origins list in application.properties and restart the backend.

Error: "Could not save..." or "DataIntegrityViolationException"

Fix: You are trying to save data that violates a database rule. Check your Spring Boot terminal. The most common cause is trying to create a user with a username that already exists.