1️⃣ 옵션 A: "Kafka! 비동기 처리가 필요한 순간" (추천 🔥)
상황: 지금은 결제 후 영수증 저장, 이메일 발송(가정) 등을 다 기다려야 응답이 나갑니다.

목표: "결제는 성공했어! 나머지는 뒤에서 알아서 할게." (Event Driven Architecture)

기술: Apache Kafka 연동, Producer/Consumer 구현.

난이도: 상 (하지만 백엔드 개발자의 꽃)

2️⃣ 옵션 B: "실패하면 어쩔 건데? (Transaction & Rollback)"
상황: 결제는 성공했는데, 영수증 저장하다가 DB가 터지면? 돈은 나갔는데 기록이 없는 대참사가 벌어집니다.

목표: **트랜잭션 전파(Propagation)**와 롤백(Rollback) 정책을 엄밀하게 테스트하고 방어하기.

기술: @Transactional(propagation = ... ), Checked vs Unchecked Exception.

난이도: 중 (기본기 다지기에 최적)

어느 쪽으로 가시겠습니까? (개인적으로는 고성능 아키텍처의 핵심인 **Kafka(옵션 A)**를 추천드리지만, 기본기를 완벽하게 하고 싶다면 옵션 B도 좋습니다.)