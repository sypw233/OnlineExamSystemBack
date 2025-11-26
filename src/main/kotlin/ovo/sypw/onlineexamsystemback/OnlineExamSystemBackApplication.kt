package ovo.sypw.onlineexamsystemback

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class OnlineExamSystemBackApplication

fun main(args: Array<String>) {
    runApplication<OnlineExamSystemBackApplication>(*args)
}
