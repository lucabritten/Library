package de.htwsaar.library;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(de.htwsaar.library.TestcontainerConfig.class)
@ActiveProfiles("test")
class ExerciseApplicationTests {

    @Test
    void contextLoads() {
    }

}
