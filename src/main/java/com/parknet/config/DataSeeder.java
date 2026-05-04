package com.parknet.config;

import com.parknet.model.District;
import com.parknet.model.ParkingListing;
import com.parknet.model.PricingType;
import com.parknet.model.Reservation;
import com.parknet.model.ReservationStatus;
import com.parknet.model.Role;
import com.parknet.model.UserAccount;
import com.parknet.repository.ParkingListingRepository;
import com.parknet.repository.ReservationRepository;
import com.parknet.repository.UserAccountRepository;
import com.parknet.service.GeoJsonService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataSeeder {

    private static final String CURRENCY_EUR = "€";

    @Bean
    public CommandLineRunner seedData(
            UserAccountRepository userAccountRepository,
            ParkingListingRepository parkingListingRepository,
            ReservationRepository reservationRepository,
            PasswordEncoder passwordEncoder,
            GeoJsonService geoJsonService
    ) {
        return args -> {
            UserAccount iva = findOrCreateUser(
                    userAccountRepository,
                    passwordEncoder,
                    "Ива Николова",
                    "iva@example.com",
                    "password",
                    "+359 88 123 4567",
                    Role.USER
            );
            UserAccount georgi = findOrCreateUser(
                    userAccountRepository,
                    passwordEncoder,
                    "Георги Петров",
                    "georgi@example.com",
                    "password",
                    "+359 89 234 5678",
                    Role.USER
            );
            UserAccount admin = findOrCreateUser(
                    userAccountRepository,
                    passwordEncoder,
                    "Администратор ParkNet",
                    "admin@parknet.bg",
                    "admin123",
                    "+359 87 000 0000",
                    Role.ADMIN
            );

            if (parkingListingRepository.count() > 0) {
                return;
            }

            LocalDate today = LocalDate.now();
            List<SeedListing> seedListings = List.of(
                    new SeedListing(iva, "Гараж до НДК", "Подземен гараж на две минути от НДК, сух и осветен, с дистанционно за бариерата.", District.CENTER, "бул. Витоша 74, София", "3.00", "24.00", PricingType.HOURLY, today, today.plusDays(45), 42.689900, 23.318700),
                    new SeedListing(georgi, "Паркомясто на ул. Солунска", "Вътрешно дворно място в центъра, удобно за кратък престой и вечерни събития.", District.CENTER, "ул. Солунска 31, София", "3.00", "22.00", PricingType.HOURLY, today, today.plusDays(30), 42.693200, 23.321100),
                    new SeedListing(admin, "Покрито място до Руски паметник", "Покрито място с лесен достъп от булеварда, подходящо за малък и среден автомобил.", District.CENTER, "бул. Ген. Скобелев 42, София", "3.00", "20.00", PricingType.HOURLY, today.minusDays(1), today.plusDays(60), 42.691600, 23.310800),

                    new SeedListing(georgi, "Паркомясто в Лозенец", "Наземно място с бариера близо до метростанция Европейски съюз.", District.LOZENETS, "ул. Крум Попов 22, София", "2.00", "18.00", PricingType.HOURLY, today, today.plusDays(35), 42.680900, 23.320500),
                    new SeedListing(iva, "Гараж до Семинарията", "Заключващ се гараж в тиха улица, удобен за живущи и гости на района.", District.LOZENETS, "ул. Богатица 18, София", "3.00", "21.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(50), 42.673600, 23.324100),
                    new SeedListing(admin, "Паркомясто до Южния парк", "Широко външно място до входа на парка, достъпно през целия ден.", District.LOZENETS, "ул. Козяк 12, София", "2.00", "17.00", PricingType.HOURLY, today, today.plusDays(25), 42.667800, 23.315900),

                    new SeedListing(iva, "Подземно място в Младост 1", "Подземно паркомясто в нова сграда до метростанция Младост 1.", District.MLADOST, "ж.к. Младост 1, бл. 48, София", "2.00", "14.00", PricingType.HOURLY, today.minusDays(2), today.plusDays(40), 42.651400, 23.374300),
                    new SeedListing(georgi, "Паркомясто до Бизнес парк връзка", "Наземно място с широк подход и добра видимост от улицата.", District.MLADOST, "ул. Йерусалим 9, София", "2.00", "13.00", PricingType.HOURLY, today, today.plusDays(32), 42.648900, 23.381700),
                    new SeedListing(admin, "Гараж в Младост 1А", "Самостоятелен гараж с ток и метална врата, подходящ за по-дълъг престой.", District.MLADOST, "ж.к. Младост 1А, бл. 506, София", "2.00", "12.00", PricingType.HOURLY, today.plusDays(2), today.plusDays(55), 42.646900, 23.386200),

                    new SeedListing(iva, "Паркомясто до УНСС", "Външно паркомясто в Студентски град, удобно за дневен престой около университетите.", District.STUDENTSKI, "ул. 8-ми декември 19, София", "2.00", "11.00", PricingType.HOURLY, today, today.plusDays(28), 42.649400, 23.342700),
                    new SeedListing(georgi, "Гараж до Зимен дворец", "Заключващ се гараж на спокойна улица, подходящ и за мотор.", District.STUDENTSKI, "ул. Акад. Борис Стефанов 14, София", "1.00", "12.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(46), 42.647900, 23.348600),
                    new SeedListing(admin, "Място до общежитията", "Малко дворно място близо до спирки и заведения, подходящо за кратки посещения.", District.STUDENTSKI, "ул. Проф. Христо Данов 6, София", "1.00", "9.00", PricingType.HOURLY, today, today.plusDays(21), 42.653200, 23.347100),

                    new SeedListing(georgi, "Гараж в Люлин 7", "Покрит гараж близо до метростанция Люлин, с удобен достъп от главен булевард.", District.LYULIN, "ж.к. Люлин 7, бл. 704, София", "1.00", "10.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(60), 42.717500, 23.253700),
                    new SeedListing(iva, "Паркомясто до Люлин център", "Наземно място до търговска зона, удобно за работни дни.", District.LYULIN, "бул. Джавахарлал Неру 28, София", "1.00", "9.00", PricingType.HOURLY, today, today.plusDays(35), 42.716200, 23.249300),
                    new SeedListing(admin, "Място в Люлин 3", "Паркомясто във вътрешен двор с контролиран вход.", District.LYULIN, "ж.к. Люлин 3, бл. 333, София", "1.00", "8.00", PricingType.HOURLY, today, today.plusDays(20), 42.720400, 23.257800),

                    new SeedListing(iva, "Място до Докторска градина", "Паркомясто в Оборище, подходящо за посещения около театри и посолства.", District.OBORISHTE, "ул. Оборище 88, София", "3.00", "25.00", PricingType.HOURLY, today, today.plusDays(55), 42.696500, 23.344600),
                    new SeedListing(georgi, "Гараж до парк Заимов", "Сух гараж с лесен заход от ул. Оборище и място за среден автомобил.", District.OBORISHTE, "ул. Буная 16, София", "3.00", "22.00", PricingType.HOURLY, today.plusDays(3), today.plusDays(70), 42.700500, 23.348200),
                    new SeedListing(admin, "Паркомясто до Александър Невски", "Компактно място близо до централните институции, удобно за кратък престой.", District.OBORISHTE, "ул. 11-ти август 9, София", "4.00", "28.00", PricingType.HOURLY, today, today.plusDays(18), 42.696900, 23.337600),

                    new SeedListing(georgi, "Гараж в Красно село", "Заключващ се гараж близо до пазара, подходящ за ежедневно ползване.", District.KRASNO_SELO, "ул. Хубча 6, София", "2.00", "15.00", PricingType.HOURLY, today, today.plusDays(45), 42.681600, 23.286700),
                    new SeedListing(iva, "Паркомясто до бул. Цар Борис III", "Външно място с удобен достъп от булеварда и достатъчна ширина за SUV.", District.KRASNO_SELO, "бул. Цар Борис III 124, София", "2.00", "13.00", PricingType.HOURLY, today.plusDays(2), today.plusDays(38), 42.680200, 23.281600),
                    new SeedListing(admin, "Покрито място в Борово", "Покрито място до жилищен вход, подходящо за нощуване и кратък наем.", District.KRASNO_SELO, "ул. Родопски извор 43, София", "2.00", "13.00", PricingType.HOURLY, today, today.plusDays(26), 42.677800, 23.289900),

                    new SeedListing(iva, "Паркомясто в Подуяне", "Място във вътрешен двор до голям транспортен възел, достъп с чип.", District.PODUYANE, "ул. Черковна 59, София", "2.00", "12.00", PricingType.HOURLY, today, today.plusDays(42), 42.708000, 23.350000),
                    new SeedListing(georgi, "Гараж до гара Подуяне", "Самостоятелен гараж на тиха улица, удобен за хора пътуващи с влак.", District.PODUYANE, "ул. Поп Грую 21, София", "2.00", "14.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(48), 42.706500, 23.356700),
                    new SeedListing(admin, "Място до парк Герена", "Наземно място близо до стадиона, удобно за събития и тренировки.", District.PODUYANE, "ул. Тодорини кукли 47, София", "1.00", "10.00", PricingType.HOURLY, today, today.plusDays(30), 42.711100, 23.351900),

                    new SeedListing(georgi, "Паркинг до Гео Милев", "Малка частна зона с маркирано място близо до парка и спирки.", District.GEO_MILEV, "ул. Гео Милев 36, София", "2.00", "14.00", PricingType.HOURLY, today, today.plusDays(44), 42.681000, 23.365000),
                    new SeedListing(iva, "Гараж до зала Фестивална", "Заключващ се гараж с лесен подход от бул. Шипченски проход.", District.GEO_MILEV, "ул. Николай Коперник 7, София", "2.00", "16.00", PricingType.HOURLY, today.plusDays(2), today.plusDays(52), 42.679200, 23.371400),
                    new SeedListing(admin, "Паркомясто до парк Гео Милев", "Широко място във вътрешен двор, удобно за вечерни посещения.", District.GEO_MILEV, "ул. Лидице 12, София", "2.00", "13.00", PricingType.HOURLY, today, today.plusDays(24), 42.682900, 23.361600),

                    new SeedListing(iva, "Гараж в Манастирски ливади", "Нов подземен гараж с автоматична врата и сух достъп от асансьор.", District.MANASTIRSKI_LIVADI, "ул. Ралевица 92, София", "2.00", "16.00", PricingType.HOURLY, today, today.plusDays(60), 42.660000, 23.295000),
                    new SeedListing(georgi, "Паркомясто до България мол", "Наземно място близо до бул. България, удобно за пазаруване и офис посещения.", District.MANASTIRSKI_LIVADI, "ул. Тодор Каблешков 53, София", "2.00", "16.00", PricingType.HOURLY, today, today.plusDays(30), 42.662800, 23.292100),
                    new SeedListing(admin, "Подземно място в Манастирски ливади", "Подземно място в нов комплекс с контролиран достъп.", District.MANASTIRSKI_LIVADI, "ул. Флора Кънева 10, София", "2.00", "17.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(50), 42.657200, 23.299400),

                    new SeedListing(iva, "Гараж в Иван Вазов", "Топъл гараж в кооперация до Южния парк, подходящ за ежедневен достъп.", District.IVAN_VAZOV, "ул. Нишава 27, София", "2.00", "16.00", PricingType.HOURLY, today, today.plusDays(42), 42.678300, 23.309100),
                    new SeedListing(georgi, "Паркомясто на ул. Балша", "Очертано дворно място с контролиран вход и широк подход от улицата.", District.IVAN_VAZOV, "ул. Балша 15, София", "2.00", "15.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(36), 42.676700, 23.304800),
                    new SeedListing(admin, "Подземен гараж до бул. Витоша", "Подземно място с асансьорен достъп, удобно за офиси около Иван Вазов.", District.IVAN_VAZOV, "бул. Витоша 168, София", "3.00", "19.00", PricingType.HOURLY, today, today.plusDays(58), 42.680600, 23.311800),

                    new SeedListing(georgi, "Гараж в Борово", "Самостоятелен гараж до жилищен блок, сух и удобен за малък автомобил.", District.BOROVO, "ул. Пирински проход 41, София", "2.00", "14.00", PricingType.HOURLY, today, today.plusDays(34), 42.672000, 23.286000),
                    new SeedListing(iva, "Паркомясто до бул. България", "Наземно място с лесен заход от булеварда, удобно за кратък престой.", District.BOROVO, "бул. България 88, София", "2.00", "15.00", PricingType.HOURLY, today.plusDays(2), today.plusDays(44), 42.669000, 23.289000),
                    new SeedListing(admin, "Покрито място в Борово", "Покрито паркомясто под жилищна сграда с вечерно осветление.", District.BOROVO, "ул. Солун 55, София", "2.00", "13.00", PricingType.HOURLY, today, today.plusDays(29), 42.667000, 23.281000),

                    new SeedListing(iva, "Гараж в Дианабад", "Заключващ се гараж до спортен комплекс, подходящ за автомобил или мотор.", District.DIANABAD, "ул. Никола Габровски 21, София", "2.00", "15.00", PricingType.HOURLY, today, today.plusDays(53), 42.671300, 23.352500),
                    new SeedListing(georgi, "Паркомясто до Ловния парк", "Външно място с бърз достъп до бул. Драган Цанков и парк зона.", District.DIANABAD, "ул. Апостол Карамитев 8, София", "2.00", "14.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(37), 42.668700, 23.348600),
                    new SeedListing(admin, "Подземно място до студентски блокове", "Подземно паркомясто в нова сграда, удобно за дългосрочен наем.", District.DIANABAD, "ул. Тинтява 31, София", "2.00", "16.00", PricingType.HOURLY, today, today.plusDays(62), 42.673400, 23.356100),

                    new SeedListing(georgi, "Гараж в Дружба 1", "Гараж до метростанция Дружба, подходящ за ежедневно паркиране.", District.DRUZHBA, "ж.к. Дружба 1, бл. 108, София", "1.00", "10.00", PricingType.HOURLY, today, today.plusDays(40), 42.666300, 23.397400),
                    new SeedListing(iva, "Паркомясто до езерото", "Наземно място близо до парк Дружба и спирки на градски транспорт.", District.DRUZHBA, "ул. Илия Бешков 12, София", "1.00", "9.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(33), 42.668500, 23.401200),
                    new SeedListing(admin, "Покрито място в Дружба", "Покрито място с бариера във вътрешен двор на жилищен комплекс.", District.DRUZHBA, "бул. Христофор Колумб 43, София", "2.00", "12.00", PricingType.HOURLY, today, today.plusDays(48), 42.663900, 23.392600),

                    new SeedListing(iva, "Гараж в Надежда 2", "Сух гараж на спокойна улица, удобен за живущи около метростанцията.", District.NADEZHDA, "ж.к. Надежда 2, бл. 243, София", "1.00", "9.00", PricingType.HOURLY, today, today.plusDays(45), 42.727600, 23.303800),
                    new SeedListing(georgi, "Паркомясто до Северен парк", "Външно място с лесен достъп от бул. Ломско шосе.", District.NADEZHDA, "бул. Ломско шосе 126, София", "1.00", "8.00", PricingType.HOURLY, today.plusDays(2), today.plusDays(31), 42.730100, 23.298900),
                    new SeedListing(admin, "Подземно място в Надежда", "Подземно паркомясто с дистанционно за гаражна врата.", District.NADEZHDA, "ул. Хан Кубрат 55, София", "1.00", "10.00", PricingType.HOURLY, today, today.plusDays(52), 42.724700, 23.307500),

                    new SeedListing(georgi, "Гараж в Банишора", "Самостоятелен гараж до централна зона, удобен за работещи около Сточна гара.", District.BANISHORA, "ул. Странджа 42, София", "2.00", "15.00", PricingType.HOURLY, today, today.plusDays(47), 42.711400, 23.315300),
                    new SeedListing(iva, "Паркомясто до Централна гара", "Очертано място във вътрешен двор с контрол на достъпа.", District.BANISHORA, "ул. Княз Борис I 188, София", "2.00", "14.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(28), 42.714200, 23.319200),
                    new SeedListing(admin, "Покрито място на ул. Опълченска", "Покрито място с удобен подход от главна улица и вечерно осветление.", District.BANISHORA, "ул. Опълченска 101, София", "2.00", "16.00", PricingType.HOURLY, today, today.plusDays(57), 42.708700, 23.310800),

                    new SeedListing(iva, "Гараж в Овча купел", "Заключващ се гараж близо до Нов български университет.", District.OVCHA_KUPEL, "ул. Монтевидео 25, София", "1.00", "10.00", PricingType.HOURLY, today, today.plusDays(39), 42.676200, 23.255600),
                    new SeedListing(georgi, "Паркомясто до минералната баня", "Наземно място в тиха част на квартала, удобно за кратки посещения.", District.OVCHA_KUPEL, "ул. Народно хоро 12, София", "1.00", "9.00", PricingType.HOURLY, today.plusDays(2), today.plusDays(49), 42.678900, 23.251500),
                    new SeedListing(admin, "Подземно място в Овча купел", "Подземно място с широк вход и контролиран достъп.", District.OVCHA_KUPEL, "ул. Любляна 46, София", "2.00", "12.00", PricingType.HOURLY, today, today.plusDays(61), 42.673700, 23.259800),

                    new SeedListing(georgi, "Гараж в Гоце Делчев", "Самостоятелен гараж близо до пазар и спирки, подходящ за ежедневен наем.", District.GOTSE_DELCHEV, "ул. Деян Белишки 52, София", "2.00", "15.00", PricingType.HOURLY, today, today.plusDays(41), 42.665400, 23.292300),
                    new SeedListing(iva, "Паркомясто до ул. Костенски водопад", "Наземно място със свободен подход и видимост от входа.", District.GOTSE_DELCHEV, "ул. Костенски водопад 21, София", "2.00", "14.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(35), 42.662600, 23.288900),
                    new SeedListing(admin, "Подземно място до бул. България", "Подземен гараж в нова сграда, подходящ за SUV.", District.GOTSE_DELCHEV, "бул. България 53, София", "2.00", "16.00", PricingType.HOURLY, today, today.plusDays(54), 42.667900, 23.296100),

                    new SeedListing(iva, "Гараж в Изток", "Сух гараж в дипломатическия район, удобен за офис посещения.", District.IZTOK, "ул. Незабравка 12, София", "3.00", "22.00", PricingType.HOURLY, today, today.plusDays(46), 42.667200, 23.351400),
                    new SeedListing(georgi, "Паркомясто до Интерпред", "Наземно място близо до метро и бизнес сгради.", District.IZTOK, "бул. Драган Цанков 36, София", "3.00", "20.00", PricingType.HOURLY, today.plusDays(2), today.plusDays(32), 42.669500, 23.348200),
                    new SeedListing(admin, "Подземно място до Борисовата градина", "Подземно място в сграда с охрана и бърз достъп до парка.", District.IZTOK, "ул. Антон П. Чехов 7, София", "3.00", "23.00", PricingType.HOURLY, today, today.plusDays(59), 42.664300, 23.355600),

                    new SeedListing(georgi, "Гараж в Света Троица", "Заключващ се гараж до тиха вътрешна улица, подходящ за нощуване.", District.SVETA_TROITSA, "ж.к. Света Троица, бл. 305, София", "1.00", "10.00", PricingType.HOURLY, today, today.plusDays(43), 42.704400, 23.289200),
                    new SeedListing(iva, "Паркомясто до парк Света Троица", "Очертано външно място близо до парк и трамвайни спирки.", District.SVETA_TROITSA, "ул. Цар Симеон 280, София", "1.00", "9.00", PricingType.HOURLY, today.plusDays(1), today.plusDays(30), 42.702100, 23.284700),
                    new SeedListing(admin, "Покрито място до бул. Сливница", "Покрито паркомясто с контролиран вход и осветление.", District.SVETA_TROITSA, "бул. Сливница 214, София", "2.00", "12.00", PricingType.HOURLY, today, today.plusDays(51), 42.706900, 23.293100)
            );

            ParkingListing firstListing = null;
            int imageIndex = 1;
            for (SeedListing seedListing : seedListings) {
                ParkingListing savedListing = parkingListingRepository.save(createListing(seedListing, geoJsonService, imageIndex));
                imageIndex++;
                if (firstListing == null) {
                    firstListing = savedListing;
                }
            }

            if (firstListing != null) {
                reservationRepository.save(new Reservation(
                        firstListing,
                        georgi,
                        today.plusDays(5),
                        today.plusDays(6),
                        firstListing.getPricePerDay().multiply(new BigDecimal("2")),
                        ReservationStatus.REQUESTED
                ));
            }
        };
    }

    private UserAccount findOrCreateUser(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            String fullName,
            String email,
            String password,
            String phone,
            Role role
    ) {
        return userAccountRepository.findByEmail(email)
                .orElseGet(() -> userAccountRepository.save(new UserAccount(
                        fullName,
                        email,
                        passwordEncoder.encode(password),
                        phone,
                        role
                )));
    }

    private ParkingListing createListing(SeedListing seed, GeoJsonService geoJsonService, int imageIndex) {
        String geometryGeoJson = geoJsonService.rectangleAround(seed.centerLatitude(), seed.centerLongitude());
        GeoJsonService.ValidatedGeometry geometry = geoJsonService.validateAndCalculateCenter(geometryGeoJson);
        ParkingListing listing = new ParkingListing(
                seed.owner(),
                seed.title(),
                seed.description(),
                seed.district(),
                seed.address(),
                money(seed.pricePerDay()),
                seed.availableFrom(),
                seed.availableTo(),
                seed.owner().getPhone(),
                demoImagePath(imageIndex),
                new BigDecimal(Double.toString(geometry.centerLatitude())),
                new BigDecimal(Double.toString(geometry.centerLongitude())),
                true
        );
        listing.setPricePerHour(money(seed.pricePerHour()));
        listing.setPricingType(seed.pricingType());
        listing.setCurrency(CURRENCY_EUR);
        listing.setGeometryGeoJson(geometry.geometryGeoJson());
        listing.setCenterLatitude(geometry.centerLatitude());
        listing.setCenterLongitude(geometry.centerLongitude());
        listing.setDemoListing(true);
        return listing;
    }

    private String demoImagePath(int imageIndex) {
        int variant = ((imageIndex - 1) % 6) + 1;
        return "/images/listings/parking-" + variant + ".svg";
    }

    private BigDecimal money(String value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(value);
    }

    private record SeedListing(
            UserAccount owner,
            String title,
            String description,
            District district,
            String address,
            String pricePerHour,
            String pricePerDay,
            PricingType pricingType,
            LocalDate availableFrom,
            LocalDate availableTo,
            double centerLatitude,
            double centerLongitude
    ) {
    }
}
