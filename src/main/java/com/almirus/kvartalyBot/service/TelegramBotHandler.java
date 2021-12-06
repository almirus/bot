package com.almirus.kvartalyBot.service;

import com.almirus.kvartalyBot.dal.entity.Apartment;
import com.almirus.kvartalyBot.dal.entity.Owner;
import com.almirus.kvartalyBot.dal.entity.TempOwner;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.groupadministration.CreateChatInviteLink;
import org.telegram.telegrambots.meta.api.methods.groupadministration.UnbanChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatInviteLink;
import org.telegram.telegrambots.meta.api.objects.Update;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.joining;

@Component
@Data
@Slf4j
public class TelegramBotHandler extends TelegramLongPollingBot {

    private final String INFO_LABEL = "🤖 О боте";
    private final String ACCESS_LABEL = "🔐 Запросить доступ";
    private final String ABOUT_BUILDING_LABEL = "🏢 О доме";
    private final String ABOUT_ROOM_LABEL = "🏠 О квартире";
    private final String BOT_LABEL = "Чего нового";
    private final String START_LABEL = "🏡 В начало";
    private final String BEGIN_LABEL = "Начать";
    private final String SEND_TO_ADMIN_LABEL = "✅ Отправить данные";
    private final String SEND_TO_ADMIN_CANCEL_LABEL = "🚫 Отменить отправку";
    // todo весь текст перенести вверх в константы

    // временный владелец
    private final TempOwnerService tempOwnerService;
    // реальный владелец
    private final OwnerService ownerService;

    private final ApartmentService apartmentService;


    private enum COMMANDS {
        ADD("/add"),
        REMOVE("/remove"),
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
        CAR_NOT_EXIST("/car_not_exist");

