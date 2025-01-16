# java8-ast

* Annotation-processor 를 이용해 AST 를 수정하는 예제

## 목적

* query-dsl 의 @QueryProjection 을 사용중인 프로젝트에서 가변 칼럼을 조회하기 위함
* 가변 칼럼은 배열이므로 인자가 마지막에 추가 되어야 함
* 이전에 사용중이거나 앞으로 수정 될 여지가 있는 @QueryProjection 가 선언 된 클래스는 변경하지 않음
* 클래스를 확장하고 상위 클래스의 생성자가 변경됐을 때 중복 코드 수정이 없는 방법으로 고려

## 특이사항

* Java8 버전 기준으로 system dependency tools.jar 추가
  * 이후 버전에선 의존성 변경이 필요함

## 실행방법

* annotation-processor 모듈 install
* application 모듈 mvn compile
  * Intellij 기준 cmd+F9 Build 는 오류 발생
  * Gradle 환경에선 정상 동작 하는 것으로 보임?
* StaticVO 의 생성자 기준으로 target/classes/cyh/ast/vo/DynamicVO.class 를 확인