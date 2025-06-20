# camel-examples

Project consisting of two components Service A and Service B to explore Apache Camel capabilities.

## Service A

Componente: /01.projects/service-a

Expose an endpoint type GET /camel/user/{name} and consumes two network resources, processes the two requests and returns the result in another custom object:

* https://api.agify.io?name=$simple{headers.name}
* https://api.nationalize.io?name=$simple{headers.name}

### Flow

The router RestConsumerRouter has an endpoint **direct:processRequest** consists of consuming two resources given a name, processes and stores them in a property and then assembles a custom object and returns the result. It also sends an audit log in an ActiveMQ queue.

[!Service A flow](/image/ServiceADiagram.png)

## Service B

The router AuditLogRepository has an endpoint **activemq:audit-logging-channel** receives a message from an ActiveMQ queue, transforms the message into an Entity and saves it in a MySQL database.

[!Service A flow](/image/ServiceBDiagram.png)

## Resources

### Docker

Directory: /02.docker-files

It contains a docker-compose.yaml file to raise the Kafka service and MySQL to be used in the project locally.

To raise the services run the command:

```bash
docker compose up
```

To access the database you can execute the following:

```bash
docker exec -it mysql_db mysql -u root -p
```

> **Warning** Be sure to change the database properties of the /02.docker-files/docker-compose.yaml file according to your preference..

To raise the ActiveMQ services run the command:

``` bash
docker run -p 61616:61616 -p 8161:8161 rmohr/activemq
```

To access the administration console from the browser go to **http://localhost:8161/admin/**.
