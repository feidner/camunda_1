package hfe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
public class Starter {



    public Starter() {

    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Starter.class);
    }

}
