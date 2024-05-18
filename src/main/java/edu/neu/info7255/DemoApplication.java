package edu.neu.info7255;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import edu.neu.info7255.service.IndexingListener;


//Starting point of the Spring Boot Application
@SpringBootApplication
public class DemoApplication {

	 public static final String topicExchangeName = "spring-boot-exchange";

	    public static final String queueName = "indexing-queue";

	    @Bean
	    Queue queue() {
	        return new Queue(queueName, false);
	    }

	    @Bean
	    TopicExchange exchange() {
	        return new TopicExchange(topicExchangeName);
	    }

	    @Bean
	    Binding binding(Queue queue, TopicExchange exchange) {
	        return BindingBuilder.bind(queue).to(exchange).with(queueName);
	    }

	    @Bean
	    MessageListenerAdapter listenerAdapter(IndexingListener receiver) {
	        return new MessageListenerAdapter(receiver, "receiveMessage");
	    }


	    @Bean
	    SimpleMessageListenerContainer container(ConnectionFactory connectionFactory,
	                                             MessageListenerAdapter listenerAdapter) {
	        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
	        container.setConnectionFactory(connectionFactory);
	        container.setQueueNames(queueName);
	        container.setMessageListener(listenerAdapter);
	        return container;
	    }

	    
	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
