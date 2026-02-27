package dev.sample.test;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

@WebServlet("/test/lombok")
@Slf4j // LogTestServlet.java L18 코드를 대체하여 로깅을 작성할 수 있는 Lombok API
public class LombokTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = resp.getWriter();


        // Builder 테스트
        User user = User.builder()
                .id(1L)
                .name("YOO")
                .age(35)
                .build();


        // Getter 테스트
        String name = user.getName();

        // Setter 테스트
        user.setAge(25);


        log.info("user={}", user);

        out.println("Lombok OK");
        out.println("name=" + name);
        out.println("age=" + user.getAge());
    }
}
