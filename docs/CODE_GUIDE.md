# How to read this app

The source files now include explanations beside the main functions, data structures,
and configuration. Start with the reading order below and follow the imports when
you want more detail.

## Follow one sales order through the app

1. Open `sales-order-punch-in-Frontend/src/App.tsx`. It connects the page components
   to shared data and decides which dialog is open.
2. Read `src/components/orders/OrderFormModal.tsx`. It holds the unsaved inputs,
   checks required values, reads optional files, and builds a sales-order object.
3. Follow `onSave` back into `App.tsx`, then into `src/hooks/useWorkspace.ts`.
   The hook waits for the backend before adding the saved order to React state.
4. Read `src/api.ts`. Its `createOrder` function sends a JSON `POST` request to
   `http://localhost:8080/api/orders` by default. Other API functions load orders
   and read or update settings.
5. Open the backend's `src/main/java/com/lankaswisstek/salesorder/order/SalesOrderController.java`.
   Spring converts the request JSON into a Java `SalesOrder`, validates annotated
   fields, checks the ID, and calls the repository.
6. Read `SalesOrderRepository.java` and `SalesOrder.java` in the same folder.
   The repository is the database access interface. The entity describes how each
   order maps to a row and its columns. Spring Data and Hibernate supply the
   database implementation.
7. The saved object comes back as JSON. The frontend hook updates its state, so
   `SalesOrdersPage.tsx` and `OrdersTable.tsx` render the new row.

## Common frontend terms

| Term | Meaning in this app |
| --- | --- |
| Component | A function that returns a piece of the interface, such as Sidebar or OrderFormModal. |
| Props | Values and callbacks a parent passes into a component. The `Props` type lists them. |
| State / `useState` | Values React remembers between renders, such as form inputs or the selected page. |
| Hook | A function that uses React features; `useWorkspace` shares loading and saving logic. |
| `useEffect` | Work that runs after rendering, with cleanup for listeners, timers, or late responses. |
| Callback | A function passed to a child so it can report an action back to its parent. |
| `async` / `await` | Wait for a file read or HTTP request without blocking the browser interface. |
| `Promise.all` | Run independent requests or file reads together and wait for all to finish. |
| Type | A compile-time description of expected fields; it does not validate server JSON at runtime. |
| JSX | The HTML-like markup returned by React components. Curly braces insert JavaScript values. |

## Common backend annotations

| Annotation / interface | Purpose |
| --- | --- |
| `@SpringBootApplication` | Starts automatic Spring setup and component discovery. |
| `@RestController` | Declares HTTP endpoint methods whose results become response bodies. |
| `@RequestMapping` | Sets a shared URL prefix for a controller. |
| `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping` | Select a handler using the HTTP method and URL. |
| `@RequestBody` | Converts incoming JSON into the Java method parameter. |
| `@PathVariable` | Reads a value from a URL segment, such as the order ID. |
| `@Valid`, `@NotBlank` | Run model validation and reject missing, empty, or whitespace-only required text. |
| `@Entity`, `@Table`, `@Id` | Identify a database model, its table, and its primary key. |
| `@Lob`, `CLOB` | Store large text values such as remarks and base64 attachments. |
| `JpaRepository` | Supplies common database operations without hand-written SQL implementations. |

## Settings and styling

- Settings follow the same path: SettingsModal → useWorkspace → api.ts →
  SettingsController → WorkspaceSettingsRepository → WorkspaceSettings.
- `WorkspaceSettings` stores one row per setting key. Salespeople are JSON text;
  the company name is plain text.
- `src/styles.css` imports the smaller CSS files. Their order matters: later rules
  can override earlier rules with the same specificity.
- Comments in each CSS file explain its purpose and the important layout groups.
- Backend runtime options are explained above the corresponding properties in
  `src/main/resources/application.properties`.
- Backend library dependencies are explained in `pom.xml` using XML comments.

## Configuration files and comments

- TypeScript/Java use `//` for a line comment and `/* ... */` for a block comment.
  Documentation comments begin with `/**`.
- CSS uses `/* ... */`, HTML/XML use `<!-- ... -->`, and `.properties` files use `#`.
- TypeScript permits comments in `tsconfig.json`.
- `package.json`, `package-lock.json`, and `.prettierrc.json` use strict JSON, so
  comments must not be inserted into them. The frontend package scripts mean:
  `dev` starts Vite, `build` runs type checking and creates production assets, and
  `preview` serves a built frontend. Prettier options control formatting only.
- Generated build files and third-party dependency code are not the source to edit.

## Check your changes

From the frontend folder, run `npm run build` to check TypeScript and build the UI.
From the backend folder, run `mvn compile` to compile the Java code. To run the
backend itself, use `mvn spring-boot:run`.
