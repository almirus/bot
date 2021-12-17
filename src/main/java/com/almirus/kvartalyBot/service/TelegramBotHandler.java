package com.almirus.kvartalyBot.service;

import com.almirus.kvartalyBot.dal.entity.Apartment;
import com.almirus.kvartalyBot.dal.entity.Owner;
import com.almirus.kvartalyBot.dal.entity.TempOwner;
import com.almirus.kvartalyBot.util.Permission;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.*;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatInviteLink;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

@EqualsAndHashCode(callSuper = true)
@Component
@Data
@Slf4j
public class TelegramBotHandler extends TelegramLongPollingBot {

    @Autowired
    BuildProperties buildProperties;

    private final String INFO_LABEL = "🤖 О боте";
    private final String ACCESS_LABEL = "🔐 Запросить доступ";
    private final String ABOUT_BUILDING_LABEL = "🏢 О доме";
    private final String ABOUT_ROOM_LABEL = "🏠 О квартире";
    private final String BOT_LABEL = "Чего нового";
    private final String START_LABEL = "🏡 В начало";
    private final String BEGIN_LABEL = "Согласен✅, начать ";
    private final String SEND_TO_ADMIN_LABEL = "✅ Отправить данные";
    private final String SEND_TO_ADMIN_CANCEL_LABEL = "🚫 Отменить отправку";
    private final String FIND_LABEL = "🔎 Найти соседей";
    private final String ABOUT_2119_LABEL = "Справочная 21/19";
    private final String FIND_NEIGHBOR2_LABEL = "👨👩2х ближайших";
    private final String FIND_FLOOR_NEIGHBOR_LABEL = "👨👩На этаже";
    private final String FIND_ENTRANCE_NEIGHBOR_LABEL = "👨👩По стояку";
    // todo весь текст перенести вверх в константы

    // временный владелец
    private final TempOwnerService tempOwnerService;
    // реальный владелец
    private final OwnerService ownerService;

    private final ApartmentService apartmentService;


    private enum COMMANDS {
        ADD_USER("/add"),
        REMOVE_USER("/remove_user"),
        BAN("/ban"),

        INFO("/info"),
        START("/start"),
        DEMO("/demo"),
        ACCESS("/access"),
        SUCCESS("/success"),
        ABOUT_ROOM("/about"),
        ABOUT_BUILDING("/about_building"),
        BOT("/bot"),
        BEGIN("/begin"),
        SEND("/send_to_admin"),
        DELETE("/delete"),
        CAR_EXIST("/car_exist"),
        CAR_NOT_EXIST("/car_not_exist"),
        PHONE_EXIST("/phone_exist"),
        PHONE_NOT_EXIST("/phone_not_exist"),
        FIND_NEIGHBORS("/find_neighbors"),
        FIND_2NEIGHBORS("/find_two_neighbors"),
        FIND_FLOOR_NEIGHBORS("/find_floor_neighbors"),
        FIND_ENTRANCE_NEIGHBORS("/find_entrance_neighbors"),
        WHO_IS("/whois");


        private final String command;

        COMMANDS(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }
    }

    @Value("${telegram.support.chat-id}")
    private String supportChatId;

    @Value("${telegram.name}")
    private String name;

    @Value("${telegram.token}")
    private String token;

