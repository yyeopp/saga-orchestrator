# SAGA-Orchestrator with KafkaQueue & Sync process

---

## Goal

효율적이고 지속 가능한 **MSA 애플리케이션** 개발을 위한 **SAGA 오케스트레이터 프레임워크**

---

## System Outline

![](./image/System_Concept.png)

동호회 플랫폼을 예시로 함

해당 프로젝트에서 구현된 부분은 **Club Orchestrator**

### Layer Concept

4계층으로 구성

- Frontend - **Service Layer** - **Domain Layer** - DB

- 기존 Monolithic MVC의 서비스 계층을 두 개로 분리하고, 기능 간의 경계를 보다 명확히 함

#### Service Layer

Controller 가 위치함으로써 Frontend와의 통신을 전담

**KafkaQueue**를 사용하여 **이벤트의 순차적 송수신과 트랜잭션 일관성을 유지**함

**오케스트레이터** 파드가 위치

#### Domain Layer

Kafka 이벤트에 의해 작동하는 **무상태 API**로 고유의 Persistence를 관리

도메인 **모듈** 파드가 위치

### SAGA-Orchestrator Framework

Kafka 이벤트 송수신 과정에 대한 **순차 처리**, **보상트랜잭션 처리 로직** 같이

**아키텍처** 수준에서 중요하면서도 **모든 오케스트레이터가 공유하는** 기능에 대해 공통화 처리를 적용

- `OrchestrationBase` 와 `OrchestrationManager`

- 아키텍처 이해도가 낮은 개발자도 두 클래스에 의존하면 MSA 코드 작성이 가능하며, 실제 비즈니스 로직에 보다 집중할 수 있음

---

## Development Guide

해당 프레임워크를 이용한 MSA Application 개발 과정을 간단하게 제시해본다.

#### 1. 기능에 대한 Domain R&R 부여

Client 에게 표시되는 화면이나 Domain 밀접성을 기준으로, 각 기능은 R&R 이 부여된다.

- ex) 클럽 가입 신청 기능은 Club Domain 에 해당할 것

- R&R이 애매한 신규 기능은 관련 담당자들 간 협의를 거쳐 하나로 확정하고,

- 기능의 고유성이 높을 시 독자 Domain 으로 분리한다.

#### 2. 각 Domain에 대해 필요한 REST API 도출

기능을 구현하기 위해 여러 Domain 의 역할이 필요할 수 있다.

- ex) 클럽 가입 신청 시, Club -> Payment -> Ext-Payment -> Mail 순으로 서로 다른 모듈이 모두 동작해야 한다.

각 Domain 에게 필요로 하는 REST API 를 도출하여, 각 Domain의 담당자에게 개발을 요청한다.

#### 3. Domain 개발

Domain 담당자는 로컬 트랜잭션을 관리하는 REST API를 개발하고, 해당 API 에 Kafka topic 을 매칭하여 Listen 한다.

단, 이 때 담당자는 **동일한 DTO**로 동작하는 **보상거래 API**를 함께 개발하여야 한다.

#### 4. Orchestrator 개발

Orchestrator 에서는 각 Domain 으로 향하는 Kafka event의 순차처리와 보상트랜잭션을 관리한다.

Service method 에서 Kafka event 를 **순서대로 발송하는 코드**를 작성하고, 발송된 event의 result **토픽을 listen 하는 listener**를 정의한다.

- ex) /club/join 으로 동작하는 join 메서드에서는, 아래 토픽으로 kafka 이벤트를 발송한다.

- shc.club.join -> shc.pay -> shc.ext.payment -> shc.sms 

- 이벤트를 발송한 뒤 응답을 기다릴 수 있고, 기다리지 않을 수도 있다.

모듈에서 실패 응답이 돌아왔을 때의 보상트랜잭션 처리는, **오케스트레이터가 자동으로 진행해준다.**

---

## Implementation Detail

### Sequential & Synchronized Orchestration

![](./image/Sequential&Synchronized_Orchestration.png)

#### 1. 클라이언트 최초 요청 시

오케스트레이터는 클라이언트 요청이 최초 진입할 시 GID를 생성한다.
그리고 해당 GID 에 매핑되는 두 가지 데이터를 메모리에 적재한다.

