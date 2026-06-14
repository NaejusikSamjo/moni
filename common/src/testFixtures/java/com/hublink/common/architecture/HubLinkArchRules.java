package com.hublink.common.architecture;

import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

public class HubLinkArchRules {
    // 1. Controller → Service → Repository 방향으로만 호출이 가능하도록 통제
    public static final ArchRule LAYER_RULE = layeredArchitecture()
            .consideringAllDependencies()
            // 계층 정의
            .layer("Controller").definedBy("..controller..", "..presentation..")
            .layer("Service").definedBy("..service..", "..application..")
            .layer("Repository").definedBy("..repository..")
            .layer("Client").definedBy("..client..")
            .layer("Message").definedBy("..message..", "..stream..", "..publisher..", "..consumer..", "..event..", "..prompt..", "..parser..")
            .layer("Entity").definedBy("..entity..", "..domain.entity..")
            .layer("Common").definedBy("com.msa.core_common..")
            .layer("DTO").definedBy("..dto..")
            .layer("Global").definedBy("..global..", "..fixture..")
            .layer("Infrastructure").definedBy("..infrastructure..")

            // 규칙 정의
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service", "Message")
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Service")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Infrastructure")
            .whereLayer("Client").mayOnlyBeAccessedByLayers("Service", "Infrastructure", "Global")
            .whereLayer("Message").mayOnlyBeAccessedByLayers("Service", "Controller")
            .whereLayer("Entity").mayOnlyBeAccessedByLayers("Service", "Repository", "Controller", "DTO", "Global", "Message", "Infrastructure")
            .whereLayer("Global").mayOnlyBeAccessedByLayers("Service", "Controller", "Entity", "Client", "Infrastructure", "DTO")
            .whereLayer("DTO").mayOnlyBeAccessedByLayers("Controller", "Service", "Client", "Message", "Infrastructure", "Global")
            .withOptionalLayers(true);

    // 2. Entity 안에 Controller, Service, Client 관련 코드가 섞이지 않게 차단
    public static final ArchRule ENTITY_ISOLATION_RULE = classes()
            .that().resideInAPackage("..entity..")
            .should().onlyDependOnClassesThat()
            .resideOutsideOfPackages("..controller..", "..service..", "..client..", "..message..");

    // 3. controller 패키지 안에 클래스를 만들면, 무조건 이름 끝이 ~Controller로 끝나야 하고, @RestController 어노테이션을 빼먹지 않고 붙이도록 시스템이 강제
    public static final ArchRule CONTROLLER_CONVENTION_RULE = classes()
            .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().haveSimpleNameEndingWith("Controller")
            .andShould().resideInAnyPackage("..controller..", "..presentation..");

    // 4. DTO 안에 Service나 Repository 클래스를 넣지 못하게 차단
    public static final ArchRule DTO_ISOLATION_RULE = classes()
            .that().resideInAPackage("..dto..")
            .should().onlyDependOnClassesThat()
            .resideOutsideOfPackages("..controller..", "..service..", "..repository..", "..client..");

    // 5. System.out.println 사용 금지
    public static final ArchRule NO_STANDARD_STREAMS_RULE = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
            .because("System.out.println 대신 Slf4j Logger를 사용해야 합니다.");
}
