package pro.sky.telegrambot.service.impl;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class NotificationTaskServiceImpl implements NotificationTaskService {

    private Logger logger = LoggerFactory.getLogger(NotificationTaskServiceImpl.class);

    private final NotificationTaskRepository notificationTaskRepository;
    private final TelegramBot telegramBot;

    public NotificationTaskServiceImpl(NotificationTaskRepository notificationTaskRepository, TelegramBot telegramBot) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.telegramBot = telegramBot;
    }

    /**
     * Метод для проверки введённого текста на соответствие паттерну
     **/
    public boolean taskPattern(String inputText, Long chat_id) {
        //Выбранный паттерн в формате "dd.MM.yyyy HH:mm текст_напоминания"
        Pattern pattern = Pattern.compile("([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");

        //Проверка на соответствие паттерну и выделение вычленение из него необходимых частей
        //с последующим созданием сущности и сохранением её в БД (в случае успеха)
        Matcher matcher = pattern.matcher(inputText);
        if (matcher.matches()) {
            String date = matcher.group(1);
            String item = matcher.group(3);
            LocalDateTime localDateTime = LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
            NotificationTask notificationTask = new NotificationTask(chat_id, localDateTime, item);
            if (notificationTask != null && notificationTask.getTimestamp().isAfter(LocalDateTime.now())) {
                logger.debug("Записываем задачу под номером " + notificationTask.getId());
                notificationTaskRepository.save(notificationTask);
                return true;
            } else {
                telegramBot.execute(new SendMessage(notificationTask.getChat_id(), "Указанная дата уже прошла. К сожалению, пока что отсутствует возможность отправить сообщение в прошлое"));
                return false;
            }
        } else {
            logger.info("Направленный текст не соответствует паттерну");
            return false;
        }
    }

    /**
     * Метод для отправки уведомлений по расписанию из собственной БД
     **/
    @Scheduled(cron = "0 0/1 * * * *")
    public void notificationEveryMinute() {
        //Создаём переменную с текущей датой и временем
        LocalDateTime currentDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        //Создаём список из сущностей в БД, чей timestamp соответствует переменной с текущей датой и временем
        List<NotificationTask> notificationTasks = notificationTaskRepository.findAll().stream()
                .filter(notificationTask -> notificationTask.getTimestamp().equals(currentDateTime))
                .collect(Collectors.toList());

        //Отправляем напоминания и удаляем из БД данные записи
        notificationTasks
                .stream().peek(notificationTask -> telegramBot.execute(new SendMessage(notificationTask.getChat_id(),
                        "Напоминание(" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + "): " +
                                notificationTask.getNotification())))
                .forEach(notificationTaskRepository::delete);

    }

    /**
     * Метод для очистки базы, в случае если сервер не работал в нужное для отправки уведомления время.
     **/
    @PostConstruct
    private void WeeklyCleaningBase() {
        //Создаём переменную с текущей датой и временем
        LocalDateTime currentDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);

        //Создаём список из сущностей в БД, чей timestamp уже просрочен по отношению к переменной с текущей датой и временем
        List<NotificationTask> notificationTasks = notificationTaskRepository.findAll().stream()
                .filter(notificationTask -> notificationTask.getTimestamp().isBefore(currentDateTime))
                .collect(Collectors.toList());

        //Отправляем напоминания и удаляем из БД данные записи
        notificationTasks
                .stream().peek(notificationTask -> telegramBot.execute(new SendMessage(notificationTask.getChat_id(),
                        "Напоминание(" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) + "): " +
                                notificationTask.getNotification() + "\nВ связи с возникшим перебоем в работе сервера направляем Вам уведомление. \nПусть с задержкой, но с заботой о Вас.")))
                .forEach(notificationTaskRepository::delete);

    }
}