- 해당 GID 가 발송하는 Kafka event 의 Topic 과 그에 대한 BlockingQueue 를 관리하는 HashMap
- 해당 GID 가 발송하는 Kafka event 에서 사용했던 DTO 를 임시로 저장하고 관리하는 HashMap

#### 2. Kafka 이벤트 발송 메서드의 2가지 종류

Service 메서드에서는 각 모듈로 송신하는 Kafka 이벤트를 순차적으로 입력한다.
각 이벤트 발송 시, 

- 모듈로부터 응답 (*.result Topic) 이 돌아오는 것을 기다렸다가 진행할 지
- 모듈의 응답을 기다리지 않을 지를 선택할 수 있다.

#### 3. Kafka 이벤트 송수신 전 과정

모듈로부터의 응답을 기다리고자 하는 경우 다음과 같은 순서로 Kakfa 이벤트 송수신이 일어난다.

- 오케스트레이터는 A 모듈로 Topic = Test, Key = REQUEST 이벤트를 송신한다.
  그리고 해당 GID, Topic 에 해당하는 BlockingQueue 를 생성하고,
  BlockingQueue 로 결과값이 수신될 때까지 쓰레드를 정지시킨다.

- A 모듈이 그에 대한 응답으로
  Topic = Test.result, Key = SUCCESS 혹은
  Topic = Test.result, Key = FAIL  이벤트를 송신한다.

- 오케스트레이터의 Listener 클래스는 Topic=Test.result 를 수신받으면,
  해당 GID, Topic 에 해당하는 BlockingQueue 로 응답 결과를 넣는다.

- 이에 따라 BlockingQueue 로 정지시킨 쓰레드가 구동된다.

모듈로부터의 응답을 기다리지 않는 경우, 오케스트레이터는 Kafka 이벤트만 송신할 뿐 별도의 BlockingQueue 처리를 하지 않는다.

#### 4. 클라이언트 요청 반환 및 메모리 반환

모든 과정이 끝나고 클라이언트로 응답을 보낼 때, 해당 GID 와 그에 매핑되는 메모리 내 데이터를 삭제처리한다.

### Compensating Transaction for SAGA

![](./image/Compensating_Transaction_for_SAGA.png)

#### DTO 메모리의 역할

Service 메서드는 보상트랜잭션을 구현하기 위해, 각 모듈로 향하는 이벤트의 DTO를 GID, Topic 에 매핑하여 메모리에 적재하게 된다. 

#### 보상트랜잭션 발생 과정

- 오케스트레이터가 A 모듈로 Topic = Test, Key = REQUEST 이벤트를 송신한 후 응답이 돌아왔을 때,

- Test.result 의 Key 가 FAIL 인 경우 KafkaFailException 을 유발한다.

- KafkaFailException이 발생한 오케스트레이터의 Service 메서드는 이후의 처리를 진행하지 못하고, 보상트랜잭션 로직으로 진입한다.

- KafkaFailException 발생 시 유발되는 보상트랜잭션 로직에서는, 
  메모리에서 해당 GID 에 매핑되는 Topic 과 DTO 를 찾아서,
  Key = FAIL 로 설정한 카프카 이벤트를 각 모듈로 송신한다.

- 이를 통해 각 모듈에서 기존에 처리된 내용들이 롤백되고,

- GID 삭제 등 공통 로직이 동작하면서 메서드가 종료한다.

---

## Effectiveness

#### MSA 애플리케이션 운영에 대한 안정성 향상

아키텍처 이해도가 낮은 개발자여도, SAGA 패턴에 대한 안정적인 개발이 가능함

#### 신규 기능 개발 시의 생산성 향상

SAGA 구현과 같은 아키텍처 레벨의 고민을 최소화한 가운데 비즈니스 로직에 집중하도록 함으로써,

신규 기능을 빠르게 개발하는 데 도움이 됨

---

## P.S.

개발 기간 : 2023.11.24 ~ 2023.11.26

- BlockingQueue 를 활용한 Sync Process 아이디어는 레퍼런스가 따로 존재함을 알림

추후 개선 과제

- 서비스 클래스 내의 `Orchestration` 관련 코드 완전 배제

- `Listener` 클래스 개선

- Kafka Topic 네이밍 룰을 강제하기 위한 시스템 마련

- `Orchestration` 관련 코드 안정성 확보 (예외처리 하나도 안됨)

- `Orchestration` 관련 코드 Freezing 및 library 화


