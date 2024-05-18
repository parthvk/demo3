<h1><centre>Project Health Plan Pro</h1></centre>
<h2>About</h2>
This project develops a robust health insurance plan management system designed to streamline operations and enhance security across the digital landscape of healthcare services. Key features and technologies include:
<ul>
<li>Efficient Data Handling: Utilizes RabbitMQ for robust message queuing and Redis for high-speed data caching, ensuring rapid response times and scalability.</li>
<li>Advanced Security: Implements OAuth2 with RS256 for secure authentication and token validation, greatly enhancing system security and protecting sensitive health data.</li>
<li>Operational Insights: Employs Kibana for real-time visualization and monitoring of Elasticsearch data, providing deep operational insights and aiding in proactive management.</li>
<li>Sophisticated API Features: Features an API with advanced RESTful semantics, including conditional read and write operations, which optimize network and storage resources while ensuring data consistency and reliability.</li>
<li>This platform is tailored to meet the complex demands of health insurance providers, offering them a scalable solution to manage, analyze, and secure large volumes of sensitive data effectively.</li>
</ul>

<br><br>

<h2>🌟 Technologies To Be Used</h2>
<ul>
 <li>Reddis</li>
 <li>Elastic Search</li>
 <li>Kibana</li>
 <li>RabbitMQ</li>
 <li>OAuth2 with RS256</li>   
 <li>Docker</li>
 <li>Google Cloud Platform for token</li>
 <li>Postman for hitting REST APIs</li>
</ul>
<br><br>

## Requirements


- Git
- CLI (only for write access)


## Redis Installations

```bash
redis-server
brew services start redis
brew services info redis
brew services stop redis
redis-cli
```

## RabbitMQ

Ref: https://dev.to/pharzad/introduction-to-rabbitmq-for-node-js-developers-1clm

## Docker

To delete all containers including its volumes use,

```bash
docker rm -vf $(docker ps -aq)
```

To delete all the images,

```bash
docker rmi -f $(docker images -aq)
```

To start build

```bash
docker-compose up
```

## Status

Docker status

```bash
http://localhost:9200/
```

Elasticsearch console

```bash
http://localhost:5601/app/dev_tools#/console
```