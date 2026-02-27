package dev.sample.test;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Lombok 테스트용 모델 클래스
 */
@Getter
@Setter
@ToString
@Builder
public class User {

    private Long id;

    private String name;

    private int age;
}
