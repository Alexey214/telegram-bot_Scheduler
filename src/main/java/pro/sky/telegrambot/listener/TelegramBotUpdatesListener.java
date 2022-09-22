package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    @Autowired
    private TelegramBot telegramBot;
    @Autowired
    private NotificationTaskService notificationTaskService;

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
            long chat_id = update.message().chat().id();
            String inputText = update.message().text();
            boolean aBoolean = notificationTaskService.taskPattern(inputText, chat_id);
            if ("/start".equals(inputText)) {
                telegramBot.execute(new SendMessage(chat_id, "Hello! \uD83D\uDE09"));
            } else if (Boolean.toString(aBoolean).equals("true")) {
                telegramBot.execute(new SendMessage(chat_id, "Ваша задача успешно записана. \n" +
                        "В назначенное время вы получите уведомление"));
            } else {
                telegramBot.execute(new SendMessage(chat_id, "Некорректный запрос. Попробуйте ещё раз.\n" +
                        "Введите доступную команду:\n" +
                        "1) /start - вывод приветственного сообщения\n" +
                        "2) Записать задачу можно командой под маской: dd.MM.yyyy HH:mm \"Наименование задачи\""));
            }


        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

}
