# StoreStack

StoreStack is an e-commerce application with a Spring Boot backend and a React, TypeScript, and Vite frontend.

## Project status

The backend implements registration and login, JWT access and rotating refresh tokens, role-based access, user profiles, a public product catalog, administrator category and product management, per-user carts, checkout, order history and status management. Dev uses H2 and seeds an administrator and sample catalog; the `prod` profile is configured for PostgreSQL.

The frontend now includes registration and login, a searchable product catalog, product details, cart and checkout flows, order history and details, and an administrator workspace for products, categories, and order status updates.

Phase 2 backend verification: `.\mvnw.cmd test` passed all 59 tests. Phase 3 frontend verification: `npm run build` completed successfully. Phase 4 frontend verification: `npm test` passed all 5 tests. The backend and frontend servers were not launched in this environment.

## Technology

- **Backend:** Java 17 target, Spring Boot 3.5.11, Spring MVC, Spring Security, Spring Data JPA, Lombok
- **Database:** H2 in the `dev` profile; PostgreSQL in the `prod` profile
- **API documentation:** Springdoc OpenAPI / Swagger UI
- **Frontend:** React, TypeScript, Vite, Axios, React Router
- **Build:** Maven Wrapper (`mvnw.cmd` on Windows)

## Requirements

- JDK 17 or newer
- Internet access the first time Maven downloads dependencies
- Node.js and npm

## Open in an IDE

Open the `E-Commerce` directory (the one containing `pom.xml`) in IntelliJ IDEA or VS Code. In IntelliJ, import it as a Maven project. In VS Code, install the Java Extension Pack and open the Maven project.

## Run the backend

From the project directory, run the backend with the Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

The default active profile is `dev`, which uses an in-memory H2 database.
The configured backend port is `8080`. Swagger UI is available at:

```text
http://localhost:8080/swagger-ui.html
```

## Run in IntelliJ IDEA

1. Open the project directory containing `pom.xml` as a Maven project.
2. Set the project SDK and Maven importer JDK to Java 17 or newer, then reload Maven.
3. Install the Lombok plugin, then enable annotation processing under **Settings > Build, Execution, Deployment > Compiler > Annotation Processors**.
4. Open **Run > Edit Configurations**, set **Active profiles** to `dev`, then run `src/main/java/E_Commerce/Website/ECommerceApplication.java`.
5. To run tests, use the test gutter icons or run `.\mvnw.cmd test` from the project directory.

No environment variables are required for local startup.

## Run in Eclipse

1. Choose **File > Import > Maven > Existing Maven Projects**, select the project directory, and finish the import.
2. Set the project JRE and Java compiler compliance to Java 17 or newer, then update the Maven project.
3. Install Lombok support through **Help > Eclipse Marketplace** by searching for Lombok, then restart Eclipse.
4. Open **Run Configurations > Java Application > Arguments**, add `--spring.profiles.active=dev` under **Program arguments**, then run `ECommerceApplication.java`.
5. To run tests, choose **Run As > JUnit Test** on a test class or run `.\mvnw.cmd test` from the project directory.

No environment variables are required for local startup.

## Database configuration

The `dev` profile uses an in-memory H2 database named `ecommercedb` with `create-drop`; data is cleared when the application stops. The `prod` profile uses PostgreSQL and requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`. `FRONTEND_ORIGIN` configures the allowed browser origin. Access-token and refresh-token lifetimes can be overridden with `JWT_ACCESS_EXPIRATION_MS` and `JWT_REFRESH_EXPIRATION_MS`.

The dev profile seeds an administrator with `admin@storestack.local` / `Admin12345` and sample categories and products. Override the administrator with `DEV_ADMIN_EMAIL` and `DEV_ADMIN_PASSWORD` if needed. Do not use development credentials or the dev signing key in production.

## Run the frontend

From the `frontend` directory:

```sh
npm install
npm run dev
```

Set `VITE_API_URL` in a local `.env` file to the backend API base URL. The example is `http://localhost:8080/api`; the frontend dev server uses port `5173`. The committed `.env.example` shows the expected variable. The frontend build can be checked with:

```sh
npm run build
```

The production build command was verified in this workspace. The dev server was not started here.

The frontend stores the authenticated session locally, attaches the access token to API calls, and attempts one refresh-token rotation after a 401 response. If refresh fails, it clears the local session and returns the user to the signed-out state.

## Tests

Run the backend tests from the project directory:

```powershell
.\mvnw.cmd test
```

Verified in this workspace after the Phase 2 backend changes: 59 tests passed, with no failures or errors. The test suite uses H2 and does not leave the application running.

Frontend production build verification: `npm run build` completed successfully.

Run frontend behavior tests from the `frontend` directory:

```sh
npm test
```

Verified in this workspace: 5 frontend tests passed, covering login/logout session persistence, refresh-token retry and failure handling, and cart quantity and clearing behavior.
