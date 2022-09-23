package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final TelegramBot telegramBot;
    private final NotificationTaskService notificationTaskService;

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskService notificationTaskService) {
        this.telegramBot = telegramBot;
        this.notificationTaskService = notificationTaskService;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            if (update.message() == null) {
                return;
            }
            if ("/start".equals(update.message().text())) {
                telegramBot.execute(new SendMessage(update.message().chat().id(), "Hello! \uD83D\uDE09"));
            }
            if (!"/start".equals(update.message().text())) {
                long chatId = update.message().chat().id();
                String inputText = update.message().text();
                boolean aBoolean = notificationTaskService.taskPattern(inputText, chatId);
                if (aBoolean) {
                    telegramBot.execute(new SendMessage(chatId, "Ваша задача успешно записана. \n" +
                            "В назначенное время вы получите уведомление"));
                } else {
                    telegramBot.execute(new SendMessage(chatId, "Некорректный запрос. Попробуйте ещё раз.\n" +
                            "Введите доступную команду:\n" +
                            "1) /start - вывод приветственного сообщения\n" +
                            "2) Записать задачу можно командой под маской: dd.MM.yyyy HH:mm \"Наименование задачи\""));
                }
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
