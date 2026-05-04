# AGENTS.md

## Project

ParkNet is a Java Maven Spring Boot full-stack information system for short-term rental of garages and parking spots in Sofia.

## Architecture Rules

- Use Controller-Service-Repository architecture.
- Controllers must be thin.
- Business logic belongs in services.
- Persistence logic belongs in repositories.
- DTO/form objects should be used for user input where helpful.
- Do not put business logic in Thymeleaf templates.
- Do not use Lombok.
- Keep code readable for a high-school Java project.

## Commands

- Run tests with: `mvn clean test`
- Run app with: `mvn spring-boot:run`

## Quality Rules

- Every feature must compile.
- Do not break existing routes.
- Do not remove tests.
- Add validation for all user input.
- Keep Bulgarian UI labels.
- Keep English class/method names.
- Add comments only where they clarify non-trivial decisions.

## Security Rules

- Passwords must be encoded with BCrypt.
- Only authenticated users can create listings and reservations.
- Users cannot reserve their own listing.
- Users should only edit/delete their own listings unless ADMIN.

## Done Means

- `mvn clean test` passes.
- App starts.
- Main user flows work manually.
