# SearchEngineOne

## _Поисковый движок представляет из себя Spring-приложение (JAR-файл, запускаемый на любом сервере или компьютере), работающее с локально установленной базой данных MySQL,  для получения результатов поисковой выдачи по запросу._
Frontend-составляющая представляет собой веб-страницу, которая отображается в браузере при входе по адресу, установленному в конфигурационном файле.

На этой веб-странице находятся три вкладки:

### Dashboard.
Эта вкладка открывается по умолчанию. На ней отображается общая статистика по всем сайтам, а также детальная статистика и статус по каждому из сайтов.

### Management.
На этой вкладке находятся инструменты управления поисковым движком — запуск и остановка полной индексации (переиндексации), а также возможность добавить (обновить) отдельную страницу по ссылке.

### Search.
Эта страница предназначена для получения результатов запросов. На ней находится поле поиска, выпадающий список с выбором сайта для поиска, а при нажатии на кнопку «Найти» выводятся результаты поиска.

Реализованы команды API для управления индексацией, а также получения статистической информации и составления запросов и получения информации по итогам обработки запросов.

### Настройка

_Перед первым запуском приложения убедитесь в работающем сервере MySQL, наличия в нем пустой базы search_engine, заполненных полей username и password. При индексации большого количества страниц рекомендуется установить парметр запуска JVM "-Xss2048k"._

Конфигурационный файл приложения в формате YAML (application.yaml) находится в корне проекта. В YAML-файле содержатся:

Данные доступа к локальной базе данных MySQL: хост, логин, пароль, имя базы. Например:

spring.datasource.url: jdbc:mysql://localhost:3306/search_engine

spring.datasource.username: ХХХХХ

spring.datasource.password: ХХХХХ

Перечень сайтов, которые необходимо индексировать. Например:

	sites:
	   - url: https://www.lenta.ru
             name: Лента.ру
           - url: https://www.skillbox.ru
             name: Skillbox

Имя User-Agent, который необходимо подставлять при запросах страниц сайтов. Например:

userAgent: "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6"

Путь к веб-интерфейсу (по умолчанию — /admin).