        private String command;

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
        //!update.getMessage().getChatId().equals(Long.parseLong(privateChannelId)) - выключает бота в закрытом чате, но доступен в приватном чате с ботом
        if (update.hasMessage() && !update.getMessage().getChatId().equals(Long.parseLong(privateChannelId)) && (update.getMessage().hasText() || update.getMessage().hasContact())) { //текст, ответ боту, отправка контакта
            String text = update.getMessage().hasText() ? update.getMessage().getText() : "";
            String reply = update.getMessage().getReplyToMessage() != null ? update.getMessage().getReplyToMessage().getText() : "";
            String phone = update.getMessage().hasContact() ? update.getMessage().getContact().getPhoneNumber() : "";
            long telegramUserId = update.getMessage().getChatId();

            try {
                //todo Нужен рефакторинг!
                Pattern patternFloor = Pattern.compile("^этаж$");
                Pattern patternRoom = Pattern.compile("^квартира$");
                Pattern patternName = Pattern.compile("^имя$");
                Pattern patternPhone = Pattern.compile("^телефон$");
                Pattern patternCar = Pattern.compile("^номер машиноместа$");

                Matcher matcherFloor = patternFloor.matcher(reply);
                Matcher matcherRoom = patternRoom.matcher(reply);
                Matcher matcherName = patternName.matcher(reply);
                Matcher matcherPhone = patternPhone.matcher(reply);
                Matcher matcherCar = patternCar.matcher(reply);

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
                    SendMessage message = handleAccessPhoneCommand(String.valueOf(telegramUserId));
                    message.enableHtml(true);
                    message.setParseMode(ParseMode.HTML);
                    message.setChatId(String.valueOf(telegramUserId));
                    execute(message);
                } else if (matcherPhone.find()) {
                    sendPhoneInfo(phone, String.valueOf(telegramUserId));
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
                    sendInfoToSupport("Error " + e.getMessage());


                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        } else if (update.hasCallbackQuery() && !update.getCallbackQuery().getMessage().getChatId().equals(Long.parseLong(privateChannelId))) { //нажатие по кнопке
            try {
                /*  command - команда по кнопке
                    telegramId - текущий ID пользователя кто общается с ботом
                    forTelegramId - ID над кем производится действие
                 */
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
                    sendInfoToSupport("Error " + e.getMessage());
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private boolean isAdmin(String telegramUserId) {
        return telegramUserId.equals(supportChatId);
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
        tmpOwner.setPhoneNum("+" + phone);
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


    private void sendRequestToSupport(String message, String userId) throws TelegramApiException {
        InlineKeyboardButton inlineKeyboardButtonApprove = new InlineKeyboardButton();
        inlineKeyboardButtonApprove.setText("✅ Добавить");
        inlineKeyboardButtonApprove.setCallbackData(COMMANDS.ADD.getCommand() + "/" + userId);

        InlineKeyboardButton inlineKeyboardButtonCancel = new InlineKeyboardButton();
        inlineKeyboardButtonCancel.setText("🚫 Отказать пользователю");
        inlineKeyboardButtonCancel.setCallbackData(COMMANDS.DELETE.getCommand() + "/" + userId);

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();
        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonApprove);
        keyboardButtonsRow1.add(inlineKeyboardButtonCancel);

        keyboardButtons.add(keyboardButtonsRow1);

        inlineKeyboardMarkup.setKeyboard(keyboardButtons);

        SendMessage messageSupport = new SendMessage();
        messageSupport.setText(message);
        messageSupport.setChatId(supportChatId);
        messageSupport.enableHtml(true);
        messageSupport.setReplyMarkup(inlineKeyboardMarkup);
        execute(messageSupport);
    }

    private void sendInfoToUser(String telegramId, String message) throws TelegramApiException {
        SendMessage messageSupport = new SendMessage();
        messageSupport.setText(message);
        messageSupport.setChatId(telegramId);
        messageSupport.enableHtml(true);
        execute(messageSupport);
    }

    private void sendInfoToSupport(String message) throws TelegramApiException {
        sendInfoToUser(supportChatId, message);
    }

    private SendMessage getCommandResponse(String text, String user, String telegramUserId) throws TelegramApiException {

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
            return handleSendToAdminCommand(user, telegramUserId);
        }
        if (text.equals(COMMANDS.DELETE.getCommand())) {
            return handleDeleteDataCommand(user, telegramUserId);
        }
        if (text.equals(COMMANDS.CAR_NOT_EXIST.getCommand())) {
            sendCarPlaceInfo("Нет", String.valueOf(telegramUserId));
            return handleSuccessCommand(String.valueOf(telegramUserId));
        }
        if (text.equals(COMMANDS.CAR_EXIST.getCommand())) {
            return handleAccessCarCommand(telegramUserId);
        }
        if (text.equals(COMMANDS.ADD.getCommand())) {
            return handleAccessAddCommand(user, telegramUserId);
        }
        if (text.equals(COMMANDS.REMOVE.getCommand())) {
            return handleAccessCarCommand(telegramUserId);
        }
        return handleNotFoundCommand(telegramUserId);
    }

    private SendMessage handleNotFoundCommand(String telegramUserId) {
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
                            """,
                    user.getName(), user.getPhoneNum(), user.getPhoneNum());

            String apartStr = user.getApartmentList().stream().map(item ->
                    String.format("""
                                    Номер подъезда: <b>%s</b>
                                    Номер этажа: <b>%s</b>
                                    Номер квартиры: <b>%s</b>, по ДДУ: <b>%s</b>
                                    Комнат: <b>%s</b>
                                    Площадь по БТИ: <b>%s</b>, разница с договором: <b>%sм2</b>
                                    """,
                            item.getEntrance(), item.getFloor(), item.getId(), item.getDduNum(), item.getRoom(), item.getRealArea(), item.getDifference())
            ).collect(joining("\n"));

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
                Наш дом находится по адресу <a href="https://yandex.ru/maps/-/CCUujETt~B">2-й Грайвороновский пр-д. д44.к.2</a>
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

    private SendMessage handleBotCommand(String telegramUserId) {
        SendMessage message = new SendMessage();
        message.setText("""
                ✨ Бот умеет выдавать информацию о недвижимости
                ✨ Первая версия бота
                ✨ Привет человек
                """);
        message.setReplyMarkup(getDefaultKeyboard(telegramUserId));
        return message;
    }

    private SendMessage handleStartCommand(String telegramUserId) {
        SendMessage message = new SendMessage();
        message.setText("Доступные команды:");
        message.setReplyMarkup(getDefaultKeyboard(telegramUserId));
        return message;
    }

    private SendMessage handleInfoCommand() {
        SendMessage message = new SendMessage();
        message.setText("""
                После авторизации вы сможете найти своих соседей по этажу и стояку, а также другую информацию, получить доступ к закрытому чату.
                Функционал будет расширяться!
                Вопросы, пожелания, донаты 🥯 @<a href="tg://user?id=153968771">Almirus</a>
                """);
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

    private SendMessage handleAccessCommand(String telegramUserId) {
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
                Далее укажите пожалуйста номер <b>этажа</b>, <b>квартиры</b>, <b>ваше имя или никнейм</b>, опционально (но очень желательно <b>ваш номер телефон</b>).
                Эти данные нужны чтобы избавиться от спама, а также иметь возможность получить дополнительный функционал. Данные будут доступны только администратору.
                После проверки предоставленных сведений вас добавят в закрытый чат дома.
                """);
        message.enableHtml(true);
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

        return SendMessage.builder()
                .text("этаж")
                .chatId(telegramUserId)
                .replyMarkup(ForceReplyKeyboard.builder().forceReply(true).build())
                .build();

    }

    private SendMessage handleAccessRoomCommand(String telegramUserId) {

        return SendMessage.builder()
                .text("квартира")
                .chatId(telegramUserId)
                .replyMarkup(ForceReplyKeyboard.builder().forceReply(true).build())
                .build();

    }

    private SendMessage handleAccessNameCommand(String telegramUserId) {

        return SendMessage.builder()
                .text("имя")
                .chatId(telegramUserId)
                .replyMarkup(ForceReplyKeyboard.builder().forceReply(true).build())
                .build();

    }

    private SendMessage handleAccessPhoneCommand(String telegramUserId) {

        KeyboardButton keyboardButton = new KeyboardButton("Номер телефона");
        keyboardButton.setRequestContact(true);
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardButtons = new KeyboardRow();
        keyboardButtons.add(keyboardButton);
        keyboard.add(keyboardButtons);
        replyKeyboardMarkup.setKeyboard(keyboard);
        replyKeyboardMarkup.setResizeKeyboard(false);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        return SendMessage.builder()
                .text("телефон")
                .chatId(telegramUserId)
                .replyMarkup(replyKeyboardMarkup)
                .build();

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

        return SendMessage.builder()
                .text("покупали место на парковке")
                .chatId(telegramUserId)
                .replyMarkup(inlineKeyboardMarkup)
                .build();

    }

    private SendMessage handleAccessCarCommand(String telegramUserId) {

        return SendMessage.builder()
                .text("номер машиноместа")
                .chatId(telegramUserId)
                .replyMarkup(ForceReplyKeyboard.builder().forceReply(true).build())
                .build();

    }

    private SendMessage handleAccessAddCommand(String userId, String telegramUserId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(userId);
        if (isAdmin(telegramUserId)) {
            try {
                UnbanChatMember unbanChatMember = new UnbanChatMember();
                unbanChatMember.setChatId(privateChannelId);
                unbanChatMember.setOnlyIfBanned(true);
                unbanChatMember.setUserId(Long.valueOf(userId));
                execute(unbanChatMember);
            } catch (TelegramApiException e) {
                try {
                    sendInfoToSupport("Ошибка при удалении пользователя из бана: " + e.getMessage());
                } catch (TelegramApiException ex) {
                    ex.printStackTrace();
                }
            }
            sendInfoToUser(userId, "Вам выдан полный доступ, нажмите здесь: " + getChatInviteLink());
            message.setText(String.format("Выдан полный доступ для <a href=\"tg://user?id=%s\">пользователя</a>", userId));
            TempOwner tempOwner = tempOwnerService.getUser(userId);
            addOwnerToDb(tempOwner, telegramUserId);
        } else {
            message.setText("⚠️У вас нет прав доступа к этому функционалу!");
        }
        return message;
    }

    private SendMessage handleSendToAdminCommand(String userId, String telegramUserId) throws TelegramApiException {
        TempOwner tmpOwner = tempOwnerService.getUser(telegramUserId);
        Apartment apartment = apartmentService.getApartment(tmpOwner.getRealNum());
        String owners = apartment.getOwnerList().size() > 0 ? apartment.getOwnerList().stream().map(item ->
                String.format("""
                                <a href="tg://user?id=%s">%s</a>
                                """,
                        item.getTelegramId(), item.getName())
        ).collect(joining(", ")) : "Пока нет";
        SendMessage messageSuccess = new SendMessage();
        String status;
        // TODO здесь ветка автоапрува пользователя, если совпали его данные из базы квартиры
        if (tmpOwner.getFloor().equals(apartment.getFloor()) && tmpOwner.getRealNum().equals(apartment.getId())) {
            messageSuccess.setText("🎉 Добро пожаловать в чат!");
            status = "Пользователь получил полный доступ автоматически";
            sendInfoToUser(userId, "Вам выдан полный доступ, нажмите здесь: " + getChatInviteLink());
            // активирует запись бот, записываем его ID
            addOwnerToDb(tmpOwner, token.substring(0, token.indexOf(":")));
        } else {
            messageSuccess.setText("🎉 Ваши данные получены. Идет проверка...");
            status = "Пользователь запросил полный доступ";
        }
        messageSuccess.setChatId(String.valueOf(telegramUserId));
        messageSuccess.setReplyMarkup(getDefaultKeyboard(telegramUserId));
        sendRequestToSupport(String.format("""
                        %s
                        Telegram аккаунт: <a href="tg://user?id=%s">%s</a>
                        Были введены данные: Этаж: %s Квартира: %s Телефон: %s Машиноместо: %s
                        --------------------             
                        В нашей базе по этой квартире: Этаж: %s Квартира: %s, Номер квартиры по ДДУ: %s
                        Другие владельцы: %s
                        """, status,
                userId, tmpOwner.getName(), tmpOwner.getFloor(), tmpOwner.getRealNum(), tmpOwner.getPhoneNum(), tmpOwner.getCarPlace(),
                apartment.getFloor(), apartment.getId(), apartment.getDduNum(),
                owners), userId);
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
        message.setText("Почти все готово");
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

    private InlineKeyboardMarkup getDefaultKeyboard(String telegramUserId) {
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

        List<List<InlineKeyboardButton>> keyboardButtons = new ArrayList<>();

        List<InlineKeyboardButton> keyboardButtonsRow1 = new ArrayList<>();

        keyboardButtonsRow1.add(inlineKeyboardHomeButton);
        keyboardButtonsRow1.add(inlineKeyboardButton);

        List<InlineKeyboardButton> keyboardButtonsRow2 = new ArrayList<>();
        keyboardButtonsRow1.add(inlineKeyboardButtonAbout);

        List<InlineKeyboardButton> keyboardButtonsRow3 = new ArrayList<>();
        if (!ownerService.isUserExist(telegramUserId)) keyboardButtonsRow3.add(inlineKeyboardButtonAccess);

        keyboardButtons.add(keyboardButtonsRow1);
        keyboardButtons.add(keyboardButtonsRow2);
        keyboardButtons.add(keyboardButtonsRow3);

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

}
