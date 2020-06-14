package services
import dao.DailyDAO
import javax.inject.Inject
import play.twirl.api.Html

import scala.concurrent.ExecutionContext

// TODO пользователю - как только разрешили доступ к тесту
// TODO авторам тестов: о прохождении их тестов, об ошибках и попытках взлома тестов (молое время ответа, др.)

class SnegirMail @Inject()(dailyDAO: DailyDAO)(implicit ec: ExecutionContext) {

//  def adminsDailySend() = for {
//    (unicUsers, allEntries, blocked, registered, createdBases, createdGroups) <- statisticsForLastDay()
//    admins <- dailyDAO.listOfAllAdmins
//    (userId, userName, email) <- admins
//    dailyLogins <- dailyDAO.userEntriesLastDay(userId)
//    body = views.html.services.mail.dailyToAdmin(userName, registered, blocked, createdBases, unicUsers, allEntries, createdGroups, dailyLogins)
//  } yield send(email, body)


  def statisticsForLastDay() = for {
    (unicUsers, allEntries) <- dailyDAO.visitorsLD
    (blocked, registered) <- dailyDAO.registrationsLD
    createdBases <- dailyDAO.createdDatabasesLD
    createdGroups <- dailyDAO.groupsCreatedLD
  } yield (unicUsers, allEntries, blocked, registered, createdBases, createdGroups)


  def send(email: String, body: Html): Boolean = {
    val headers = ("Content-type: text/plain; charset=UTF-8",
      "From: Система рассылки Снегирь<snegir@lastochka-os.ru>",
      "X-Mailer: PHP/")
    val topic = "Ежедневная рассылка для администраторов сайта Снегирь"
    false
  }



}