# ParkNet

<p align="center">
  <img src="src/main/resources/static/images/brand/parknet-logo.png" alt="ParkNet logo" width="140">
</p>

<h3 align="center">Платформа за краткосрочен наем на гаражи и паркоместа в София</h3>

<p align="center">
  <a href="#технологии"><img alt="Java 21" src="https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white"></a>
  <a href="#технологии"><img alt="Spring Boot 3.3.5" src="https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?logo=springboot&logoColor=white"></a>
  <a href="#стартиране"><img alt="Maven" src="https://img.shields.io/badge/Maven-build-C71A36?logo=apachemaven&logoColor=white"></a>
  <a href="#тестове"><img alt="Tests" src="https://img.shields.io/badge/tests-mvn%20clean%20test-2E7D32"></a>
</p>

---

## Обзор

ParkNet е Java Maven Spring Boot информационна система за отдаване под наем на частни гаражи и паркоместа. Приложението е насочено към София и помага на собствениците да публикуват свободни места, а на шофьорите да намират, разглеждат и заявяват резервации за удобни локации.

Проектът не е статична HTML страница. Той има Spring Boot backend, H2 база данни, Spring Security автентикация, Bean Validation, Thymeleaf изгледи, Leaflet карта и автоматизирани тестове.

## Какво може приложението

| Зона | Възможности |
| --- | --- |
| Обяви | Публичен списък, детайлна страница, снимка, квартал, адрес, период на наличност и статус |
| Карта | Интерактивна Leaflet карта с координати за София, GeoJSON граници и маркери |
| Филтри | Търсене по квартал, дата и максимална цена |
| Собственици | Създаване, редакция, деактивиране и управление на собствени обяви |
| Резервации | Заявка от наемател, потвърждение или отказ от собственик |
| Потребители | Регистрация, вход, роли `USER` и `ADMIN`, BCrypt пароли |
| Сигурност | Защитени маршрути, контрол на собствеността и забрана за резервация на собствена обява |

## Основни екрани

- Начална страница с активни обяви и карта: `/` и `/listings`
- Детайли за обява: `/listings/{id}`
- Нова обява: `/listings/new`
- Моите обяви: `/my-listings`
- Моите резервации: `/my-reservations`
- Заявки към собственика: `/reservation-requests`
- Вход и регистрация: `/login`, `/register`

## Технологии

- Java 21
- Maven
- Spring Boot 3.3.5
- Spring MVC
- Spring Security
- Spring Data JPA / Hibernate
- H2 in-memory database
- Thymeleaf
- HTML и CSS
- Leaflet с OpenStreetMap/CARTO tiles
- JUnit, Spring Boot Test и MockMvc

## Архитектура

Проектът следва ясен Controller-Service-Repository модел:

```text
src/main/java/com/parknet
├── config       # Security, seed data, global model attributes, web resources
├── controller   # Thin MVC controllers and route handling
├── dto          # Form and request objects with validation
├── model        # JPA entities and enums
├── repository   # Spring Data JPA repositories
└── service      # Business logic, validation rules, maps, images, reservations
```

Контролерите приемат заявките и връщат Thymeleaf изгледи. Service слоят съдържа бизнес правилата за обяви, резервации, изображения, потребители и карти. Repository слоят се грижи за постоянството на данните чрез Spring Data JPA.

## Бизнес правила

- Само автентикирани потребители могат да създават обяви и резервации.
- Потребител не може да резервира собствената си обява.
- Собственикът управлява само своите обяви, освен ако потребителят е `ADMIN`.
- Паролите се записват единствено като BCrypt хешове.
- Формите валидират задължителни полета, дати, цени, GeoJSON геометрия и качени изображения.
- Качените изображения са ограничени до JPG, JPEG, PNG и WEBP до 5MB.

## Стартиране

Изисквания:

- Java 21
- Maven 3.9+ или Maven wrapper/локална Maven инсталация

Стартиране на тестовете:

```bash
mvn clean test
```

Стартиране на приложението:

```bash
mvn spring-boot:run
```

След това отвори:

```text
http://localhost:8080
```

За H2 console при локална разработка:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Адрес:

```text
http://localhost:8080/h2-console
```

## Демо данни

При празна база приложението автоматично създава примерни потребители, активни обяви за различни квартали в София и една примерна заявка за резервация.

| Потребител | Имейл | Парола | Роля |
| --- | --- | --- | --- |
| Ива Николова | `iva@example.com` | `password` | `USER` |
| Георги Петров | `georgi@example.com` | `password` | `USER` |
| Администратор ParkNet | `admin@parknet.bg` | `admin123` | `ADMIN` |

Демо паролите са само за локално тестване. В базата те се записват като BCrypt хешове.

## Тестове

Проектът съдържа тестове за:

- зареждане на Spring контекста;
- регистрация и валидиране на потребители;
- BCrypt кодиране на пароли;
- създаване, редакция и филтриране на обяви;
- проверка на дати, цени и GeoJSON данни;
- правила за резервации;
- публични и защитени маршрути чрез MockMvc;
- seed данни и основни service сценарии.

Команда:

```bash
mvn clean test
```

## Структура на проекта

```text
src
├── main
│   ├── java/com/parknet
│   └── resources
│       ├── static
│       └── templates
└── test
    ├── java/com/parknet
    └── resources
```

## Защо ParkNet е пълноценна информационна система

ParkNet има реална backend логика, persistent модел, роли, защита, бизнес правила, валидация и тестове. HTML и CSS изграждат интерфейса, но основната функционалност се изпълнява от Spring Boot приложението, което обработва заявки, записва данни, изчислява цени, управлява достъп и поддържа работните потоци за собственици и наематели.