    @Value("${telegram.chanel-id}")
    private String privateChannelId;

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    public void onUpdateReceived(Update update) {
        //!update.getMessage().getChatId().equals(Long.parseLong(privateChannelId)) - выключает бота в закрытом чате, но будет доступен в приватном чате с ботом
        if (update.hasMessage() && !update.getMessage().getChatId().equals(Long.parseLong(privateChannelId)) && (update.getMessage().hasText() || update.getMessage().hasContact())) { //текст, ответ боту, отправка контакта
            // в этой ветке обрабатываются в текстовые сообщения (Text) боту, текстовые сообщения с цитированием (Reply), сообщения содержание контакт пользователя (Contact)
            String text = update.getMessage().hasText() ? update.getMessage().getText() : "";
            String reply = update.getMessage().getReplyToMessage() != null ? update.getMessage().getReplyToMessage().getText() : "";
            String phone = update.getMessage().hasContact() ? update.getMessage().getContact().getPhoneNumber() : "";
            long telegramUserId = update.getMessage().getChatId();

            try {
                //todo Нужен рефакторинг!
                Pattern patternFloor = Pattern.compile("^этаж$");
                Pattern patternRoom = Pattern.compile("^квартира$");
                Pattern patternName = Pattern.compile("^имя$");
                Pattern patternPhone = Pattern.compile("^телефон (нажмите кнопку внизу) ⬇️$");
                Pattern patternCar = Pattern.compile("^номер машиноместа$");
                //если на запрос номера ввели телефон руками как обычный текст, а не нажали кнопку
                Pattern patternCustomPhone = Pattern.compile("^((8|\\+7)[\\- ]?)?(\\(?\\d{3}\\)?[\\- ]?)?[\\d\\- ]{7,10}$");

                Matcher matcherFloor = patternFloor.matcher(reply);
                Matcher matcherRoom = patternRoom.matcher(reply);
                Matcher matcherName = patternName.matcher(reply);
                Matcher matcherPhone = patternPhone.matcher(reply);
                Matcher matcherCar = patternCar.matcher(reply);
                // введенный руками текст приходит не как ответ, а как обычный текст
                Matcher matcherCustomPhone = patternCustomPhone.matcher(text);
                if (matcherFloor.find()) {
                    sendFloorInfo(Integer.parseInt(text), String.valueOf(telegramUserId));
                    SendMessage message = handleAccessRoomCommand(String.valueOf(telegramUserId));
                    message.enableHtml(true);
                    message.setParseMode(ParseMode.HTML);
                    message.setChatId(String.valueOf(telegramUserId));
                    execute(message);
                } else if (matcherRoom.find()) {
                    sendRoomInfo(Integer.parseInt(text), String.valueOf(telegramUserId));
                    SendMessage message = handleAccessNameCommand(String.valueOf(telegramUserId));
                    message.enableHtml(true);
                    message.setParseMode(ParseMode.HTML);
                    message.setChatId(String.valueOf(telegramUserId));
                    execute(message);
                } else if (matcherName.find()) {
                    sendNameInfo(!"".equals(text) ? text : update.getMessage().getFrom().getUserName(), String.valueOf(telegramUserId));
                    SendMessage message = handleAccessPhoneExistCommand(String.valueOf(telegramUserId));
                    //SendMessage message = handleAccessPhoneCommand(String.valueOf(telegramUserId));
                    message.enableHtml(true);
                    message.setParseMode(ParseMode.HTML);
                    message.setChatId(String.valueOf(telegramUserId));
                    execute(message);
                } else if (matcherPhone.find() || !"".equals(phone) || matcherCustomPhone.find()) { // странно, но у некоторых контакт уходит без reply текста
                    sendPhoneInfo(!"".equals(phone) ? phone : matcherCustomPhone.group(0), String.valueOf(telegramUserId));
                    SendMessage message = handleAccessCarExistCommand(String.valueOf(telegramUserId));
                    message.enableHtml(true);
                    message.setParseMode(ParseMode.HTML);
                    message.setChatId(String.valueOf(telegramUserId));
                    execute(message);
                } else if (matcherCar.find()) {
                    sendCarPlaceInfo(text, String.valueOf(telegramUserId));
                    SendMessage message = handleSuccessCommand(String.valueOf(telegramUserId));
                    message.enableHtml(true);
                    message.setParseMode(ParseMode.HTML);
                    message.setChatId(String.valueOf(telegramUserId));
                    execute(message);
                } else {
                    SendMessage message = getCommandResponse(text, String.valueOf(telegramUserId), String.valueOf(telegramUserId));
                    message.enableHtml(true);
                    message.setParseMode(ParseMode.HTML);
                    message.setChatId(String.valueOf(telegramUserId));
                    execute(message);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
                try {
                    sendDebugToSupport("Error " + e.getMessage());
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            } catch (NumberFormatException e) {
                try {
                    // если ошибка начинаем сначала
                    SendMessage message = handleAccessFloorCommand(String.valueOf(telegramUserId));
                    execute(message);
                    sendDebugToSupport("Error " + e.getMessage());
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        } else if (update.hasCallbackQuery() && !update.getCallbackQuery().getMessage().getChatId().equals(Long.parseLong(privateChannelId))) {
            // в этой ветке обрабатываются нажатия на кнопки, содержащие CallbackQuery
            try {
                Pattern commandPattern = Pattern.compile("(/[a-z_]+)/?(\\d+)?");
                Matcher matcherCommand = commandPattern.matcher(update.getCallbackQuery().getData());
                if (matcherCommand.find()) {
                    SendMessage message = getCommandResponse(matcherCommand.group(1), matcherCommand.group(2), String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
                    message.enableHtml(true);
                    message.setParseMode(ParseMode.HTML);
                    message.setChatId(String.valueOf(update.getCallbackQuery().getMessage().getChatId()));
                    execute(message);
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
                try {
                    sendDebugToSupport("Error " + e.getMessage());
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    // todo Нужен рефакторинг, объединить в одну функцию сохранения
    private void sendFloorInfo(Integer floor, String telegramUserId) {
        TempOwner tmpOwner;
        if (tempOwnerService.isUserExist(telegramUserId)) {
            tmpOwner = tempOwnerService.getUser(telegramUserId);
        } else {
            tmpOwner = new TempOwner();
            tmpOwner.setTelegramId(telegramUserId);
        }
        tmpOwner.setFloor(floor);
        tempOwnerService.add(tmpOwner);

    }

    private void sendRoomInfo(Integer room, String telegramUserId) {
        TempOwner tmpOwner;
        if (tempOwnerService.isUserExist(telegramUserId)) {
            tmpOwner = tempOwnerService.getUser(telegramUserId);
        } else {
            tmpOwner = new TempOwner();
            tmpOwner.setTelegramId(telegramUserId);
        }
        tmpOwner.setRealNum(room);
        tempOwnerService.add(tmpOwner);
    }

    private void sendNameInfo(String name, String telegramUserId) {
        TempOwner tmpOwner;
        if (tempOwnerService.isUserExist(telegramUserId)) {
            tmpOwner = tempOwnerService.getUser(telegramUserId);
        } else {
            tmpOwner = new TempOwner();
            tmpOwner.setTelegramId(telegramUserId);
        }
        tmpOwner.setName(name);
        tempOwnerService.add(tmpOwner);
    }

    private void sendPhoneInfo(String phone, String telegramUserId) {
        TempOwner tmpOwner;
        if (tempOwnerService.isUserExist(telegramUserId)) {
            tmpOwner = tempOwnerService.getUser(telegramUserId);
        } else {
            tmpOwner = new TempOwner();
            tmpOwner.setTelegramId(telegramUserId);
        }
        if (!"".equals(phone)) {
            // удаляем лишние
            phone = phone.replaceAll("[^0-9]", "");
            // заменяем 8 на 7
            if (phone.charAt(0) == '8') phone = "7" + phone.substring(1);
            tmpOwner.setPhoneNum("+" + phone);
        } else {
            tmpOwner.setPhoneNum("");
        }
        tempOwnerService.add(tmpOwner);
    }

    private void sendCarPlaceInfo(String carPlace, String telegramUserId) {
        TempOwner tmpOwner;
        if (tempOwnerService.isUserExist(telegramUserId)) {
            tmpOwner = tempOwnerService.getUser(telegramUserId);
        } else {
            tmpOwner = new TempOwner();
            tmpOwner.setTelegramId(telegramUserId);
        }
        tmpOwner.setCarPlace(carPlace);
        tempOwnerService.add(tmpOwner);
    }


    private void sendRequestToSupport(String message, String userId, boolean addFlag) throws TelegramApiException {
        InlineKeyboardButton inlineKeyboardButtonApprove = new InlineKeyboardButton();
        inlineKeyboardButtonApprove.setText("✅ Добавить");
        inlineKeyboardButtonApprove.setCallbackData(COMMANDS.ADD_USER.getCommand() + "/" + userId);

        InlineKeyboardButton inlineKeyboardButtonCancel = new InlineKeyboardButton();
        inlineKeyboardButtonCancel.setText("🚫 Отказать");
        inlineKeyboardButtonCancel.setCallbackData(COMMANDS.REMOVE_USER.getCommand() + "/" + userId);

        InlineKeyboardButton inlineKeyboardButtonBan = new InlineKeyboardButton();
        inlineKeyboardButtonBan.setText("👊 Забанить");
        inlineKeyboardButtonBan.setCallbackData(COMMANDS.BAN.getCommand() + "/" + userId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        // нужны кнопки?
        if (addFlag) {
            keyboardButtonsRow1.add(inlineKeyboardButtonApprove);
            keyboardButtonsRow1.add(inlineKeyboardButtonCancel);
            keyboardButtonsRow1.add(inlineKeyboardButtonBan);
        }
        ;

        keyboardButtons.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(keyboardButtons);
        sendInfoToSupportAdmins(message, inlineKeyboardMarkup);
    }

    private void sendInfoToUser(String telegramId, String message, InlineKeyboardMarkup inlineKeyboardMarkup) throws TelegramApiException {
        SendMessage messageForUser = new SendMessage();
        messageForUser.setText(message);
        messageForUser.setChatId(telegramId);
        if (inlineKeyboardMarkup != null) messageForUser.setReplyMarkup(inlineKeyboardMarkup);
        messageForUser.enableHtml(true);
        execute(messageForUser);
    }

    private void sendInfoToSupportAdmins(String message, InlineKeyboardMarkup inlineKeyboardMarkup) throws TelegramApiException {
        // отправляем всем админам чата
        getChatAdministrators(privateChannelId).forEach(user -> {
            try {
                sendInfoToUser(String.valueOf(user.getUser().getId()), message, inlineKeyboardMarkup);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendDebugToSupport(String message) throws TelegramApiException {
        sendInfoToUser(supportChatId, message, null);
    }

    /***
     * Обработка всех сообщений боту, после парсинга
     * @param text - сообщение, возможно с кнопками
     * @param userId - ид пользователя, который "привязан к кнопке", идет после слэша (/command/userId) см переменную commandPattern в {@link #onUpdateReceived(Update) onUpdateReceived}
     * @param telegramUserId - ид пользователя который общается с ботом
     * @return
     * @throws TelegramApiException
     */
    private SendMessage getCommandResponse(String text, String userId, String telegramUserId) throws TelegramApiException {

        if (text.equals(COMMANDS.INFO.getCommand())) {
            return handleInfoCommand();
        }

        if (text.equals(COMMANDS.ACCESS.getCommand())) {
            return handleAccessCommand(telegramUserId);
        }

        if (text.equals(COMMANDS.START.getCommand())) {
            return handleStartCommand(telegramUserId);
        }

        if (text.equals(COMMANDS.ABOUT_ROOM.getCommand())) {
            return handleAboutRoomCommand(telegramUserId);
        }
        if (text.equals(COMMANDS.ABOUT_BUILDING.getCommand())) {
            return handleAboutBuildingCommand(telegramUserId);
        }
        if (text.equals(COMMANDS.BOT.getCommand())) {
            return handleBotCommand(telegramUserId);
        }

        if (text.equals(COMMANDS.BEGIN.getCommand())) {
            return handleAccessFloorCommand(telegramUserId);
        }

        if (text.equals(COMMANDS.SEND.getCommand())) {
            return handleSendToAdminCommand(userId, telegramUserId);
        }
        if (text.equals(COMMANDS.DELETE.getCommand())) {
            return handleDeleteDataCommand(userId, telegramUserId);
        }
        if (text.equals(COMMANDS.CAR_NOT_EXIST.getCommand())) {
            sendCarPlaceInfo("Нет", String.valueOf(telegramUserId));
            return handleSuccessCommand(String.valueOf(telegramUserId));
        }
        if (text.equals(COMMANDS.CAR_EXIST.getCommand())) {
            return handleAccessCarCommand(telegramUserId);
        }
        if (text.equals(COMMANDS.PHONE_NOT_EXIST.getCommand())) {
            sendPhoneInfo("", String.valueOf(telegramUserId));
            return handleAccessCarExistCommand(String.valueOf(telegramUserId));
        }
        if (text.equals(COMMANDS.PHONE_EXIST.getCommand())) {
            return handleAccessPhoneCommand(telegramUserId);
        }
        if (text.equals(COMMANDS.ADD_USER.getCommand())) {
            return handleAccessAddCommand(userId, telegramUserId);
        }
        if (text.equals(COMMANDS.BAN.getCommand())) {
            return handleAccessBanCommand(userId, telegramUserId);
        }
        if (text.equals(COMMANDS.FIND_NEIGHBORS.getCommand())) {
            return handleAccessFindNeighborsCommand(userId, telegramUserId);
        }

        if (text.equals(COMMANDS.FIND_2NEIGHBORS.getCommand())) {
            return handleAccessFind2NeighborsCommand(userId, telegramUserId);
        }
        if (text.equals(COMMANDS.FIND_FLOOR_NEIGHBORS.getCommand())) {
            return handleAccessFindFloorNeighborsCommand(userId, telegramUserId);
        }
        if (text.equals(COMMANDS.FIND_ENTRANCE_NEIGHBORS.getCommand())) {
            return handleAccessFindEntranceNeighborsCommand(userId, telegramUserId);
        }
        if (text.equals(COMMANDS.REMOVE_USER.getCommand())) {
            return handleAccessRemoveUserCommand(userId, telegramUserId);
        }
        return handleNotFoundCommand(telegramUserId);
    }

    private SendMessage handleNotFoundCommand(String telegramUserId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setText("Вы что-то сделали не так или я вас не понял. Выберите команду:");
        message.setReplyMarkup(getDefaultKeyboard(telegramUserId));
        return message;
    }

    private String getChatInviteLink() throws TelegramApiException {
        CreateChatInviteLink createChatInviteLink = new CreateChatInviteLink();
        createChatInviteLink.setChatId(privateChannelId);
        createChatInviteLink.setMemberLimit(1);
        ChatInviteLink chatInviteLink = execute(createChatInviteLink);
        return chatInviteLink.getInviteLink();
    }

    private SendMessage handleAboutRoomCommand(String telegramUserId) throws TelegramApiException {
        SendMessage message = new SendMessage();

        if (ownerService.isUserExist(telegramUserId)) {

            Owner user = ownerService.getUser(telegramUserId);
            String userStr = String.format("""
                    👤   Логин/Имя: <b>%s</b>
                    ☎   Номер телефона: <b><a href="tel:%s">%s</a></b>
                    """, user.getName(), user.getPhoneNum(), user.getPhoneNum());

            String apartStr = user.getApartmentList().stream().map(item -> String.format("""
                    Номер подъезда: <b>%s</b>
                    Номер этажа: <b>%s</b>
                    Номер квартиры: <b>%s</b>, по ДДУ: <b>%s</b>
                    Комнат: <b>%s</b>
                    Площадь по БТИ: <b>%s</b>, разница с договором: <b>%sм2</b>
                    """, item.getEntrance(), item.getFloor(), item.getId(), item.getDduNum(), item.getRoom(), item.getRealArea(), item.getDifference())).collect(joining("\n"));

            message.setText(userStr + apartStr);

        } else {
            message.setText("Я вас не нашел, запросите доступ");
        }
        message.enableHtml(true);
        message.setReplyMarkup(getDefaultKeyboard(telegramUserId));

        return message;
    }

    private SendMessage handleAboutBuildingCommand(String telegramUserId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        String info = """
                Наш дом находится по адресу <a href="https://yandex.ru/maps/-/CCUujETt~B">109518, 2-й Грайвороновский пр-д. д44.к.2</a>
                Документы на дом находятся <a href="https://2119.ru/about/docs/">тут</a>
                Дом имеет 11 подъездов, не смотря на отдельные здания, это один дом и имеет один почтовый адрес.
                 """;
        message.setText(info);
        message.enableHtml(true);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> keyboardButtons = getDefaultKeyboard(telegramUserId).getKeyboard();
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        InlineKeyboardButton inlineKeyboardButtonRoom = new InlineKeyboardButton();
        inlineKeyboardButtonRoom.setText(ABOUT_ROOM_LABEL);
        inlineKeyboardButtonRoom.setCallbackData(COMMANDS.ABOUT_ROOM.getCommand());
        keyboardButtonsRow1.add(inlineKeyboardButtonRoom);
        keyboardButtons.add(keyboardButtonsRow1);
        inlineKeyboardMarkup.setKeyboard(keyboardButtons);
        message.setReplyMarkup(inlineKeyboardMarkup);

        return message;
    }

    private SendMessage handleBotCommand(String telegramUserId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setText("""
                ✨ Добавлена ссылка на справочную по кварталу 21/19
                ✨ Бот может искать соседей
                ✨ Бот умеет выдавать информацию о недвижимости
                ✨ Первая версия бота
                ✨ Привет человек
                """);
        message.setReplyMarkup(getDefaultKeyboard(telegramUserId));
        return message;
    }

    private SendMessage handleStartCommand(String telegramUserId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setText("Доступные команды:");
        message.setReplyMarkup(getDefaultKeyboard(telegramUserId));
        return message;
    }

    private SendMessage handleInfoCommand() {
        SendMessage message = new SendMessage();
        message.setText(String.format("""
                После авторизации вы сможете найти своих соседей по этажу и стояку, а также другую информацию, получить доступ к закрытому чату.
                Функционал будет расширяться!
                Вопросы, пожелания, донаты 🥯 @<a href="tg://user?id=153968771">Almirus</a>
                Версия от %s
                """, buildProperties.getTime()));
        message.enableHtml(true);
        InlineKeyboardButton inlineKeyboardButtonBot = new InlineKeyboardButton();
        inlineKeyboardButtonBot.setText(BOT_LABEL);

        inlineKeyboardButtonBot.setCallbackData(COMMANDS.BOT.getCommand());
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
        keyboardButtons.add(Collections.singletonList(inlineKeyboardButtonBot));
        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        message.setReplyMarkup(inlineKeyboardMarkup);
        return message;
    }

    private SendMessage handleAccessCommand(String telegramUserId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        // есть в основной таблице
        if (ownerService.isUserExist(telegramUserId)) {
            message.setText("""
                    Вы уже зарегистрированы, вам доступен весь функционал.
                    """);
            message.setReplyMarkup(getDefaultKeyboard(telegramUserId));
            return message;
        }
        // есть во временной таблице
        if (tempOwnerService.isUserExist(telegramUserId) && tempOwnerService.isDataComplete(telegramUserId)) {
            message.setText("""
                    Вы уже подавали заявку на доступ. Подождите...
                    """);
            message.setReplyMarkup(getDefaultKeyboard(telegramUserId));
            return message;
        }
        message.setText("""
                Далее укажите, пожалуйста, <b>номер этажа</b>, <b>квартиры</b> (почтовый), Ваше <b>имя или никнейм</b>, Ваш <b>номер телефона</b>.
                Эти данные нужны, чтобы избавиться от спама, а также иметь возможность получить дополнительный функционал. Данные будут доступны только администратору.
                После проверки предоставленных сведений, Вам придёт ссылка на закрытый чат дома 44/2.
                <a href="http://563603-cd36585.tmweb.ru/privacy.pdf">Политика конфиденциальности</a>                           
                ❗ На данный момент бот не поддерживает регистрацию владельцев нежилых помещений.
                """);
        message.enableHtml(true);
        message.disableWebPagePreview();
        InlineKeyboardButton inlineKeyboardButtonBegin = new InlineKeyboardButton();
        inlineKeyboardButtonBegin.setText(BEGIN_LABEL);

        inlineKeyboardButtonBegin.setCallbackData(COMMANDS.BEGIN.getCommand());
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
        keyboardButtons.add(Collections.singletonList(inlineKeyboardButtonBegin));
        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        message.setReplyMarkup(inlineKeyboardMarkup);
        return message;
    }

    private SendMessage handleAccessFloorCommand(String telegramUserId) {

        return SendMessage.builder().text("этаж").chatId(telegramUserId).replyMarkup(ForceReplyKeyboard.builder().forceReply(true).build()).build();

    }

    private SendMessage handleAccessRoomCommand(String telegramUserId) {

        return SendMessage.builder().text("квартира").chatId(telegramUserId).replyMarkup(ForceReplyKeyboard.builder().forceReply(true).build()).build();

    }

    private SendMessage handleAccessNameCommand(String telegramUserId) {

        return SendMessage.builder().text("имя").chatId(telegramUserId).replyMarkup(ForceReplyKeyboard.builder().forceReply(true).build()).build();

    }

    private SendMessage handleAccessPhoneExistCommand(String telegramUserId) {

        InlineKeyboardButton inlineKeyboardButtonCancel = new InlineKeyboardButton();
        inlineKeyboardButtonCancel.setText("🛑 нет");
        inlineKeyboardButtonCancel.setCallbackData(COMMANDS.PHONE_NOT_EXIST.getCommand());

        InlineKeyboardButton inlineKeyboardButtonBegin = new InlineKeyboardButton();
        inlineKeyboardButtonBegin.setText("✅️ да");
        inlineKeyboardButtonBegin.setCallbackData(COMMANDS.PHONE_EXIST.getCommand());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonBegin);
        keyboardButtonsRow1.add(inlineKeyboardButtonCancel);

        keyboardButtons.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        return SendMessage.builder().text("номер телефона нужен для экстренной связи в случае ЧП, поделитесь им?").chatId(telegramUserId).replyMarkup(inlineKeyboardMarkup).build();

    }

    private SendMessage handleAccessPhoneCommand(String telegramUserId) {

        KeyboardButton keyboardButton = new KeyboardButton("""
                ✅ Отправить свой номер телефона
                (будет виден только администратору)
                """);
        keyboardButton.setRequestContact(true);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardButtons = new KeyboardRow();
        keyboardButtons.add(keyboardButton);
        keyboard.add(keyboardButtons);
        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setResizeKeyboard(false);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        return SendMessage.builder().text("телефон (нажмите кнопку внизу) ⬇️").chatId(telegramUserId).replyMarkup(replyKeyboardMarkup).build();

    }

    private SendMessage handleAccessCarExistCommand(String telegramUserId) {

        InlineKeyboardButton inlineKeyboardButtonBegin = new InlineKeyboardButton();
        inlineKeyboardButtonBegin.setText("нет купленного места");
        // пустое машиноместо
        inlineKeyboardButtonBegin.setCallbackData(COMMANDS.CAR_NOT_EXIST.getCommand());

        InlineKeyboardButton inlineKeyboardButtonCancel = new InlineKeyboardButton();
        inlineKeyboardButtonCancel.setText("🚙 есть");
        //запросим ввод потом, следующий шаг
        inlineKeyboardButtonCancel.setCallbackData(COMMANDS.CAR_EXIST.getCommand());

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonBegin);
        keyboardButtonsRow1.add(inlineKeyboardButtonCancel);

        keyboardButtons.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        return SendMessage.builder().text("покупали место на парковке").chatId(telegramUserId).replyMarkup(inlineKeyboardMarkup).build();

    }

    private SendMessage handleAccessCarCommand(String telegramUserId) {

        return SendMessage.builder().text("номер машиноместа").chatId(telegramUserId).replyMarkup(ForceReplyKeyboard.builder().forceReply(true).build()).build();

    }

    private SendMessage handleAccessBanCommand(String userId, String telegramUserId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(userId);
        if (getRole(telegramUserId).equals(Permission.ADMIN) || getRole(telegramUserId).equals(Permission.OWNER)) {
            try {
                BanChatMember banChatMember = new BanChatMember();
                banChatMember.setChatId(privateChannelId);
                banChatMember.setUserId(Long.valueOf(userId));
                execute(banChatMember);
                sendInfoToSupportAdmins(String.format("""
                        <a href="tg://user?id=%s">Пользователь</a> был забанен <a href="tg://user?id=%s">админом</a>, он более не сможет подавать заявку через бота
                        """, userId, telegramUserId), null);
                Owner owner = ownerService.getUser(userId);
                TempOwner tmpOwner = tempOwnerService.getUser(userId);
                tempOwnerService.delete(tmpOwner);
                owner.getApartmentList().clear();
                ownerService.delete(owner);
            } catch (TelegramApiException e) {
                try {
                    sendDebugToSupport("Ошибка при добавлении пользователя в бан: " + e.getMessage());
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        }
        message.setText("⚠⚠⚠ ️Мы были вынуждены вас забанить! Очень жаль.");
        return message;
    }

    private SendMessage handleAccessAddCommand(String userId, String telegramUserId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(userId);
        if (getRole(telegramUserId).equals(Permission.ADMIN) || getRole(telegramUserId).equals(Permission.OWNER)) {
            if (tempOwnerService.isUserExist(userId)) {
                try {
                    UnbanChatMember unbanChatMember = new UnbanChatMember();
                    unbanChatMember.setChatId(privateChannelId);
                    unbanChatMember.setOnlyIfBanned(true);
                    unbanChatMember.setUserId(Long.valueOf(userId));
                    execute(unbanChatMember);
                } catch (TelegramApiException e) {
                    try {
                        sendDebugToSupport("Ошибка при удалении пользователя из бана: " + e.getMessage());
                    } catch (TelegramApiException ex) {
                        ex.printStackTrace();
                    }
                }
                sendInfoToUser(userId, String.format("""
                                            
                        Вам выдан полный доступ, нажмите 
                        ➡️➡️➡️<a href="%s">ЗДЕСЬ</a>⬅️⬅️⬅️
                                                
                        """, getChatInviteLink()), null);
                message.setText(String.format("Выдан полный доступ для <a href=\"tg://user?id=%s\">пользователя</a>", userId));
                TempOwner tempOwner = tempOwnerService.getUser(userId);
                addOwnerToDb(tempOwner, telegramUserId);
            } else {
                message.setText(String.format("⚠️Похоже, другой админ уже обработал заявку <a href=\"tg://user?id=%s\">пользователя</a>", userId));
            }
        } else {
            message.setText("⚠️У вас нет прав доступа к этому функционалу!");
        }
        return message;
    }

    private SendMessage handleSendToAdminCommand(String userId, String telegramUserId) throws TelegramApiException {
        TempOwner tmpOwner = tempOwnerService.getUser(telegramUserId);
        Apartment apartment = apartmentService.getApartment(tmpOwner.getRealNum());
        Apartment apartmentDDU = apartmentService.getApartmentDDU(tmpOwner.getRealNum());
        String owners = apartment.getOwnerList().size() > 0 ? apartment.getOwnerList().stream().map(item -> String.format("""
                <a href="tg://user?id=%s">%s</a>
                """, item.getTelegramId(), item.getName())).collect(joining(", ")) : "Пока нет";
        SendMessage messageSuccess = new SendMessage();
        boolean addFlag = false;
        String status, dduStr = "";
        // TODO здесь ветка автоапрува пользователя, если совпали его данные из базы квартиры
        if (tmpOwner.getFloor().equals(apartment.getFloor()) && tmpOwner.getRealNum().equals(apartment.getId())) {
            messageSuccess.setText("🎉 Добро пожаловать в чат!");
            status = "Пользователь получил полный доступ ✅автоматически";
            sendInfoToUser(userId, String.format("""
                                
                    Вам выдан полный доступ, нажмите 
                    ➡️➡️➡️<a href=\"%s\">ЗДЕСЬ</a>⬅️⬅️⬅️
                                
                    """, getChatInviteLink()), null);
            // активирует запись бот, записываем его ID
            addOwnerToDb(tmpOwner, token.substring(0, token.indexOf(":")));
        } else {
            messageSuccess.setText("""
                    🎉 Ваши данные получены. Идет проверка...
                    Слишком долго? Напишите совету дома.
                    """);
            status = "Пользователь запросил полный доступ, введенные данные отличаются ❗от плана дома❗";
            addFlag = true;
            dduStr = String.format("❓Проверка по квартире ДДУ: Подъезд: %s, Этаж: %s Квартира: %s, Номер квартиры по ДДУ: <b>%s</b>",
                    apartmentDDU.getEntrance(), apartmentDDU.getFloor(), apartmentDDU.getId(), apartmentDDU.getDduNum());
        }
        messageSuccess.setChatId(String.valueOf(telegramUserId));
        messageSuccess.setReplyMarkup(getDefaultKeyboard(telegramUserId));
        String phoneNum;
        if (!"".equals(tmpOwner.getPhoneNum())) {
            //маскируем - конфиденциальность
            phoneNum = tmpOwner.getPhoneNum().replaceAll("^(.{5}).{5}(.*)$", "$1***$2");
        } else {
            phoneNum = "не предоставлен";
        }

        sendRequestToSupport(String.format("""
                                %s
                                Telegram аккаунт: <a href="tg://user?id=%s">%s</a>
                                Были введены данные: Этаж: %s Квартира: %s Телефон: %s Машиноместо: %s
                                --------------------            
                                В нашей базе по этой квартире: Подъезд: %s, Этаж: %s Квартира: %s, Номер квартиры по ДДУ: %s
                                %s
                                Другие владельцы: %s
                                🌏 Это сообщение получили все админы приватного чата. Телефон замаскирован в целях конфиденциальности.
                                """,
                        status,
                        userId, tmpOwner.getName(),
                        tmpOwner.getFloor(), tmpOwner.getRealNum(), phoneNum, tmpOwner.getCarPlace(),
                        apartment.getEntrance(), apartment.getFloor(), apartment.getId(), apartment.getDduNum(),
                        dduStr,
                        owners),
                userId, addFlag);
        return messageSuccess;
    }

    private SendMessage handleDeleteDataCommand(String userId, String telegramUserId) throws TelegramApiException {
        TempOwner tmpOwner = tempOwnerService.getUser(telegramUserId);
        SendMessage messageSuccess = new SendMessage();
        tempOwnerService.delete(tmpOwner);
        messageSuccess.setText("Ваши данные не сохранены и удалены, вы можете повторить регистрацию позже");
        messageSuccess.setChatId(String.valueOf(telegramUserId));
        messageSuccess.setReplyMarkup(getDefaultKeyboard(telegramUserId));
        return messageSuccess;
    }

    private SendMessage handleSuccessCommand(String userId) {
        SendMessage message = new SendMessage();
        TempOwner tmpOwner = tempOwnerService.getUser(userId);
        message.setText(String.format("""
                        Почти все готово. Вы ввели следующую информацию:
                                                
                        Ваш аккаунт: <a href="tg://user?id=%s">%s</a>
                        Этаж: %s Квартира: %s Телефон: %s Машиноместо: %s
                                                
                        Если ошиблись, нажмите 
                        🚫Отменить отправку и заполните анкету с самого начала.
                        Если все правильно, нажмите 
                        ✅Отправить данные 
                        """, userId, tmpOwner.getName(),
                tmpOwner.getFloor(), tmpOwner.getRealNum(), tmpOwner.getPhoneNum(), tmpOwner.getCarPlace()));
        InlineKeyboardButton inlineKeyboardButtonBegin = new InlineKeyboardButton();
        inlineKeyboardButtonBegin.setText(SEND_TO_ADMIN_LABEL);
        inlineKeyboardButtonBegin.setCallbackData(COMMANDS.SEND.getCommand() + "/" + userId);

        InlineKeyboardButton inlineKeyboardButtonCancel = new InlineKeyboardButton();
        inlineKeyboardButtonCancel.setText(SEND_TO_ADMIN_CANCEL_LABEL);
        inlineKeyboardButtonCancel.setCallbackData(COMMANDS.DELETE.getCommand());


        inlineKeyboardButtonBegin.setCallbackData(COMMANDS.SEND.getCommand() + "/" + userId);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonBegin);
        keyboardButtonsRow1.add(inlineKeyboardButtonCancel);

        keyboardButtons.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        message.setReplyMarkup(inlineKeyboardMarkup);
        return message;
    }

    private SendMessage handleAccessFindNeighborsCommand(String user, String telegramUserId) {
        if (!ownerService.isUserExist(telegramUserId)) {
            SendMessage messageSuccess = new SendMessage();
            messageSuccess.setText("⚠️У вас нет доступа к данному функционалу!");
            messageSuccess.setChatId(String.valueOf(telegramUserId));
            return messageSuccess;
        }
        SendMessage messageSuccess = new SendMessage();
        messageSuccess.setChatId(String.valueOf(telegramUserId));
        messageSuccess.setText("Поиск соседей среди тех кто зарегистрирован через бота");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton inlineKeyboardNeighbor2Button = new InlineKeyboardButton();
        inlineKeyboardNeighbor2Button.setText(FIND_NEIGHBOR2_LABEL);
        inlineKeyboardNeighbor2Button.setCallbackData(COMMANDS.FIND_2NEIGHBORS.getCommand());

        InlineKeyboardButton inlineKeyboardFloorButton = new InlineKeyboardButton();
        inlineKeyboardFloorButton.setText(FIND_FLOOR_NEIGHBOR_LABEL);
        inlineKeyboardFloorButton.setCallbackData(COMMANDS.FIND_FLOOR_NEIGHBORS.getCommand());

        InlineKeyboardButton inlineKeyboardEntranceButtonAbout = new InlineKeyboardButton();
        inlineKeyboardEntranceButtonAbout.setText(FIND_ENTRANCE_NEIGHBOR_LABEL);
        inlineKeyboardEntranceButtonAbout.setCallbackData(COMMANDS.FIND_ENTRANCE_NEIGHBORS.getCommand());

        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        keyboardButtonsRow1.add(inlineKeyboardNeighbor2Button);
        keyboardButtonsRow1.add(inlineKeyboardFloorButton);
        keyboardButtonsRow1.add(inlineKeyboardEntranceButtonAbout);
        keyboardButtons.add(keyboardButtonsRow1);
        inlineKeyboardMarkup.setKeyboard(keyboardButtons);
        messageSuccess.setReplyMarkup(inlineKeyboardMarkup);
        return messageSuccess;
    }

    private SendMessage handleAccessFind2NeighborsCommand(String user, String telegramUserId) {
        if (!ownerService.isUserExist(telegramUserId)) {
            SendMessage messageSuccess = new SendMessage();
            messageSuccess.setText("⚠️У вас нет доступа к данному функционалу!");
            messageSuccess.setChatId(String.valueOf(telegramUserId));
            return messageSuccess;
        }
        SendMessage messageSuccess = new SendMessage();
        messageSuccess.setChatId(String.valueOf(telegramUserId));
        StringBuilder sb = new StringBuilder();
        Owner owner = ownerService.getUser(telegramUserId);
        List<Apartment> apartmentList = owner.getApartmentList();
        apartmentList.forEach(ownerApartment -> {
            sb.append("\nВаши соседи по квартире №").append(ownerApartment.getId());
            // все квартиры в подъезде и на этаже
            List<Apartment> floorApartList = apartmentService.getFloorApartments(ownerApartment.getFloor(), ownerApartment.getEntrance());
            OptionalInt result = IntStream.range(0, floorApartList.size())
                    .filter(x -> ownerApartment.getId().equals(floorApartList.get(x).getId()))
                    .findFirst();
            Apartment apartmentLeft = null;
            Apartment apartmentRight = null;
            if (result.isPresent()) {
                int index = result.getAsInt();

                if (index - 1 >= 0) apartmentLeft = floorApartList.get(index - 1);
                if (index + 1 <= floorApartList.size() - 1) apartmentRight = floorApartList.get(index + 1);

                if (apartmentLeft != null)
                    sb.append("\nСосед(и) из квартиры №").append(apartmentLeft.getId()).append(" ").append(apartmentLeft.getOwnerList().size() > 0 ? " зарегистрирован(ы) " +
                            apartmentLeft.getOwnerList().stream().map(neighbor ->
                                    String.format("<a href=\"tg://user?id=%s\">%s</a>", neighbor.getTelegramId(), neighbor.getName())).collect(joining(","))
                            : " еще не зарегистрирован(ы) через бота.");
                if (apartmentRight != null)
                    sb.append("\nСосед(и) из квартиры №").append(apartmentRight.getId()).append(" ").append(apartmentRight.getOwnerList().size() > 0 ? " зарегистрирован(ы) " +
                            apartmentRight.getOwnerList().stream().map(neighbor ->
                                    String.format("<a href=\"tg://user?id=%s\">%s</a>", neighbor.getTelegramId(), neighbor.getName())).collect(joining(", "))
                            : " еще не зарегистрирован(ы) через бота.");
            } else {
                try {
                    sendDebugToSupport("запрошены соседи квартиры " + ownerApartment.getRoom() + ", но не найдены данные на этаже");
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        });
        messageSuccess.enableHtml(true);
        messageSuccess.setText(sb.toString());
        return messageSuccess;
    }

    private SendMessage handleAccessFindEntranceNeighborsCommand(String user, String telegramUserId) {
        if (!ownerService.isUserExist(telegramUserId)) {
            SendMessage messageSuccess = new SendMessage();
            messageSuccess.setText("⚠️У вас нет доступа к данному функционалу!");
            messageSuccess.setChatId(String.valueOf(telegramUserId));
            return messageSuccess;
        }
        SendMessage messageSuccess = new SendMessage();
        messageSuccess.setChatId(String.valueOf(telegramUserId));
        StringBuilder sb = new StringBuilder();
        Owner owner = ownerService.getUser(telegramUserId);
        List<Apartment> apartmentList = owner.getApartmentList();
        apartmentList.forEach(ownerApartment -> {
            sb.append("\nВаши соседи по квартире №").append(ownerApartment.getId()).append(" и стояку");
            List<Apartment> floorApartList = apartmentService.getFloorApartments(ownerApartment.getFloor(), ownerApartment.getEntrance());
            // индекс квартиры на этаже
            OptionalInt result = IntStream.range(0, floorApartList.size())
                    .filter(x -> ownerApartment.getId().equals(floorApartList.get(x).getId()))
                    .findFirst();
            if (result.isPresent()) {
                int index = result.getAsInt();
                apartmentService.getEntranceFloors(ownerApartment.getEntrance()).forEach(floor -> {
                    List<Apartment> floorApartmentList = apartmentService.getFloorApartments(floor, ownerApartment.getEntrance());
                    try {
                        Apartment apartment = floorApartmentList.get(index);
                        if (ownerApartment.getFloor() != floor)
                            sb.append("\nСосед(и) из квартиры №").append(apartment.getId()).append(" и этажа ").append(floor).append(apartment.getOwnerList().size() > 0 ? " зарегистрирован(ы) " +
                                    apartment.getOwnerList().stream().map(neighbor ->
                                            String.format("<a href=\"tg://user?id=%s\">%s</a>", neighbor.getTelegramId(), neighbor.getName())).collect(joining(", "))
                                    : " нет инф.");
                        else {
                            sb.append("\nВаша квартира №").append(ownerApartment.getId()).append(" на этаже ").append(ownerApartment.getFloor());
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        try {
                            sendDebugToSupport("запрошены соседи квартиры " + ownerApartment.getRoom() + " и этажу " + ownerApartment.getFloor() + ", но не найдена квартира по индексу" + index);
                        } catch (TelegramApiException ex) {
                            ex.printStackTrace();
                        }
                        e.printStackTrace();
                    }
                });
            } else {
                try {
                    sendDebugToSupport("запрошены соседи квартиры " + ownerApartment.getRoom() + " и этажу " + ownerApartment.getFloor() + ", но не найдены данные на этаже");
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        });
        messageSuccess.enableHtml(true);
        messageSuccess.setText(sb.toString());
        return messageSuccess;
    }

    private SendMessage handleAccessRemoveUserCommand(String userId, String telegramUserId) throws TelegramApiException {
        if (getRole(telegramUserId).equals(Permission.ADMIN) || getRole(telegramUserId).equals(Permission.OWNER)) {
            TempOwner tmpOwner = tempOwnerService.getUser(userId);
            tempOwnerService.delete(tmpOwner);
            sendInfoToUser(userId, "Администратор посчитал, что вы указали неверные данные, проверьте номер квартиры, он мог <a href=\"https://2119.ru/upload/iblock/f28/%D0%A0%D0%B5%D0%B7%D1%83%D0%BB%D1%8C%D1%82%D0%B0%D1%82%D1%8B%20%D0%BE%D0%B1%D0%BC%D0%B5%D1%80%D0%BE%D0%B2_4%20%D0%BE%D1%87%D0%B5%D1%80%D0%B5%D0%B4%D1%8C.pdf\">измениться</a>. Нужно указывать почтовый номер квартиры. Запросите доступ снова.", null);
            SendMessage messageSuccess = new SendMessage();
            messageSuccess.setText("Пользователь был удален, ему отправлено сообщение с просьбой повторить регистрацию");
            messageSuccess.setReplyMarkup(getDefaultKeyboard(userId));
            return messageSuccess;
        } else {
            SendMessage messageSuccess = new SendMessage();
            messageSuccess.setText("⚠️У вас нет доступа к данному функционалу!");
            messageSuccess.setChatId(String.valueOf(userId));
            return messageSuccess;
        }
    }

    private SendMessage handleAccessFindFloorNeighborsCommand(String user, String telegramUserId) {
        if (!ownerService.isUserExist(telegramUserId)) {
            SendMessage messageSuccess = new SendMessage();
            messageSuccess.setText("⚠️У вас нет доступа к данному функционалу!");
            messageSuccess.setChatId(String.valueOf(telegramUserId));
            return messageSuccess;
        }
        SendMessage messageSuccess = new SendMessage();
        messageSuccess.setChatId(String.valueOf(telegramUserId));
        StringBuilder sb = new StringBuilder();
        Owner owner = ownerService.getUser(telegramUserId);
        List<Apartment> apartmentList = owner.getApartmentList();
        apartmentList.forEach(ownerApartment -> {
            sb.append("\nВаши соседи по квартире №").append(ownerApartment.getId()).append(" и этажу ").append(ownerApartment.getFloor());
            // все квартиры в подъезде и на этаже
            List<Apartment> floorApartList = apartmentService.getFloorApartments(ownerApartment.getFloor(), ownerApartment.getEntrance());
            floorApartList.forEach(apartment -> {
                        if (ownerApartment.getId() != apartment.getId())
                            sb.append("\nСосед(и) из квартиры №").append(apartment.getId()).append(" ").append(apartment.getOwnerList().size() > 0 ? " зарегистрирован(ы) " +
                                    apartment.getOwnerList().stream().map(neighbor ->
                                            String.format("<a href=\"tg://user?id=%s\">%s</a>", neighbor.getTelegramId(), neighbor.getName())).collect(joining(","))
                                    : " еще не зарегистрирован(ы) через бота.");
                    }
            );
        });
        messageSuccess.enableHtml(true);
        messageSuccess.setText(sb.toString());
        return messageSuccess;
    }

    private InlineKeyboardMarkup getDefaultKeyboard(String telegramUserId) throws TelegramApiException {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();

        InlineKeyboardButton inlineKeyboardHomeButton = new InlineKeyboardButton();
        inlineKeyboardHomeButton.setText(START_LABEL);
        inlineKeyboardHomeButton.setCallbackData(COMMANDS.START.getCommand());

        InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
        inlineKeyboardButton.setText(INFO_LABEL);
        inlineKeyboardButton.setCallbackData(COMMANDS.INFO.getCommand());

        InlineKeyboardButton inlineKeyboardButtonAbout = new InlineKeyboardButton();
        inlineKeyboardButtonAbout.setText(ABOUT_BUILDING_LABEL);
        inlineKeyboardButtonAbout.setCallbackData(COMMANDS.ABOUT_BUILDING.getCommand());

        InlineKeyboardButton inlineKeyboardButtonAccess = new InlineKeyboardButton();
        inlineKeyboardButtonAccess.setText(ACCESS_LABEL);
        inlineKeyboardButtonAccess.setCallbackData(COMMANDS.ACCESS.getCommand());

        InlineKeyboardButton inlineKeyboardButtonSearch = new InlineKeyboardButton();
        inlineKeyboardButtonSearch.setText(FIND_LABEL);
        inlineKeyboardButtonSearch.setCallbackData(COMMANDS.FIND_NEIGHBORS.getCommand());

        InlineKeyboardButton inlineKeyboardButtonBot2119 = new InlineKeyboardButton();
        inlineKeyboardButtonBot2119.setText(ABOUT_2119_LABEL);
        inlineKeyboardButtonBot2119.setUrl("https://t.me/Info_2119_bot");


        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        keyboardButtonsRow1.add(inlineKeyboardHomeButton);
        keyboardButtonsRow1.add(inlineKeyboardButton);

        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonAbout);

        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow4 = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow5 = new ArrayList<>();

        keyboardButtonsRow5.add(inlineKeyboardButtonBot2119);

        keyboardButtons.add(keyboardButtonsRow1);
        keyboardButtons.add(keyboardButtonsRow2);
        keyboardButtons.add(keyboardButtonsRow3);
        keyboardButtons.add(keyboardButtonsRow4);
        keyboardButtons.add(keyboardButtonsRow5);

        // если не в списке владельцев и не забанен в приватном чате, то добавляем кнопку регистрации
        if (!ownerService.isUserExist(telegramUserId))
            if (getRole(telegramUserId).equals(Permission.BANNED)) {
                InlineKeyboardButton bannedHomeButton = new InlineKeyboardButton();
                bannedHomeButton.setText("Очень жаль, вы были забанены");
                bannedHomeButton.setCallbackData(COMMANDS.START.getCommand());
                keyboardButtonsRow3.add(bannedHomeButton);
            } else
                keyboardButtonsRow3.add(inlineKeyboardButtonAccess);
            // пользователь зарегистрирован
        else {
            keyboardButtonsRow4.add(inlineKeyboardButtonSearch);
        }

        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        return inlineKeyboardMarkup;
    }

    private Owner addOwnerToDb(TempOwner tmpOwner, String activatedBy) {
        Owner owner = new Owner();
        owner.setId(tmpOwner.getId());
        owner.setName(tmpOwner.getName());
        owner.setTelegramId(tmpOwner.getTelegramId());
        owner.setCarPlace(tmpOwner.getCarPlace());
        owner.setPhoneNum(tmpOwner.getPhoneNum());
        owner.setActivatedTelegramId(activatedBy);
        Apartment apartment = apartmentService.getApartment(tmpOwner.getRealNum());
        owner.getApartmentList().add(apartment);
        tempOwnerService.delete(tmpOwner);
        return ownerService.add(owner);
    }

    private Permission getRole(String telegramId) throws TelegramApiException {
        GetChatMember chatMember = new GetChatMember();
        chatMember.setChatId(privateChannelId);
        chatMember.setUserId(Long.parseLong(telegramId));
        ChatMember member = execute(chatMember);
        String status = member.getStatus();
        return switch (status) {
            case "creator" -> Permission.OWNER;
            case "administrator" -> Permission.ADMIN;
            case "kicked", "restricted" -> Permission.BANNED;
            default -> Permission.USER;
        };
    }

    /***
     * возвращает список админов чата, за исключением ботов-админов
     * @param chatId
     * @return
     * @throws TelegramApiException
     */
    private List<ChatMember> getChatAdministrators(String chatId) throws TelegramApiException {
        GetChatAdministrators getChatAdministrators = new GetChatAdministrators();
        getChatAdministrators.setChatId(chatId);
        List<ChatMember> admins = execute(getChatAdministrators);
        Predicate<ChatMember> isQualified = item -> item.getUser().getIsBot();
        admins.removeIf(isQualified);
        return admins;
    }
}
